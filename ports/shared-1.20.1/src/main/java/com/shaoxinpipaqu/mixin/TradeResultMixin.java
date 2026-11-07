package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.ExtraTrades;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.*;

/** 普通点击在物品被取走前校验第三材料；成功扣两槽材料后再扣碗。 */
@Mixin(MerchantResultSlot.class)
public abstract class TradeResultMixin extends Slot {
    @Shadow @Final private MerchantContainer slots;
    @Shadow @Final private Player player;
    protected TradeResultMixin(Container c,int slot,int x,int y) { super(c,slot,x,y); }
    @Override public boolean mayPickup(Player p) { return super.mayPickup(p) && ExtraTrades.canTake(p,slots.getActiveOffer()); }
    @WrapOperation(method="onTake",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/trading/MerchantOffer;take(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean pipa$pay(MerchantOffer o,ItemStack a,ItemStack b,Operation<Boolean> original) { return ExtraTrades.noodles(o) ? ExtraTrades.take(o,a,b,player) : original.call(o,a,b); }
}
