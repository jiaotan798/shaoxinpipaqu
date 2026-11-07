package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.ExtraTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 原版 Shift 快速交易不调用 mayPickup，必须在移动结果之前单独核验。 */
@Mixin(MerchantMenu.class)
public abstract class TradeMenuMixin {
    @Shadow @Final private MerchantContainer tradeContainer;
    @Inject(method="quickMoveStack",at=@At("HEAD"),cancellable=true)
    private void pipa$guard(Player p,int index,CallbackInfoReturnable<ItemStack> cir) { if (index==2 && !ExtraTrades.canTake(p,tradeContainer.getActiveOffer())) cir.setReturnValue(ItemStack.EMPTY); }
}
