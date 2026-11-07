package com.shaoxinpipaqu.qa;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("pipa_port_qa")
public final class PortQa {
    public PortQa() {
        var check=new PortCheck();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event)->check.tick(event.getServer()));
    }
}
