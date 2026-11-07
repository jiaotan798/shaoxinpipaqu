package com.shaoxinpipaqu.client;

/** 提取阶段只保存当前帧的姿势值，延迟绘制不再引用活动实体。 */
public interface FlightRenderState {
    float pipa$weight();
    float pipa$bank();
    void pipa$pose(float weight,float bank);
}
