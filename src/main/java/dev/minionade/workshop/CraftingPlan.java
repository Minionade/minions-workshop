package dev.minionade.workshop;

import java.util.List;
import net.minecraft.world.item.ItemStack;

final class CraftingPlan {
    private CraftingPlan() {}

    static int[] create(List<ItemStack> items, boolean storageEnabled, boolean preservePattern) {
        if (items.size() != 15) {
            throw new IllegalArgumentException("A crafting plan requires nine grid and six storage slots");
        }
        int[] plan = new int[15];
        for (int grid = 0; grid < 9; grid++) {
            ItemStack ingredient = items.get(grid);
            if (ingredient.isEmpty()) {
                continue;
            }
            int source = -1;
            if (storageEnabled) {
                for (int storage = 9; storage < 15; storage++) {
                    ItemStack stack = items.get(storage);
                    if (ItemStack.isSameItemSameComponents(ingredient, stack) && stack.getCount() > plan[storage]) {
                        source = storage;
                        break;
                    }
                }
            }
            if (source == -1) {
                int largest = 0;
                for (int other = 0; other < 9; other++) {
                    ItemStack stack = items.get(other);
                    int available = stack.getCount() - plan[other] - (preservePattern ? 1 : 0);
                    if (ItemStack.isSameItemSameComponents(ingredient, stack) && available > largest) {
                        source = other;
                        largest = available;
                    }
                }
            }
            if (source == -1) {
                return null;
            }
            plan[source]++;
        }
        return plan;
    }
}
