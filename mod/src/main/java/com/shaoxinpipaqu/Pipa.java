package com.shaoxinpipaqu;

import com.friendswine.FriendsWine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.*;

/** 附属注册入口；前置物品与实体只引用，不重复注册。 */
@Mod(Pipa.ID)
public final class Pipa {
    public static final String ID = "shaoxinpipaqu";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ID);
    public static final DeferredHolder<MobEffect, MobEffect> PEACE = EFFECTS.register("forgetfulness", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD5A04D) {});
    public static final DeferredHolder<MobEffect, MobEffect> MILK = EFFECTS.register("great_grandma", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x9CD8FA) {});
    // 内部姿势标记走原版效果同步，控制不依赖客户端画面设置。
    public static final DeferredHolder<MobEffect, MobEffect> AIRBORNE = EFFECTS.register("airborne", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0xC0EFFF) {});
    public static final DeferredItem<MealItem> NOODLES = ITEMS.register("forgetful_beef_noodles", () -> new MealItem(new Item.Properties().stacksTo(64).food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.6F).alwaysEdible().build()), false));
    public static final DeferredItem<MealItem> DOG_MILK = ITEMS.register("wild_dog_milk", () -> new MealItem(new Item.Properties().stacksTo(16), true));

    public Pipa(IEventBus bus) {
        ITEMS.register(bus);
        EFFECTS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTab() == FriendsWine.TAB.get()) { e.accept(NOODLES); e.accept(DOG_MILK); }
        });
        bus.addListener(Network::register);
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent e) -> { if (RemoteControl.held(e.getEntity())) e.setCanceled(true); });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> { if (RemoteControl.held(e.getEntity())) e.setCanceled(true); });
        NeoForge.EVENT_BUS.addListener((LivingChangeTargetEvent e) -> {
            if (e.getEntity() instanceof Mob mob && PeaceAura.cancelTarget(mob,e.getNewAboutToBeSetTarget())) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent e) -> {
            if (PeaceAura.cancelDamage(e.getSource(),e.getEntity())) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Pre e) -> { if (e.getEntity() instanceof Mob mob) PeaceAura.tick(mob); });
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre e) -> { if (e.getLevel() instanceof ServerLevel level) MobFlight.levelTick(level); });
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e) -> { if (e.getEntity() instanceof Mob mob) MobFlight.entityTick(mob); });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> { if (e.getEntity() instanceof ServerPlayer p) PlayerFlight.tick(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e) -> {
            if (e.getEntity() instanceof ServerPlayer p) PlayerFlight.tracking(p,e.getTarget());
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (e.getTarget() instanceof Villager v) ExtraTrades.interact(v,e.getEntity(),e.getItemStack());
        });
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e) -> { if (e.getEntity() instanceof Villager v) ExtraTrades.tick(v); });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e) -> MobFlight.unload(e.getLevel()));
    }

    /** 维持已发布版本在 NeoForge 实体持久数据中的存档位置。 */
    public static CompoundTag data(Entity entity) { return entity.getPersistentData(); }
    static boolean hasData(Entity entity, String key) { return data(entity).contains(key); }
    static boolean dataFlag(Entity entity, String key) { return data(entity).getBoolean(key); }
}
