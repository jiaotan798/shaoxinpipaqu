package com.shaoxinpipaqu;

import com.friendswine.FriendsWine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;

/** 注册 Fabric 物品、效果与事件，实际玩法共用 1.21.1 源码。 */
public final class Pipa implements ModInitializer {
    public static final String ID = "shaoxinpipaqu";
    public static final Holder<MobEffect> PEACE = effect("forgetfulness", MobEffectCategory.BENEFICIAL, 0xD5A04D);
    public static final Holder<MobEffect> MILK = effect("great_grandma", MobEffectCategory.BENEFICIAL, 0x9CD8FA);
    public static final Holder<MobEffect> AIRBORNE = effect("airborne", MobEffectCategory.NEUTRAL, 0xC0F0FF);
    public static final Supplier<MealItem> NOODLES = item("forgetful_beef_noodles", new MealItem(new Item.Properties()
            .stacksTo(64).food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.6F).alwaysEdible().build()), false));
    public static final Supplier<MealItem> DOG_MILK = item("wild_dog_milk", new MealItem(new Item.Properties().stacksTo(16), true));
    private static final AttachmentType<CompoundTag> DATA = AttachmentRegistry.create(id("entity_state"),
            builder -> builder.initializer(CompoundTag::new).persistent(CompoundTag.CODEC));

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }

    private static Holder<MobEffect> effect(String path, MobEffectCategory category, int color) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id(path), new MobEffect(category, color) {});
    }

    private static Supplier<MealItem> item(String path, MealItem item) {
        Registry.register(BuiltInRegistries.ITEM, id(path), item);
        return () -> item;
    }

    /** 飞行恢复标记与补货日期随实体保存；姿势仍通过原版效果同步。 */
    public static CompoundTag data(Entity entity) { return entity.getAttachedOrCreate(DATA); }

    // 每刻查恢复状态的普通生物不分配附加数据，也不写入空存档条目。
    static boolean hasData(Entity entity, String key) {
        var tag = entity.getAttached(DATA);
        return tag != null && tag.contains(key);
    }

    static boolean dataFlag(Entity entity, String key) {
        var tag = entity.getAttached(DATA);
        return tag != null && tag.getBoolean(key);
    }

    @Override public void onInitialize() {
        var group = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(FriendsWine.TAB.get()).orElseThrow();
        ItemGroupEvents.modifyEntriesEvent(group).register(entries -> {
            entries.accept(NOODLES.get());
            entries.accept(DOG_MILK.get());
        });
        Network.register();
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                RemoteControl.held(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackBlockCallback.EVENT.register((player, level, hand, pos, face) ->
                RemoteControl.held(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (entity instanceof Villager villager) ExtraTrades.interact(villager, player, player.getItemInHand(hand));
            return InteractionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !PeaceAura.cancelDamage(source, entity));
        ServerTickEvents.START_WORLD_TICK.register(MobFlight::levelTick);
        ServerWorldEvents.UNLOAD.register((server, level) -> MobFlight.unload(level));
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> PlayerFlight.tracking(player, entity));
    }
}
