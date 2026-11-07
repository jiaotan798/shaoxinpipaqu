package com.shaoxinpipaqu.mixin;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(QuadrupedModel.class)
public interface QuadrupedAccessor {
    @Accessor("body") ModelPart pipa$body();
    @Accessor("head") ModelPart pipa$head();
    @Accessor("rightFrontLeg") ModelPart pipa$frontRight();
    @Accessor("leftFrontLeg") ModelPart pipa$frontLeft();
    @Accessor("rightHindLeg") ModelPart pipa$hindRight();
    @Accessor("leftHindLeg") ModelPart pipa$hindLeft();
}
