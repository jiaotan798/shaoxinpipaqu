package com.shaoxinpipaqu.qa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import com.google.gson.Gson;

/** 仅在三遍音乐稳态阶段采集真实呈现帧间隔；包含30FPS限帧等待，不等同GPU执行耗时。 */
final class FrameTimingProbe {
    private final ArrayList<Double> intervals=new ArrayList<>();
    private long previous;

    void frame(boolean sampling) {
        long now=System.nanoTime();
        if(sampling&&previous!=0)intervals.add((now-previous)/1_000_000.0);
        previous=sampling?now:0;
    }

    /** 原始样本一并落盘，后续可在相同限帧、分辨率与场景下比较，不能跨测试模式比较。 */
    void finish(String role) {
        var result=new LinkedHashMap<String,Object>();
        result.put("scope","stage53 steady playback; 1000x760; 30 FPS cap; hidden dual clients; includes frame limiter and scheduling");
        result.put("renderer",org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
        result.put("vendor",org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR));
        result.put("openGL",org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION));
        result.put("samples",intervals.size());result.put("rawFrameIntervalsMs",intervals);
        if(!intervals.isEmpty()) {
            var sorted=new ArrayList<>(intervals);Collections.sort(sorted);
            double mean=intervals.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            result.put("meanMs",mean);result.put("observedFps",1000/mean);
            result.put("p50Ms",sorted.get((int)Math.ceil(sorted.size()*0.5)-1));
            result.put("p95Ms",sorted.get((int)Math.ceil(sorted.size()*0.95)-1));
            result.put("p99Ms",sorted.get((int)Math.ceil(sorted.size()*0.99)-1));
            result.put("over100Ms",intervals.stream().filter(v->v>100).count());
        }
        MultiplayerCheck.write(role+"-frame-timing.json",new Gson().toJson(result));
    }
}
