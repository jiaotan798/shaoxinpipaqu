package com.shaoxinpipaqu.client;

import com.friendswine.DollBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.SongState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** 玩偶旁的MJ文字与上方图片；只读取同步歌曲时钟，不生成实体、粒子或歌词字幕。 */
public final class MemeEffects {
    private static final ResourceLocation DOG=texture("big_dog"),CABBAGE=texture("cabbage"),COURAGE=texture("courage");
    private static final MultiBufferSource.BufferSource PICTURES=MultiBufferSource.immediate(new BufferBuilder(4096));
    private static ShaderInstance shader;
    private static final java.util.Map<DollBlockEntity,MemeTiming.LayoutCache> LAYOUTS=new java.util.IdentityHashMap<>();
    private MemeEffects() {}
    private static ResourceLocation texture(String name) { return new ResourceLocation(Pipa.ID,"textures/meme/"+name+".png"); }

    /** 使用独立连续alpha着色器；Iris替换实体/文字着色器时曾丢失图片或忽略淡出。 */
    public static void loadShader(ShaderInstance loaded) {
        shader=loaded;
    }

    /** 每帧读取同步时钟与本机显示偏好；重新开启直接跟随当前进度，不重播音乐或改变服务端状态。 */
    public static MemeTiming.Cue active(DollBlockEntity doll,float partial) {
        if(!doll.isPlaying()||doll.getLevel()==null||!SongState.of(doll).pipa$isSelected())return null;
        var cue=MemeTiming.active(doll.getElapsedTicks(doll.getLevel().getGameTime())+partial);
        if(cue==null)return null;
        return (cue==MemeTiming.Cue.MJ?ClientConfig.showCueText:ClientConfig.showMemePictures)?cue:null;
    }

    /** 客户端tick在退回菜单时仍执行；清理放在这里，避免退出世界后因不再渲染而保留旧世界引用。 */
    public static void tick() {
        var level=Minecraft.getInstance().level;
        LAYOUTS.keySet().removeIf(d->d.getLevel()!=level||d.isRemoved()||active(d,0)!=MemeTiming.Cue.MJ);
    }

    /** MJ和图片均在世界合成结束后绘制，统一保留连续alpha，避免光影替换字体通道导致柔光失真。 */
    public static void render(DollBlockEntity doll,float partial,PoseStack pose,MultiBufferSource buffers) {
        var cue=active(doll,partial);if(cue!=MemeTiming.Cue.MJ)return;
        var mc=Minecraft.getInstance();var camera=mc.gameRenderer.getMainCamera();
        // 与歌曲20格听音范围保持一致，且不依赖原玩偶的一格包围盒决定空中图片可见性。
        if(camera.getPosition().distanceToSqr(doll.getBlockPos().getCenter())>400)return;
        double ticks=doll.getElapsedTicks(doll.getLevel().getGameTime())+partial;
        int count=MemeTiming.count(doll.getBlockPos().asLong(),doll.getStartTick(),MemeTiming.cycle(ticks),cue);
        double age=MemeTiming.phaseMs(ticks)-cue.startMs;
        var layout=LAYOUTS.computeIfAbsent(doll,d->new MemeTiming.LayoutCache()).get(doll.getBlockPos().asLong(),doll.getStartTick(),MemeTiming.cycle(ticks));
        for(int i=0;i<count;i++) {
            pose.pushPose();
            try {
                var point=layout[i];var pos=doll.getBlockPos();var eye=camera.getPosition();
                // 先放到固定世界位置，再独立面向观察者；不能先旋转镜头，否则文字仍挤在同一平面。
                pose.translate(0.5+point.x(),0.5+point.y(),0.5+point.z());
                double dx=eye.x-(pos.getX()+0.5+point.x()),dy=eye.y-(pos.getY()+0.5+point.y()),dz=eye.z-(pos.getZ()+0.5+point.z());
                pose.mulPose(Axis.YP.rotation((float)Math.atan2(dx,dz)));
                pose.mulPose(Axis.XP.rotation((float)-Math.atan2(dy,Math.hypot(dx,dz))));
                // 大倾角按触发固定，额外轻抖8Hz变化；只影响自身，世界位置和前后关系不变。
                long frame=MemeTiming.mjWiggleFrame(age),cycle=MemeTiming.cycle(ticks);
                float wiggle=MemeTiming.mjTilt(pos.asLong(),doll.getStartTick(),cycle*3+frame,i)/8;
                pose.mulPose(Axis.ZP.rotationDegrees(MemeTiming.mjTilt(pos.asLong(),doll.getStartTick(),cycle,i)+wiggle*2));
                float size=point.scale()*MemeTiming.mjPop(age)*(1+wiggle*0.025F);
                pose.scale(size,-size,size);pose.translate(wiggle*0.12,wiggle*0.16,0);
                drawMj(pose,buffers,MemeTiming.mjColor(i));
            } finally {pose.popPose();}
        }
    }

