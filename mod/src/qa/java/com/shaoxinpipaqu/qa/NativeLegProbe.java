package com.shaoxinpipaqu.qa;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.mojang.blaze3d.vertex.*;
import java.util.*;

/** 自定义腿可能含父骨骼和旋转子网格；收集完整层级变换后的顶点，检查实际长轴而非只认局部角度。 */
final class NativeLegProbe implements VertexConsumer {
    private float minX=Float.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
    private int vertices;
    static String measure(BedrockPart part) {
        var pose=new PoseStack();var parents=new ArrayList<BedrockPart>();
        for(var p=part.parent;p!=null;p=p.parent)parents.add(p);
        Collections.reverse(parents);
        for(var p:parents)p.translateAndRotateAndScale(pose);
        var probe=new NativeLegProbe();part.render(pose,probe,0,0);
        float x=probe.maxX-probe.minX,y=probe.maxY-probe.minY,z=probe.maxZ-probe.minZ;
        MultiplayerCheck.require(probe.vertices>0 && y<Math.max(x,z)*0.5F,"玄爷实际腿网格仍垂直：extent="+x+","+y+","+z);
        return "vertices="+probe.vertices+" extent="+x+","+y+","+z;
    }
    @Override public VertexConsumer addVertex(float x,float y,float z) {
        minX=Math.min(minX,x);minY=Math.min(minY,y);minZ=Math.min(minZ,z);
        maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);maxZ=Math.max(maxZ,z);vertices++;return this;
    }
    @Override public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    @Override public VertexConsumer setUv(float u,float v){return this;}
    @Override public VertexConsumer setUv1(int u,int v){return this;}
    @Override public VertexConsumer setUv2(int u,int v){return this;}
    @Override public VertexConsumer setNormal(float x,float y,float z){return this;}
}
