package com.shaoxinpipaqu.qa;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value="pipa_port_qa",dist=Dist.CLIENT)
public final class PortQaClient {
    public PortQaClient() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event)->PortClientCheck.INSTANCE.tick(Minecraft.getInstance()));
    }
}
