package dev.minionade.workshop;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;

public final class UpgradeItem extends Item {
    private final Upgrade upgrade;

    public UpgradeItem(Upgrade upgrade) {
        super(Workshop.itemProperties(upgrade.id()).component(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("item.minions_workshop." + upgrade.id() + ".description"),
            Component.translatable("tooltip.minions_workshop.maximum", upgrade.maximum())
        ))));
        this.upgrade = upgrade;
    }

    public Upgrade upgrade() {
        return upgrade;
    }
}