    /** 光影完成世界合成后输出图片；显式恢复世界矩阵，结束时还原全局状态，不绘制到HUD。 */
    public static void pictures(float partial, Camera camera, Matrix4f modelView, Matrix4f frameProjection) {
        if(shader==null)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){LAYOUTS.clear();return;}
        var eye=camera.getPosition();
        var dolls=DollBlockEntity.getActiveDolls(mc.level);
        if(dolls.stream().noneMatch(d->active(d,partial)!=null&&eye.distanceToSqr(d.getBlockPos().getCenter())<=400))return;
        mc.getMainRenderTarget().bindWrite(false);
        var view=RenderSystem.getModelViewStack();view.pushPose();view.last().pose().set(modelView);RenderSystem.applyModelViewMatrix();
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(frameProjection,sorting);
        try {
            for(var doll:dolls) {
                var cue=active(doll,partial);if(cue==null||eye.distanceToSqr(doll.getBlockPos().getCenter())>400)continue;
                if(cue==MemeTiming.Cue.MJ) {
                    var pose=new PoseStack();var pos=doll.getBlockPos();pose.translate(pos.getX()-eye.x,pos.getY()-eye.y,pos.getZ()-eye.z);
                    render(doll,partial,pose,PICTURES);continue;
                }
                double ticks=doll.getElapsedTicks(mc.level.getGameTime())+partial;
                int count=MemeTiming.count(doll.getBlockPos().asLong(),doll.getStartTick(),MemeTiming.cycle(ticks),cue);
                float opacity=MemeTiming.alpha(cue,MemeTiming.phaseMs(ticks));
                var right=new org.joml.Vector3f(1,0,0).rotate(camera.rotation());
                for(int i=0;i<count;i++) {
                    var pose=new PoseStack();var pos=doll.getBlockPos();double offset=MemeTiming.pictureX(count,i);
                    // 位置仍左右偏转45°，双图自身额外倾斜15°；单张保持正上方且不倾斜。
                    pose.translate(pos.getX()+0.5+right.x*offset-eye.x,pos.getY()+0.5+MemeTiming.pictureY(count,i)-eye.y,pos.getZ()+0.5+right.z*offset-eye.z);
                    pose.mulPose(camera.rotation());
                    pose.mulPose(Axis.ZP.rotationDegrees(MemeTiming.pictureTilt(count,i)));
                    drawPicture(pose,PICTURES,cue,opacity);
                }
            }
            PICTURES.endBatch();
        } finally {RenderSystem.setProjectionMatrix(projection,sorting);view.popPose();RenderSystem.applyModelViewMatrix();}
    }

    private static void drawMj(PoseStack pose,MultiBufferSource buffers,int color) {
        var out=buffers.getBuffer(PictureLayer.type(MjTexture.ID));var transform=pose.last();
        // 四格字形留白容纳柔光，核心字高仍为7像素；Y负缩放与原版字体坐标一致。
        vertex(out,transform,-10,-4,0,0,color,1);
        vertex(out,transform,-10,11,0,1,color,1);
        vertex(out,transform,10,11,1,1,color,1);
        vertex(out,transform,10,-4,1,0,color,1);
    }

    /** 按原图宽高比提交图片；UV 适配 1.20.1 相机方向，透明度沿用同步卡点淡入淡出。 */
    private static void drawPicture(PoseStack pose,MultiBufferSource buffers,MemeTiming.Cue cue,float alpha) {
        ResourceLocation texture=switch(cue) {case DOG->DOG;case CABBAGE->CABBAGE;case COURAGE->COURAGE;default->throw new IllegalArgumentException("非图片卡点");};
        // 导出纹理尺寸分别为396×388、379×512和512×512；原背景、文字和构图全部保留。
        float aspect=switch(cue) {case DOG->396F/388;case CABBAGE->379F/512;default->1;};
        float halfHeight=0.9F,halfWidth=halfHeight*aspect;
        var out=buffers.getBuffer(PictureLayer.type(texture));var transform=pose.last();
        // 1.20.1 的相机与世界视图合成后仍绕 Y 轴转 180°，局部 +X 朝屏幕左；反向 U 才能保持原图文字可读。
        vertex(out,transform,-halfWidth,-halfHeight,1,1,0xFFFFFF,alpha);vertex(out,transform,halfWidth,-halfHeight,0,1,0xFFFFFF,alpha);
        vertex(out,transform,halfWidth,halfHeight,0,0,0xFFFFFF,alpha);vertex(out,transform,-halfWidth,halfHeight,1,0,0xFFFFFF,alpha);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float u,float v,int color,float alpha) {
        // 1.20.1 的 BufferBuilder 只接受 POSITION_TEX_COLOR 规定的位置、UV、颜色顺序。
        out.vertex(pose.pose(),x,y,0).uv(u,v).color((color>>16)&255,(color>>8)&255,color&255,(int)(alpha*255)).endVertex();
    }

    /** 不写深度、不做0.1透明裁剪，保留地形深度测试；只在AFTER_LEVEL使用，避免光影再次改写alpha。 */
    private static final class PictureLayer extends RenderType {
        private static final java.util.Map<ResourceLocation,RenderType> TYPES=new java.util.HashMap<>();
        static RenderType type(ResourceLocation texture) { return TYPES.computeIfAbsent(texture,PictureLayer::new); }
        private PictureLayer(ResourceLocation texture) {
            super("pipa_meme_picture",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,
                    ()->{new ShaderStateShard(()->shader).setupRenderState();new TextureStateShard(texture,false,false).setupRenderState();TRANSLUCENT_TRANSPARENCY.setupRenderState();LEQUAL_DEPTH_TEST.setupRenderState();NO_CULL.setupRenderState();COLOR_WRITE.setupRenderState();},
                    ()->{COLOR_WRITE.clearRenderState();NO_CULL.clearRenderState();LEQUAL_DEPTH_TEST.clearRenderState();TRANSLUCENT_TRANSPARENCY.clearRenderState();new TextureStateShard(texture,false,false).clearRenderState();new ShaderStateShard(()->shader).clearRenderState();});
        }
    }
}
