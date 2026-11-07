package com.shaoxinpipaqu.qa;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** 从实际烘焙网格收集四边形UV并逐像素检查基础皮肤，避免关节角度正确却采样到透明纹理。 */
final class VillagerUvProbe implements VertexConsumer {
    private final NativeImage skin;
    private final float[] us=new float[4],vs=new float[4];
    private final List<ModelPart.Cube> boxes=new ArrayList<>();
    private int vertices,pixels,transparent;
    private VillagerUvProbe(NativeImage skin) {this.skin=skin;}

    /** 旧网格作为红灯对照；新网格须保留12像素臂长且两段相接，所有基础皮肤采样均应不透明。 */
    static String check(ModelPart right,ModelPart left) throws java.io.IOException {
        var resource=ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
        try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(resource).open();var skin=NativeImage.read(stream)) {
            var mesh=new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("old_arm",CubeListBuilder.create().texOffs(44,22).addBox(-2,-2,-2,4,12,4),PartPose.ZERO);
            var old=scan(LayerDefinition.create(mesh,64,64).bakeRoot().getChild("old_arm"),skin);
            MultiplayerCheck.require(old.transparent==64,"旧网格应重现64个透明采样像素，当前资源不满足对照前提");
            var details=new StringBuilder("legacy transparent=64 (red control)\n");
            for(var arm:List.of(right,left)) {
                var probe=scan(arm,skin);probe.boxes.sort(Comparator.comparingDouble(b->b.minY));
                MultiplayerCheck.require(probe.boxes.size()==2&&probe.vertices==48,"独立臂应包含袖段和手部的12个四边形");
                var sleeve=probe.boxes.get(0);var hand=probe.boxes.get(1);
                MultiplayerCheck.require(sleeve.minY==-2&&sleeve.maxY==hand.minY&&hand.maxY==10,"袖段与手部必须连续且总长12像素");
                MultiplayerCheck.require(sleeve.minX==hand.minX&&sleeve.maxX==hand.maxX&&sleeve.minZ==hand.minZ&&sleeve.maxZ==hand.maxZ,"手部截面应与袖段对齐");
                MultiplayerCheck.require(probe.transparent==0,"村民手臂网格仍有透明采样："+probe.transparent+"/"+probe.pixels);
                details.append("arm quads=12 length=12 sampledPixels=").append(probe.pixels).append(" transparent=0 (green)\n");
            }
            return details.toString();
        }
    }
    private static VillagerUvProbe scan(ModelPart arm,NativeImage skin) {
        var probe=new VillagerUvProbe(skin);
        arm.visit(new PoseStack(),(pose,path,index,cube)->{probe.boxes.add(cube);cube.compile(pose,probe,0,0,-1);});
        return probe;
    }
    @Override public void addVertex(float x,float y,float z,int color,float u,float v,int overlay,int light,float nx,float ny,float nz) {
        int corner=vertices++%4;us[corner]=u;vs[corner]=v;
        if(corner!=3)return;
        float minU=us[0],maxU=us[0],minV=vs[0],maxV=vs[0];
        for(int i=1;i<4;i++){minU=Math.min(minU,us[i]);maxU=Math.max(maxU,us[i]);minV=Math.min(minV,vs[i]);maxV=Math.max(maxV,vs[i]);}
        // UV边界本身落在相邻像素之间，只采样四边形内部的纹素，不把右/下邻区误算成面内透明。
        int x0=(int)Math.floor(minU*skin.getWidth()+0.0001F),x1=(int)Math.ceil(maxU*skin.getWidth()-0.0001F);
        int y0=(int)Math.floor(minV*skin.getHeight()+0.0001F),y1=(int)Math.ceil(maxV*skin.getHeight()-0.0001F);
        MultiplayerCheck.require(x0>=0&&y0>=0&&x1<=skin.getWidth()&&y1<=skin.getHeight(),"村民手臂UV越界");
        for(int py=y0;py<y1;py++)for(int px=x0;px<x1;px++){pixels++;if((skin.getPixelRGBA(px,py)>>>24)==0)transparent++;}
    }
    @Override public VertexConsumer addVertex(float x,float y,float z){return this;}
    @Override public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    @Override public VertexConsumer setUv(float u,float v){return this;}
    @Override public VertexConsumer setUv1(int u,int v){return this;}
    @Override public VertexConsumer setUv2(int u,int v){return this;}
    @Override public VertexConsumer setNormal(float x,float y,float z){return this;}
}
