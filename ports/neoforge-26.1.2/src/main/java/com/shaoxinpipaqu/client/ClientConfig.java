package com.shaoxinpipaqu.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** 本机卡点显示偏好；不参与服务端玩法、音乐时钟或其他玩家的画面判定。 */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_CUE_TEXT, SHOW_MEME_PICTURES;
    static {
        var builder=new ModConfigSpec.Builder();
        SHOW_CUE_TEXT=builder.comment("是否显示卡点文字（目前为MJ），只影响本机画面，保存后立即生效。")
                .translation("shaoxinpipaqu.config.showCueText").define("showCueText",true);
        SHOW_MEME_PICTURES=builder.comment("是否显示卡点表情包，只影响本机画面，不停止音乐或飞行。")
                .translation("shaoxinpipaqu.config.showMemePictures").define("showMemePictures",true);
        SPEC=builder.build();
    }
    private ClientConfig() {}
}
