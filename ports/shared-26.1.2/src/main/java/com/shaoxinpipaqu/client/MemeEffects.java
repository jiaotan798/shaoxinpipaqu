package com.shaoxinpipaqu.client;

import com.friendswine.DollBlockEntity;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.SongState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Map;

/** 按同步歌曲时钟提交卡点几何，与前置玩偶的裁剪盒无关。 */
public final class MemeEffects {
    private static final Identifier DOG=texture("big_dog"),CABBAGE=texture("cabbage"),COURAGE=texture("courage");
    private static final Map<DollBlockEntity,MemeTiming.LayoutCache> LAYOUTS=new IdentityHashMap<>();
    private static final Map<Identifier,RenderType> LAYERS=new HashMap<>();
    private static final MultiBufferSource.BufferSource CARDS=MultiBufferSource.immediate(new ByteBufferBuilder(4096));
    // 原版位置纹理着色器只丢弃 alpha=0；不使用带 0.1 裁剪的实体预设，保留连续淡入淡出。
    private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Pipa.ID,"pipeline/meme_picture"))
            .withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0").withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL,false)).withCull(false).build();
    private static boolean mjLoaded;
    private MemeEffects() {}

    private static Identifier texture(String name) { return Identifier.fromNamespaceAndPath(Pipa.ID,"textures/meme/"+name+".png"); }

    private static MemeTiming.Cue active(DollBlockEntity doll,float partial) {
        if(!doll.isPlaying()||doll.getLevel()==null||!SongState.of(doll).pipa$isSelected())return null;
        var cue=MemeTiming.active(doll.getElapsedTicks(doll.getLevel().getGameTime())+partial);
        if(cue==null)return null;
        return (cue==MemeTiming.Cue.MJ?ClientConfig.SHOW_CUE_TEXT.get():ClientConfig.SHOW_MEME_PICTURES.get())?cue:null;
    }

    /** 换世界、停播或禁用文字后释放布局缓存，避免旧世界引用残留。 */
    public static void tick() {
        var level=Minecraft.getInstance().level;
        LAYOUTS.keySet().removeIf(d->d.getLevel()!=level||d.isRemoved()||active(d,0)!=MemeTiming.Cue.MJ);
    }

    /** 世界已合成，按相机相对坐标绘制卡点；只读主目标地形深度，不写深度。 */
    public static void render(CameraRenderState camera,float partial) {
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        var pose=new PoseStack();
        var eye=camera.pos;
        for(var doll:DollBlockEntity.getActiveDolls(mc.level)) {
            var cue=active(doll,partial);if(cue==null||eye.distanceToSqr(doll.getBlockPos().getCenter())>400)continue;
            pose.pushPose();
            try {
                var pos=doll.getBlockPos();
                pose.translate(pos.getX()-eye.x,pos.getY()-eye.y,pos.getZ()-eye.z);
                if(cue==MemeTiming.Cue.MJ) {
                    if(!mjLoaded) { MjTexture.reload();mjLoaded=true; }
                    text(doll,partial,eye,pose,cue);
                } else {
                    double ticks=doll.getElapsedTicks(doll.getLevel().getGameTime())+partial;
                    int count=MemeTiming.count(pos.asLong(),doll.getStartTick(),MemeTiming.cycle(ticks),cue);
                    float opacity=MemeTiming.alpha(cue,MemeTiming.phaseMs(ticks));
                    var right=new org.joml.Vector3f(1,0,0).rotate(camera.orientation);
                    for(int i=0;i<count;i++) {
                        pose.pushPose();
                        try {
                            double offset=MemeTiming.pictureX(count,i);
                            pose.translate(0.5+right.x*offset,0.5+MemeTiming.pictureY(count,i),0.5+right.z*offset);
                            pose.mulPose(camera.orientation);
                            pose.mulPose(Axis.ZP.rotationDegrees(MemeTiming.pictureTilt(count,i)));
                            picture(pose,cue,opacity);
                        } finally {pose.popPose();}
                    }
                }
            } finally {pose.popPose();}
        }
        CARDS.endBatch();
    }

    private static void text(DollBlockEntity doll,float partial,Vec3 eye,PoseStack pose,MemeTiming.Cue cue) {
        double ticks=doll.getElapsedTicks(doll.getLevel().getGameTime())+partial;
        int count=MemeTiming.count(doll.getBlockPos().asLong(),doll.getStartTick(),MemeTiming.cycle(ticks),cue);
        double age=MemeTiming.phaseMs(ticks)-cue.startMs;
        var layout=LAYOUTS.computeIfAbsent(doll,d->new MemeTiming.LayoutCache()).get(doll.getBlockPos().asLong(),doll.getStartTick(),MemeTiming.cycle(ticks));
        for(int i=0;i<count;i++) {
            pose.pushPose();
            try {
                var point=layout[i];var pos=doll.getBlockPos();
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
                int color=0xFF000000|MemeTiming.mjColor(i);
                quad(pose,MjTexture.ID,-10,-4,10,11,color,true);
            } finally {pose.popPose();}
        }
    }

    private static void picture(PoseStack pose,MemeTiming.Cue cue,float alpha) {
        var image=switch(cue) {case DOG->DOG;case CABBAGE->CABBAGE;case COURAGE->COURAGE;default->throw new IllegalArgumentException("非图片卡点");};
        // 保持各原图宽高比以及 0.9 格半高。
        float aspect=switch(cue) {case DOG->396F/388;case CABBAGE->379F/512;default->1;};
        float y=0.9F,x=y*aspect;
        quad(pose,image,-x,-y,x,y,(Math.round(alpha*255)<<24)|0xFFFFFF,false);
    }

    private static void quad(PoseStack pose,Identifier image,float left,float bottom,float right,float top,int argb,boolean flip) {
        var layer=LAYERS.computeIfAbsent(image, id->RenderType.create("pipa_meme_picture",
                RenderSetup.builder(PIPELINE).withTexture("Sampler0",id).sortOnUpload().createRenderSetup()));
        var out=CARDS.getBuffer(layer);
        var transform=pose.last();
        int low=flip?0:1,high=flip?1:0;
        out.addVertex(transform,left,bottom,0).setUv(0,low).setColor(argb);
        out.addVertex(transform,right,bottom,0).setUv(1,low).setColor(argb);
        out.addVertex(transform,right,top,0).setUv(1,high).setColor(argb);
        out.addVertex(transform,left,top,0).setUv(0,high).setColor(argb);
    }
}
