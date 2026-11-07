package com.shaoxinpipaqu.qa;

import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.client.ClientConfig;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** 通过原生配置界面编辑、保存和重开；不直接set配置值冒充界面验收。 */
final class BaseConfigScreenCheck {
    private String request;
    private int phase,age;
    private final String role=System.getProperty("pipa.qa.role");

    /** 每tick按编辑→保存→磁盘校验→重开推进；保存值、运行值和重开后的两个按钮一致才返回true。 */
    boolean apply(String request,boolean text,boolean pictures) throws Exception {
        var mc=Minecraft.getInstance();
        if(!request.equals(this.request)) {this.request=request;phase=0;age=0;open();return false;}
        if(phase==4)return true;
        if(++age>200)throw new AssertionError("配置界面检查超时："+request+" phase="+phase);
        if(age<10)return false;
        BaseCheck.require(phase==2||mc.screen instanceof ConfigurationScreen.ConfigurationSectionScreen,"原生配置页必须可访问");
        if(phase==0) {
            shot("before");
            var textButton=toggle("showCueText");
            if(!textButton.getValue().equals(text))textButton.onPress();
            var pictureButton=toggle("showMemePictures");
            if(!pictureButton.getValue().equals(pictures))pictureButton.onPress();
            phase=1;age=0;return false;
        }
        if(phase==1) {
            shot("edited");mc.screen.onClose();mc.setScreen(null);phase=2;age=0;return false;
        }
        if(phase==2) {
            BaseCheck.require(ClientConfig.SHOW_CUE_TEXT.get()==text&&ClientConfig.SHOW_MEME_PICTURES.get()==pictures,"保存后运行值立即生效");
            var file=mc.gameDirectory.toPath().resolve("config/shaoxinpipaqu-client.toml");
            String saved=Files.readString(file);
            BaseCheck.require(saved.contains("showCueText = "+text)&&saved.contains("showMemePictures = "+pictures),"设置必须真实保存到TOML");
            BaseCheck.write(role+"-config-"+request+".toml",saved);open();phase=3;age=0;return false;
        }
        BaseCheck.require(toggle("showCueText").getValue().equals(text)&&toggle("showMemePictures").getValue().equals(pictures),"重开界面须保留两项设置");
        shot("reopened");mc.screen.onClose();mc.setScreen(null);phase=4;
        BaseCheck.write(role+"-config-"+request+".txt","PASS native UI edit/save/reopen; text="+text+" pictures="+pictures+"\n");
        return true;
    }
    private static void open() {
        var mc=Minecraft.getInstance();var container=ModList.get().getModContainerById(Pipa.ID).orElseThrow();
        mc.setScreen(IConfigScreenFactory.getForMod(container.getModInfo()).orElseThrow().createScreen(container,null));
    }
    private static CycleButton<?> toggle(String key) {
        String label=Component.translatable("shaoxinpipaqu.config."+key).getString();
        var controls=widgets(Minecraft.getInstance().screen);
        var caption=controls.stream().filter(w->w.getMessage().getString().equals(label)).findFirst().orElseThrow(()->new AssertionError("找不到配置标签："+label));
        // 原生布尔按钮只显示开/关，名称是同一行的独立StringWidget，必须按标签所在行定位。
        return controls.stream().filter(w->w instanceof CycleButton<?> && w.getY()==caption.getY())
                .map(w->(CycleButton<?>)w).findFirst().orElseThrow(()->new AssertionError("找不到配置按钮："+label));
    }
    private static List<AbstractWidget> widgets(GuiEventListener root) {
        var result=new ArrayList<AbstractWidget>();
        if(root instanceof AbstractWidget widget)result.add(widget);
        if(root instanceof ContainerEventHandler parent)for(var child:parent.children())result.addAll(widgets(child));
        return result;
    }
    private void shot(String suffix) throws Exception {
        try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            image.writeToFile(BaseCheck.ROOT.resolve(role+"-config-"+request+"-"+suffix+".png"));
        }
    }
}
