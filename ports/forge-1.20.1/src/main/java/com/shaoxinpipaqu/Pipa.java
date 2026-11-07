package com.shaoxinpipaqu;

import com.friendswine.FriendsWine;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Forge 注册和事件桥接，飞行与交易逻辑由 1.20.1 两个加载器共用。 */
@Mod(Pipa.ID)
public final class Pipa {
    public static final String ID = "shaoxinpipaqu";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ID);
    public static final RegistryObject<MobEffect> PEACE_REGISTRY = EFFECTS.register("forgetfulness", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD5A04D) {});
    public static final RegistryObject<MobEffect> MILK_REGISTRY = EFFECTS.register("great_grandma", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x9CD8FA) {});
    public static final RegistryObject<MobEffect> AIRBORNE_REGISTRY = EFFECTS.register("airborne", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0xC0F4FF) {});
    public static final RegistryObject<MealItem> NOODLES = ITEMS.register("forgetful_beef_noodles", () -> new MealItem(new Item.Properties().stacksTo(64), false));
    public static final RegistryObject<MealItem> DOG_MILK = ITEMS.register("wild_dog_milk", () -> new MealItem(new Item.Properties().stacksTo(16), true));
    public static MobEffect peace() { return PEACE_REGISTRY.get(); }
    public static MobEffect milk() { return MILK_REGISTRY.get(); }
    public static MobEffect airborne() { return AIRBORNE_REGISTRY.get(); }
    public static CompoundTag data(Entity entity) { return ((PipaEntityData) entity).pipa$data(); }

    public Pipa() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        EFFECTS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTab() == FriendsWine.TAB.get()) {
                event.accept(NOODLES);
                event.accept(DOG_MILK);
            }
        });
        Network.register();
        var events = MinecraftForge.EVENT_BUS;
        events.addListener((TickEvent.LevelTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START && event.level instanceof ServerLevel level) MobFlight.levelTick(level);
        });
        events.addListener((TickEvent.PlayerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) PlayerFlight.tick(player);
        });
        events.addListener((LevelEvent.Unload event) -> MobFlight.unload(event.getLevel()));
        events.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getEntity() instanceof ServerPlayer player) PlayerFlight.tracking(player, event.getTarget());
        });
        events.addListener((PlayerInteractEvent.EntityInteract event) -> {
            if (event.getTarget() instanceof Villager villager) ExtraTrades.interact(villager, event.getEntity(), event.getItemStack());
        });
        events.addListener((LivingChangeTargetEvent event) -> {
            if (event.getEntity() instanceof Mob mob && PeaceAura.cancelTarget(mob, event.getNewTarget())) event.setCanceled(true);
        });
        events.addListener((LivingAttackEvent event) -> {
            if (PeaceAura.cancelDamage(event.getSource(), event.getEntity())) event.setCanceled(true);
        });
        events.addListener((AttackEntityEvent event) -> {
            if (RemoteControl.held(event.getEntity())) event.setCanceled(true);
        });
        events.addListener((PlayerInteractEvent.LeftClickBlock event) -> {
            if (RemoteControl.held(event.getEntity())) event.setCanceled(true);
        });
        if (FMLEnvironment.dist == Dist.CLIENT) com.shaoxinpipaqu.client.PipaClient.register(bus);
    }
}
