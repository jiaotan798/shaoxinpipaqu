package com.shaoxinpipaqu.qa;

import com.shaoxinpipaqu.AudioInfo;
import net.minecraft.client.sounds.AudioStream;

/** 观察前置实际返回的已定位 PCM 流；样本偏移检查不代表主观听感验收。 */
public final class AudioProbe {
    public static volatile String failure;
    private static final java.util.concurrent.atomic.AtomicInteger SEQUENCE=new java.util.concurrent.atomic.AtomicInteger();
    public static void capture(AudioStream stream,long elapsed,String location) {
        if(!location.equals("shaoxinpipaqu:pipa"))return;
        try {
            var field=stream.getClass().getDeclaredField("skippedBytes");field.setAccessible(true);
            long frame=field.getLong(stream)/stream.getFormat().getFrameSize()%AudioInfo.FRAMES;
            long expected=Math.floorMod(elapsed*(AudioInfo.SAMPLE_RATE/20),AudioInfo.FRAMES);
            long difference=Math.abs(frame-expected);difference=Math.min(difference,AudioInfo.FRAMES-difference);
            // 异步解码完成后读取服务端 tick 镜像，允许两个 tick 的线程调度差，不能容许从头重播。
            MultiplayerCheck.require(difference<=AudioInfo.SAMPLE_RATE/10,"实际PCM定位与同步进度偏差超过100ms："+difference);
            MultiplayerCheck.write(System.getProperty("pipa.qa.role")+"-audio-"+SEQUENCE.incrementAndGet()+".txt","stage="+PoseProbe.stage+" elapsedTicks="+elapsed+" actualFrame="+frame+" expectedFrame="+expected+" differenceFrames="+difference+" sampleRate="+stream.getFormat().getSampleRate());
        }catch(Throwable error){failure=error.toString();}
    }
}
