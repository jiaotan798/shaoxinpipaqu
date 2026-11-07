package com.shaoxinpipaqu.qa;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

/** 独立 QA 模组的 Forge 服务端入口。 */
@Mod("pipa_qa")
public final class ForgePortQa {
    public ForgePortQa() {
        if (!"server".equals(System.getProperty("pipa.qa.role"))) return;
        var check = new PortCheck();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) check.tick(event.getServer());
        });
    }
}
