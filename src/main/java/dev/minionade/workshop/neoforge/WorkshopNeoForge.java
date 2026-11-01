//? if neoforge {
/*package dev.minionade.workshop.neoforge;

import dev.minionade.workshop.Upgrade;
import dev.minionade.workshop.UpgradeItem;
import dev.minionade.workshop.Workshop;
import dev.minionade.workshop.WorkshopBlockEntity;
import dev.minionade.workshop.WorkshopMenu;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
//? if <26.1 {
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
//? } else {
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
//? }

@Mod(Workshop.MOD_ID)
public final class WorkshopNeoForge {
    public WorkshopNeoForge(IEventBus bus) {
        bus.addListener(this::register);
        bus.addListener(this::creative);
        bus.addListener(this::capabilities);
        //? if >=26.1 {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
        //? } else {
        if (FMLEnvironment.dist == Dist.CLIENT) {
        //? }
            WorkshopNeoForgeClient.register(bus);
        }
    }

    private void register(RegisterEvent event) {
        event.register(Registries.BLOCK, Workshop.id("table"), () -> Workshop.TABLE = Workshop.createTable());
        event.register(Registries.ITEM, Workshop.id("table"), Workshop::createTableItem);
        for (Upgrade upgrade : Upgrade.values()) {
            event.register(Registries.ITEM, Workshop.id(upgrade.id()), () -> {
                UpgradeItem item = new UpgradeItem(upgrade);
                Workshop.UPGRADES.put(upgrade, item);
                return item;
            });
        }
        event.register(Registries.BLOCK_ENTITY_TYPE, Workshop.id("table"), () -> {
            //? if >=26.1 {
            return Workshop.TABLE_ENTITY = new BlockEntityType<>(WorkshopBlockEntity::new, Set.of(Workshop.TABLE));
            //? } else {
            return Workshop.TABLE_ENTITY = BlockEntityType.Builder.of(WorkshopBlockEntity::new, Workshop.TABLE).build(null);
            //? }
        });
        event.register(Registries.MENU, Workshop.id("table"), () ->
            Workshop.TABLE_MENU = new MenuType<>(WorkshopMenu::new, FeatureFlags.DEFAULT_FLAGS));
    }

    private void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(Workshop.TABLE);
            Workshop.UPGRADES.values().forEach(event::accept);
        }
    }

    private void capabilities(RegisterCapabilitiesEvent event) {
        //? if <26.1 {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Workshop.TABLE_ENTITY,
            (table, side) -> side == null ? null : new SidedInvWrapper(table, side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Workshop.TABLE_ENTITY,
            (table, side) -> table.resourceHandlers.energy(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Workshop.TABLE_ENTITY,
            (table, side) -> table.resourceHandlers.fluid(side));
        //? } else {
        event.registerBlockEntity(Capabilities.Item.BLOCK, Workshop.TABLE_ENTITY,
            (table, side) -> side == null ? null : new WorldlyContainerWrapper(table, side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, Workshop.TABLE_ENTITY,
            (table, side) -> table.resourceHandlers.energy(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, Workshop.TABLE_ENTITY,
            (table, side) -> table.resourceHandlers.fluid(side));
        //? }
        WorkshopIndustrialEnergy.register(event);
    }
}
*///? }
