package com.shaoxinpipaqu;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** 完整使用才触发；手动处理返碗，避免堆叠使用时覆盖剩余食物。 */
public final class MealItem extends Item {
    private final boolean milk;
    public MealItem(Properties properties, boolean milk) { super(properties); this.milk = milk; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return milk ? ItemUseAnimation.DRINK : ItemUseAnimation.EAT; }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) { return ItemUtils.startUsingInstantly(level, player, hand); }
    /** 服务端结算完整使用；移除旧实例再添加，避免较长旧效果隐藏本次刷新。 */
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;
        if (!level.isClientSide()) {
            var effect = milk ? Pipa.MILK : Pipa.PEACE;
            player.removeEffect(effect);
            player.addEffect(new MobEffectInstance(effect, milk ? 1200 : 3600, 0, false, false, true));
            if (!milk) player.getFoodData().eat(8, 0.6F);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                if (!milk) {
                    ItemStack bowl = new ItemStack(Items.BOWL);
                    if (stack.isEmpty()) return bowl;
                    if (!player.getInventory().add(bowl)) player.drop(bowl, false);
                }
            }
        }
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable(milk ? "tooltip.shaoxinpipaqu.milk" : "tooltip.shaoxinpipaqu.noodles"));
    }
}
