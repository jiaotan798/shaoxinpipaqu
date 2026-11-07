package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.client.AnimalModelRoot;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 这些1.21.1模型均以ModelPart构造，且四腿为根节点的同名子关节；不反射或猜测模组骨架。 */
@Mixin({WolfModel.class,OcelotModel.class,FoxModel.class,HorseModel.class,LlamaModel.class,HoglinModel.class,RavagerModel.class,CreeperModel.class})
public abstract class AnimalModelRootMixin implements AnimalModelRoot {
    @Unique private ModelPart pipa$root;
    @Inject(method="<init>",at=@At("TAIL"))
    private void pipa$capture(ModelPart root,CallbackInfo ci){pipa$root=root;}
    public ModelPart pipa$animalRoot(){return pipa$root;}
}
