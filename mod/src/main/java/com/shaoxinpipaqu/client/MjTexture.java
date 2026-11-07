package com.shaoxinpipaqu.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.shaoxinpipaqu.Pipa;

/** 仅MJ使用的自绘像素字形及弱柔光遮罩，不复制外部字体资源；重载时由纹理管理器替换旧纹理。 */
final class MjTexture {
    static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(Pipa.ID,"dynamic/mj_glow");
    private MjTexture() {}

    /** 4倍像素字芯加高斯衰减外晕，合在一张RGBA中；运行时只染色，不依赖光影包泛光。 */
    static void reload() {
        int width=80,height=60;float[] mask=new float[width*height];
        String[] rows={"10001 11111","11011 00010","10101 00010","10001 00010","10001 10010","10001 10010","10001 01100"};
        for(int y=0;y<7;y++)for(int x=0;x<11;x++)if(rows[y].charAt(x)=='1')
            for(int yy=0;yy<4;yy++)for(int xx=0;xx<4;xx++)mask[(y*4+16+yy)*width+x*4+18+xx]=1;
        var image=new NativeImage(width,height,false);
        // 高斯核与像素位置无关；保持原求和顺序和double精度，避免每像素重复计算289次exp。
        double[] kernel=new double[17*17];double weight=0;
        for(int dy=-8;dy<=8;dy++)for(int dx=-8;dx<=8;dx++) {
            double w=Math.exp(-(dx*dx+dy*dy)/18.0);kernel[(dy+8)*17+dx+8]=w;weight+=w;
        }
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            double sum=0;
            for(int dy=-8;dy<=8;dy++)for(int dx=-8;dx<=8;dx++) {
                double w=kernel[(dy+8)*17+dx+8];
                if(x+dx>=0&&x+dx<width&&y+dy>=0&&y+dy<height)sum+=mask[(y+dy)*width+x+dx]*w;
            }
            // 1个纹理像素=0.25字体像素的细暗边，在天空背景上保护浅色字芯，不扩成粗描边。
            boolean edge=false;
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)
                if(x+dx>=0&&x+dx<width&&y+dy>=0&&y+dy<height&&mask[(y+dy)*width+x+dx]>0)edge=true;
            boolean core=mask[y*width+x]>0;
            int alpha=Math.round(Math.max(core?1:edge?0.72F:0,(float)(sum/weight)*0.38F)*255);
            image.setPixelRGBA(x,y,(alpha<<24)|(edge&&!core?0x303030:0xFFFFFF));
        }
        Minecraft.getInstance().getTextureManager().register(ID,new DynamicTexture(image));
    }
}
