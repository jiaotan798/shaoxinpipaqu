package com.shaoxinpipaqu;

/** 方案中的人数及速度换算集中保存，单位为格和服务端 tick。 */
public final class FlightRules {
    private FlightRules() {}
    public static int count(int population) { return population <= 0 ? 0 : Math.max(1, population / 5); }
    public static double wave(long ticks) { return 5 + 2 * Math.sin(ticks * Math.PI * 2 / 160.0); }
    public static void main(String[] args) {
        int[] expected = {0,1,1,1,1,1,1,1,1,1,2,2,2,2,2,3};
        for (int n = 0; n < expected.length; n++) if (count(n) != expected[n]) throw new AssertionError("count " + n);
        for (int t = 0; t < 1600; t++) if (wave(t) < 3 || wave(t) > 7 || Math.abs(wave(t) - wave(t + 160)) > 1e-9) throw new AssertionError("wave");
        System.out.println("人数取整与波浪周期检查通过");
    }
}
