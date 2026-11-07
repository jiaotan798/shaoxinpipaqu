package com.shaoxinpipaqu;

import com.friendswine.FriendsWine;
import java.util.function.Supplier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;

/** Fabric 注册和事件桥接，飞行与交易逻辑由 1.20.1 两个加载器共用。 */
public final class Pipa implements ModInitializer {
    public static final String ID = "shaoxinpipaqu";
    private static final MobEffect PEACE = effect("forgetfulness", MobEffectCategory.BENEFICIAL, 0xD5A04D);
    private static final MobEffect MILK = effect("great_grandma", MobEffectCategory.BENEFICIAL, 0x9CD8FA);
    private static final MobEffect AIRBORNE = effect("airborne", MobEffectCategory.NEUTRAL, 0xC0F4FF);
    public static final Supplier<MealItem> NOODLES = item("forgetful_beef_noodles", new MealItem(new Item.Properties().stacksTo(64), false));
    public static final Supplier<MealItem> DOG_MILK = item("wild_dog_milk", new MealItem(new Item.Properties().stacksTo(16), true));

    private static MobEffect effect(String name, MobEffectCategory category, int color) {
        return Registry.register(BuiltInRegistries.MOB_EFFECT, new ResourceLocation(ID, name), new MobEffect(category, color) {});
    }

    private static Supplier<MealItem> item(String name, MealItem item) {
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(ID, name), item);
        return () -> item;
    }

    public static MobEffect peace() { return PEACE; }
    public static MobEffect milk() { return MILK; }
    public static MobEffect airborne() { return AIRBORNE; }
    public static CompoundTag data(Entity entity) { return ((PipaEntityData) entity).pipa$data(); }

    @Override public void onInitialize() {
        var tabKey = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(FriendsWine.TAB.get()).orElseThrow();
        ItemGroupEvents.modifyEntriesEvent(tabKey).register(entries -> {
            entries.accept(NOODLES.get());
            entries.accept(DOG_MILK.get());
        });
        Network.register();
        ServerTickEvents.START_WORLD_TICK.register(MobFlight::levelTick);
        ServerTickEvents.END_WORLD_TICK.register(level -> level.players().forEach(PlayerFlight::tick));
        ServerWorldEvents.UNLOAD.register((server, level) -> MobFlight.unload(level));
        EntityTrackingEvents.START_TRACKING.register((target, player) -> PlayerFlight.tracking(player, target));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (entity instanceof Villager villager) ExtraTrades.interact(villager, player, player.getItemInHand(hand));
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> RemoteControl.held(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> RemoteControl.held(player) ? InteractionResult.FAIL : InteractionResult.PASS);
    }
}
