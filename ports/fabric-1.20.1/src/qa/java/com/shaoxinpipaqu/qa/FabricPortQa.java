package com.shaoxinpipaqu.qa;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** 独立 QA 模组的 Fabric 服务端入口。 */
public final class FabricPortQa implements ModInitializer {
    @Override public void onInitialize() {
        if ("server".equals(System.getProperty("pipa.qa.role")))
            ServerTickEvents.END_SERVER_TICK.register(new PortCheck()::tick);
    }
}
