package com.shaoxinpipaqu.qa;

import com.shaoxinpipaqu.Pipa;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;

/** 读取实际加载的效果图集，并截取真实HUD和背包画面；仅独立QA使用。 */
final class EffectIconProbe {
    private InventoryScreen inventory;
    private java.util.concurrent.CompletableFuture<Void> reload;
    private int preparedStage=-1,readyAge;

    boolean ownsScreen(Minecraft mc) { return inventory!=null && mc.screen==inventory; }

    /** 每种缩放先等界面完成绘制再截图，阶段五另检查资源重载后的图集。 */
    void tick(Minecraft mc,int stage,int age,String role) throws Exception {
        if(Files.exists(MultiplayerCheck.ROOT.resolve(role+"-"+stage+".ok")))return;
        if(!mc.player.hasEffect(Pipa.PEACE)||!mc.player.hasEffect(Pipa.MILK))return;
        if(preparedStage!=stage) {
            preparedStage=stage;readyAge=age;
            mc.options.guiScale().set(stage<=2?2:3);mc.resizeDisplay();
            if(stage==5)reload=mc.reloadResourcePacks();
        }
        if(stage==5 && (reload==null || !reload.isDone()))return;
        if(stage==5)reload.join();
        boolean bag=stage==2||stage==4;
        if(bag && inventory==null) {inventory=new InventoryScreen(mc.player);mc.setScreen(inventory);readyAge=age;return;}
        // 从实际就绪/打开界面计时，不能把等待效果同步的时间当成已经绘制了背包。
        if(age-readyAge<20)return;
        var detail=new StringBuilder("PASS actual resource atlas, GUI scale="+mc.getWindow().getGuiScale()+" inventory="+bag+" reload="+(stage==5)+"\n");
        for(var effect:java.util.List.of(Pipa.PEACE,Pipa.MILK)) {
            var name=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
            var sprite=mc.getMobEffectTextures().get(effect);
            MultiplayerCheck.require(sprite.contents().width()==36&&sprite.contents().height()==36,"实际效果图集尺寸："+name);
            try(var stream=mc.getResourceManager().open(ResourceLocation.fromNamespaceAndPath(name.getNamespace(),"textures/mob_effect/"+name.getPath()+".png"));
                var image=com.mojang.blaze3d.platform.NativeImage.read(stream)) {
                int opaque=0,clear=0;var colors=new java.util.HashSet<Integer>();
                for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
                    int pixel=image.getPixelRGBA(x,y),alpha=pixel>>>24;
                    MultiplayerCheck.require(alpha==0||alpha==255,"效果贴图不能残留半透明边缘");
                    if(alpha==0)clear++;else {opaque++;colors.add(pixel);}
                }
                MultiplayerCheck.require(opaque>30&&clear>30&&colors.size()>4,"实际加载图标必须有透明边缘及多色图案："+name);
                detail.append(name).append(" opaque=").append(opaque).append(" clear=").append(clear).append(" colors=").append(colors.size()).append('\n');
            }
        }
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {image.writeToFile(MultiplayerCheck.ROOT.resolve(role+"-effects-"+stage+".png"));}
        if(inventory!=null){mc.setScreen(null);inventory=null;}
        if(stage==5){mc.options.guiScale().set(2);mc.resizeDisplay();}
        MultiplayerCheck.write(role+"-"+stage+".ok",detail.toString());
    }
}
