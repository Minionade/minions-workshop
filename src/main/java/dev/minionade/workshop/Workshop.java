package dev.minionade.workshop;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Workshop {
    public static final String MOD_ID = "minions_workshop";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Map<Upgrade, Item> UPGRADES = new EnumMap<>(Upgrade.class);
    public static WorkshopBlock TABLE;
    public static BlockEntityType<WorkshopBlockEntity> TABLE_ENTITY;
    public static MenuType<WorkshopMenu> TABLE_MENU;

    private Workshop() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static WorkshopBlock createTable() {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.WOOD);
        //? if >=26.1 {
        /*properties.setId(ResourceKey.create(Registries.BLOCK, id("table")));
        *///? }
        return new WorkshopBlock(properties);
    }

    public static Item.Properties itemProperties(String name) {
        Item.Properties properties = new Item.Properties();
        //? if >=26.1 {
        /*properties.setId(ResourceKey.create(Registries.ITEM, id(name)));
        *///? }
        return properties;
    }

    public static Item createTableItem() {
        Item.Properties properties = itemProperties("table");
        //? if >=26.1 {
        /*properties.useBlockDescriptionPrefix();
        *///? }
        return new BlockItem(TABLE, properties);
    }
}
