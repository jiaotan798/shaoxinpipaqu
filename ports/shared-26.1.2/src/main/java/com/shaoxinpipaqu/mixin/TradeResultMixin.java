package com.shaoxinpipaqu.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shaoxinpipaqu.ExtraTrades;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** 在普通点击取走商品前查碗，只有原版支付成功才扣除背包里的碗。 */
@Mixin(MerchantResultSlot.class)
abstract class TradeResultMixin extends Slot {
    @Shadow @Final private MerchantContainer slots;
    @Shadow @Final private Player player;

    protected TradeResultMixin(Container c,int slot,int x,int y) { super(c,slot,x,y); }

    @Override public boolean mayPickup(Player p) {
        return super.mayPickup(p) && ExtraTrades.canTake(p,slots.getActiveOffer());
    }

    @WrapOperation(method="onTake",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/trading/MerchantOffer;take(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean pipa$pay(MerchantOffer offer,ItemStack first,ItemStack second,Operation<Boolean> original) {
        return ExtraTrades.take(offer,player,()->original.call(offer,first,second));
    }
}
