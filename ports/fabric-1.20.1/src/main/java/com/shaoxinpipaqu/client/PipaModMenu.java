package com.shaoxinpipaqu.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu 安装时提供本机卡点显示入口。 */
public final class PipaModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return ClientConfig::screen; }
}
