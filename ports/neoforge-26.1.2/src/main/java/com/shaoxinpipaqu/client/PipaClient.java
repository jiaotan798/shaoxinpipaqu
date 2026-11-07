package com.shaoxinpipaqu.client;

import com.shaoxinpipaqu.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** 客户端按下沿发出切歌请求；长按只触发一次。 */
@Mod(value=Pipa.ID,dist=Dist.CLIENT)
public final class PipaClient {
    private boolean held;
    public PipaClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT,ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class,ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> { FlightVisuals.tick(); MemeEffects.tick(); });
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered e) -> {
            var player=Minecraft.getInstance().player;
            if (e.isAttack() && player!=null && RemoteControl.held(player)) { e.setCanceled(true); e.setSwingHand(false); }
        });
    }
    private void tick(ClientTickEvent.Pre e) {
        var mc=Minecraft.getInstance();
        boolean down=mc.options.keyAttack.isDown();
        if (down && !held && mc.screen==null && mc.player!=null && RemoteControl.held(mc.player))
            ClientPacketDistributor.sendToServer(new Network.SwitchSong());
        held=down;
    }
}
