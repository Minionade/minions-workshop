package dev.minionade.workshop;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum Module {
    EMPTY, CRAFTING, SMELTING, STORAGE;

    public static Module of(ItemStack stack) {
        if (stack.is(Items.CRAFTING_TABLE)) {
            return CRAFTING;
        }
        if (stack.is(Items.FURNACE)) {
            return SMELTING;
        }
        if (stack.is(Items.CHEST)) {
            return STORAGE;
        }
        return EMPTY;
    }
}
