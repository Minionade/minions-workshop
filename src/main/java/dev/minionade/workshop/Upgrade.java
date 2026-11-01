package dev.minionade.workshop;

import java.util.Locale;
import net.minecraft.world.item.ItemStack;

public enum Upgrade {
    BLANK(0),
    AUTO_CRAFTER(1, Module.CRAFTING),
    STORAGE(1, Module.CRAFTING),
    CHARGED(8, Module.CRAFTING, Module.SMELTING),
    SPEED(8, Module.CRAFTING, Module.SMELTING),
    QUEUE(3, Module.SMELTING),
    AUTO_TRANSFER(1),
    FILTER(1),
    TRANSFER(6),
    ENERGY(1),
    FLUID(1);

    private final int maximum;
    private final Module[] parents;

    Upgrade(int maximum, Module... parents) {
        this.maximum = maximum;
        this.parents = parents;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int maximum() {
        return maximum;
    }

    public boolean global() {
        return this == AUTO_TRANSFER || this == FILTER || this == TRANSFER || this == ENERGY || this == FLUID;
    }

    public boolean supports(Module module) {
        for (Module parent : parents) {
            if (parent == module) {
                return true;
            }
        }
        return false;
    }

    public static Upgrade of(ItemStack stack) {
        return stack.getItem() instanceof UpgradeItem item ? item.upgrade() : null;
    }
}
