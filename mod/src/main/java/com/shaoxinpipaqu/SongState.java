package com.shaoxinpipaqu;

/** 玩偶持久化选曲接口；切歌不重新触发开启生成。 */
public interface SongState {
    // 前置类为 final，编译期不声明此接口；Mixin 在加载时实现它。
    static SongState of(Object doll) { return (SongState)doll; }
    boolean pipa$isSelected();
    void pipa$toggle();
}
