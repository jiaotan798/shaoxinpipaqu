package com.shaoxinpipaqu.client;

import net.minecraft.client.model.geom.ModelPart;

/** 部分原版四足模型不继承QuadrupedModel；保留构造时的根节点用于按真实关节适配。 */
public interface AnimalModelRoot {
    ModelPart pipa$animalRoot();
}
