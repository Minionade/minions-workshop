//? if fabric {
package dev.minionade.workshop.fabric;

import dev.minionade.workshop.Upgrade;
import dev.minionade.workshop.UpgradeItem;
import dev.minionade.workshop.Workshop;
import dev.minionade.workshop.WorkshopBlockEntity;
import dev.minionade.workshop.WorkshopMenu;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import team.reborn.energy.api.EnergyStorage;

public final class WorkshopFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Workshop.TABLE = Registry.register(BuiltInRegistries.BLOCK, Workshop.id("table"), Workshop.createTable());
        Registry.register(BuiltInRegistries.ITEM, Workshop.id("table"), Workshop.createTableItem());
        for (Upgrade upgrade : Upgrade.values()) {
            UpgradeItem item = Registry.register(BuiltInRegistries.ITEM, Workshop.id(upgrade.id()), new UpgradeItem(upgrade));
            Workshop.UPGRADES.put(upgrade, item);
        }
        Workshop.TABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Workshop.id("table"),
            FabricBlockEntityTypeBuilder.create(WorkshopBlockEntity::new, Workshop.TABLE).build());
        Workshop.TABLE_MENU = Registry.register(BuiltInRegistries.MENU, Workshop.id("table"),
            new MenuType<>(WorkshopMenu::new, FeatureFlags.DEFAULT_FLAGS));
        // Block the generic container fallback for unsided requests.
        ItemStorage.SIDED.registerForBlockEntity((table, direction) ->
            direction == null ? Storage.empty() : InventoryStorage.of(table, direction), Workshop.TABLE_ENTITY);
        EnergyStorage.SIDED.registerForBlockEntity((table, side) -> table.resourceHandlers.energy(side), Workshop.TABLE_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity((table, side) -> table.resourceHandlers.fluid(side), Workshop.TABLE_ENTITY);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(Workshop.TABLE);
            Workshop.UPGRADES.values().forEach(entries::accept);
        });
    }
}
//? }
