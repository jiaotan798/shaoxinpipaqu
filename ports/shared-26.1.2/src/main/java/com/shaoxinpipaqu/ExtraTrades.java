package com.shaoxinpipaqu;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;

/** 原版只有两种交易材料槽；面条的第三种材料由结果槽额外校验、扣取背包中的碗。 */
public final class ExtraTrades {
    private static final String DAY="PipaRestockDay";
    private ExtraTrades() {}
    private static boolean eligible(Villager v) { return !v.level().isClientSide() && v.isAlive() && !v.isBaby() && v.getVillagerData().profession().is(VillagerProfession.NITWIT); }
    public static void interact(Villager v, Player player, ItemStack held) {
        if (eligible(v) && !v.isTrading() && !v.isSleeping() && !player.isSecondaryUseActive() && !held.is(Items.VILLAGER_SPAWN_EGG)) {
            ensure(v); restock(v);
        }
    }
    public static void tick(Villager v) {
        if (eligible(v) && v.tickCount%20==0 && Pipa.data(v).contains(DAY)) restock(v);
    }
    /** 幂等补齐附属商品，不重置已有库存或前置商品；只在首次接入时建立日期基准。 */
    public static void ensure(Villager v) {
        if (v.getOffers().stream().noneMatch(o -> noodles(o))) v.getOffers().add(new MerchantOffer(new ItemCost(Items.COOKED_BEEF,2),Optional.of(new ItemCost(Items.WHEAT,8)),new ItemStack(Pipa.NOODLES.get()),16,0,0));
        if (v.getOffers().stream().noneMatch(o -> o.getResult().is(Pipa.DOG_MILK.get()))) v.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD,4),Optional.empty(),new ItemStack(Pipa.DOG_MILK.get()),16,0,0));
        if (!Pipa.data(v).contains(DAY)) Pipa.data(v).putLong(DAY,Math.floorDiv(v.level().getOverworldClockTime(),24000));
    }
    private static void restock(Villager v) {
        long now=Math.floorDiv(v.level().getOverworldClockTime(),24000), previous=Pipa.data(v).getLongOr(DAY,0);
        if (now==previous) return;
        // 日期倒退仅重建基准，下一次跨天才补货；不重置前置或其他模组交易。
        if (now>previous) v.getOffers().stream().filter(o -> noodles(o) || o.getResult().is(Pipa.DOG_MILK.get())).forEach(MerchantOffer::resetUses);
        Pipa.data(v).putLong(DAY,now);
        Player buyer=v.getTradingPlayer();
        if (buyer!=null && buyer.containerMenu instanceof MerchantMenu menu) {
            menu.slotsChanged(buyer.getInventory());
            buyer.sendMerchantOffers(menu.containerId,v.getOffers(),v.getVillagerData().level(),v.getVillagerXp(),v.showProgressBar(),v.canRestock());
            menu.broadcastChanges();
        }
    }
    public static boolean noodles(MerchantOffer o) { return o!=null && o.getResult().is(Pipa.NOODLES.get()); }
    public static boolean canTake(Player p, MerchantOffer o) {
        if (!noodles(o)) return true;
        boolean found=p.getInventory().getNonEquipmentItems().stream().anyMatch(s -> s.is(Items.BOWL));
        if (!found && !p.level().isClientSide()) p.sendOverlayMessage(Component.translatable("message.shaoxinpipaqu.bowl"));
        return found;
    }
    public static boolean take(MerchantOffer o, Player p, BooleanSupplier payment) {
        if (!canTake(p,o) || !payment.getAsBoolean()) return false;
        if (noodles(o)) for (ItemStack s:p.getInventory().getNonEquipmentItems()) if (s.is(Items.BOWL)) { s.shrink(1); p.getInventory().setChanged(); break; }
        return true;
    }
}
