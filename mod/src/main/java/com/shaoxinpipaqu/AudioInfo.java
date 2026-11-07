package com.shaoxinpipaqu;
/** 由 scripts/prepare-resources.py 生成；修改音频请重跑源脚本，不手动改时钟。 */
public final class AudioInfo {
    public static final int SAMPLE_RATE = 44100;
    public static final long FRAMES = 1170494L;
    public static final double LOOP_TICKS = FRAMES * 20.0 / SAMPLE_RATE;
    private AudioInfo() {}
}
