package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.client.FlightRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
abstract class FlightRenderStateMixin implements FlightRenderState {
    @Unique private float pipa$weight,pipa$bank;
    public float pipa$weight() { return pipa$weight; }
    public float pipa$bank() { return pipa$bank; }
    public void pipa$pose(float weight,float bank) { pipa$weight=weight;pipa$bank=bank; }
}
