package com.shaoxinpipaqu;

import com.friendswine.FriendsWine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;

/** 注册 Fabric 物品、效果与事件，实际玩法共用 26.1.2 源码。 */
public final class Pipa implements ModInitializer {
    public static final String ID = "shaoxinpipaqu";
    public static final Holder<MobEffect> PEACE = effect("forgetfulness", MobEffectCategory.BENEFICIAL, 0xD5A04D);
    public static final Holder<MobEffect> MILK = effect("great_grandma", MobEffectCategory.BENEFICIAL, 0x9CD8FA);
    public static final Holder<MobEffect> AIRBORNE = effect("airborne", MobEffectCategory.NEUTRAL, 0xC0F0FF);
    public static final Supplier<MealItem> NOODLES = item("forgetful_beef_noodles", new MealItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id("forgetful_beef_noodles"))).stacksTo(64), false));
    public static final Supplier<MealItem> DOG_MILK = item("wild_dog_milk", new MealItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id("wild_dog_milk"))).stacksTo(16), true));
    private static final AttachmentType<CompoundTag> DATA = AttachmentRegistry.create(id("entity_state"),
            builder -> builder.initializer(CompoundTag::new).persistent(CompoundTag.CODEC));

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }

    private static Holder<MobEffect> effect(String path, MobEffectCategory category, int color) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id(path), new MobEffect(category, color) {});
    }

    private static Supplier<MealItem> item(String path, MealItem item) {
        Registry.register(BuiltInRegistries.ITEM, id(path), item);
        return () -> item;
    }

    /** 飞行恢复标记与补货日期随实体保存；姿势仍通过原版效果同步。 */
    public static CompoundTag data(Entity entity) { return entity.getAttachedOrCreate(DATA); }

    @Override public void onInitialize() {
        var group = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(FriendsWine.TAB.get()).orElseThrow();
        CreativeModeTabEvents.modifyOutputEvent(group).register(entries -> {
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
        ServerTickEvents.START_LEVEL_TICK.register(MobFlight::levelTick);
        ServerLevelEvents.UNLOAD.register((server, level) -> MobFlight.unload(level));
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> PlayerFlight.tracking(player, entity));
    }
}
