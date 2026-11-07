package com.shaoxinpipaqu.mixin;

import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 只读原版私有网格列表，用真实手臂边界定位掌心，不修改或复制网格。 */
@Mixin(ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("cubes") List<ModelPart.Cube> pipa$cubes();
}
