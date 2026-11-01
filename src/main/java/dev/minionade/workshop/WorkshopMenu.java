package dev.minionade.workshop;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class WorkshopMenu extends AbstractContainerMenu {
    private final WorkshopBlockEntity table;
    private final Container inventory;
    private final SimpleContainer results = new SimpleContainer(4);
    private final SimpleContainer filters = new SimpleContainer(WorkshopLayout.FILTER_SIZE);
    private final int[] progress = new int[4];
    private final int[] charges = new int[4];
    private final int[] modes = new int[WorkshopLayout.FILTER_SIZE];
    private final int playerStart;
    private int page;
    private int group;
    private int side;
    private int direction;
    private int flags;
    private int fuel;

    public WorkshopMenu(int id, Inventory player) {
        this(id, player, null);
    }

    public WorkshopMenu(int id, Inventory player, WorkshopBlockEntity table) {
        super(Workshop.TABLE_MENU, id);
        this.table = table;
        this.inventory = table == null ? new SimpleContainer(WorkshopLayout.SIZE) : table;
        addSlot(new TableSlot(WorkshopLayout.FUEL, 226, 226));
        for (int unit = 0; unit < 4; unit++) {
            int x = (unit % 2) * 128;
            int y = (unit / 2) * 87;
            for (int offset = 0; offset < WorkshopLayout.UNIT_SIZE; offset++) {
                int sx;
                int sy;
                if (offset < 9) {
                    sx = 5 + offset % 3 * 18;
                    sy = 5 + offset / 3 * 18;
                } else if (offset < 15) {
                    sx = 5 + (offset - 9) * 18;
                    sy = 65;
                } else if (offset == WorkshopLayout.CRAFT_OUTPUT) {
                    sx = 99;
                    sy = 41;
                } else if (offset == WorkshopLayout.SMELT_INPUT || offset == WorkshopLayout.SMELT_OUTPUT) {
                    sx = offset == WorkshopLayout.SMELT_INPUT ? 25 : 81;
                    sy = 23;
                } else if (offset < WorkshopLayout.STORAGE) {
                    sx = 5;
                    sy = 5 + (offset - WorkshopLayout.QUEUE) * 18;
                } else {
                    sx = 12 + (offset - WorkshopLayout.STORAGE) % 6 * 18;
                    sy = 8 + (offset - WorkshopLayout.STORAGE) / 6 * 18;
                }
                addSlot(new TableSlot(WorkshopLayout.slot(unit, offset), x + sx, y + sy));
            }
        }
        for (int unit = 0; unit < 4; unit++) {
            for (int offset = 0; offset < 8; offset++) {
                addSlot(new TableSlot(WorkshopLayout.parent(unit) + offset,
                    10 + unit % 2 * 128 + offset % 4 * 18, 20 + unit / 2 * 52 + offset / 4 * 18));
            }
        }
        for (int slot = 0; slot < 8; slot++) {
            addSlot(new TableSlot(WorkshopLayout.GLOBAL + slot, 10 + slot * 18, 146));
        }
        for (int unit = 0; unit < 4; unit++) {
            addSlot(new CraftingSlot(unit, 99 + unit % 2 * 128, 23 + unit / 2 * 87));
        }
        for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
            addSlot(new Slot(filters, filter, -1000, -1000) {
                @Override
                public boolean isActive() {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        playerStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(player, 9 + row * 9 + column, 8 + column * 18, 180 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(player, column, 8 + column * 18, 238));
        }
        sync(() -> page, value -> page = value);
        sync(() -> group, value -> group = value);
        sync(() -> side, value -> side = value);
        sync(() -> direction, value -> direction = value);
        sync(() -> table == null ? flags : selectedRule().flags(), value -> flags = value);
        sync(() -> table == null ? fuel : table.fuel(), value -> fuel = value);
        for (int unit = 0; unit < 4; unit++) {
            int index = unit;
            sync(() -> table == null ? progress[index] : table.progress(index), value -> progress[index] = value);
            sync(() -> table == null ? charges[index] : table.charges(index), value -> charges[index] = value);
        }
        for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
            int index = filter;
            sync(() -> table == null ? modes[index] : selectedRule().filter(index).mode().ordinal(), value -> modes[index] = value);
        }
    }

    private void sync(IntSupplier getter, IntConsumer setter) {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt();
            }

            @Override
            public void set(int value) {
                setter.accept(value);
            }
        });
    }

    public int page() {
        return page;
    }

    public int group() {
        return group;
    }

    public int side() {
        return side;
    }

    public int direction() {
        return direction;
    }

    public int flags() {
        return table == null ? flags : selectedRule().flags();
    }

    public int fuel() {
        return table == null ? fuel : table.fuel();
    }

    public int progress(int unit) {
        return table == null ? progress[unit] : table.progress(unit);
    }

    public int charges(int unit) {
        return table == null ? charges[unit] : table.charges(unit);
    }

    public ItemStack filter(int index) {
        return filters.getItem(index);
    }

    public TransferRule.MatchMode mode(int index) {
        return TransferRule.MatchMode.values()[Math.clamp(modes[index], 0, TransferRule.MatchMode.values().length - 1)];
    }

    public Module module(int unit) {
        return Module.of(inventory.getItem(WorkshopLayout.parent(unit)));
    }

    public int upgrades(int section, Upgrade upgrade) {
        if (section == 4 ? !upgrade.global() : !upgrade.supports(module(section))) {
            return 0;
        }
        int start = section == 4 ? WorkshopLayout.GLOBAL : WorkshopLayout.parent(section) + 1;
        int count = 0;
        for (int index = 0; index < (section == 4 ? 8 : 7); index++) {
            ItemStack stack = inventory.getItem(start + index);
            if (Upgrade.of(stack) == upgrade) {
                count += stack.getCount();
            }
        }
        return Math.min(count, upgrade.maximum());
    }

    private TransferRule selectedRule() {
        return table.rule(group, side, direction);
    }

    @Override
    public void broadcastChanges() {
        if (table != null) {
            for (int unit = 0; unit < 4; unit++) {
                ItemStack result = table.crafting(unit).map(WorkshopBlockEntity.Craft::result).orElse(ItemStack.EMPTY);
                if (!ItemStack.matches(results.getItem(unit), result)) {
                    results.setItem(unit, result.copy());
                }
            }
            for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
                ItemStack sample = selectedRule().filter(filter).item();
                if (!ItemStack.matches(filters.getItem(filter), sample)) {
                    filters.setItem(filter, sample.copy());
                }
            }
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return table == null || table.stillValid(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (table == null || !stillValid(player)) {
            Workshop.LOGGER.warn("Rejected workshop action {} for an invalid container", button);
            return false;
        }
        if (button >= 0 && button < 3) {
            page = button;
        } else if (page == 1 && button >= 10 && button < 15) {
            group = button - 10;
        } else if (page == 1 && button >= 20 && button < 26) {
            side = button - 20;
        } else if (page == 1 && button >= 30 && button < 32) {
            direction = button - 30;
        } else if (page == 1 && button == 40) {
            selectedRule().toggle(TransferRule.ENABLED);
        } else if (page == 1 && button == 41 && upgrades(4, Upgrade.AUTO_TRANSFER) > 0) {
            selectedRule().toggle(TransferRule.AUTO);
        } else if (page == 1 && button == 42 && upgrades(4, Upgrade.FILTER) > 0) {
            selectedRule().toggle(TransferRule.WHITELIST);
        } else if (page == 1 && button >= 50 && button < 60 && upgrades(4, Upgrade.FILTER) > 0) {
            selectedRule().setFilter(button - 50, getCarried());
        } else if (page == 1 && button >= 60 && button < 70 && upgrades(4, Upgrade.FILTER) > 0) {
            selectedRule().cycleMode(button - 60);
        } else if (page == 0 && button >= 70 && button < 74) {
            table.clearGrid(button - 70, player);
        } else {
            Workshop.LOGGER.warn("Rejected invalid workshop action {} on page {}", button, page);
            return false;
        }
        table.setChanged();
        broadcastChanges();
        return true;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container == inventory || slot.index >= playerStart;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < playerStart) {
            if (slot instanceof CraftingSlot && !fitsPlayerInventory(stack)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, playerStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int target = 0; target < WorkshopLayout.SIZE && !stack.isEmpty(); target++) {
                Slot destination = slots.get(target);
                if (destination.isActive() && destination.mayPlace(stack)) {
                    moved |= moveItemStackTo(stack, target, target + 1, false);
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        broadcastChanges();
        return original;
    }

    private boolean fitsPlayerInventory(ItemStack stack) {
        int remaining = stack.getCount();
        for (int index = playerStart; index < slots.size(); index++) {
            Slot slot = slots.get(index);
            ItemStack existing = slot.getItem();
            if (slot.mayPlace(stack) && (existing.isEmpty() || ItemStack.isSameItemSameComponents(stack, existing))) {
                remaining -= Math.max(0, Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack)) - existing.getCount());
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean activeUnitSlot(int slot) {
        int unit = (slot - 1) / WorkshopLayout.UNIT_SIZE;
        int offset = (slot - 1) % WorkshopLayout.UNIT_SIZE;
        return switch (module(unit)) {
            case CRAFTING -> offset < 9 || offset < 15 && upgrades(unit, Upgrade.STORAGE) > 0
                || offset == WorkshopLayout.CRAFT_OUTPUT && upgrades(unit, Upgrade.AUTO_CRAFTER) > 0;
            case SMELTING -> offset == WorkshopLayout.SMELT_INPUT || offset == WorkshopLayout.SMELT_OUTPUT
                || offset >= WorkshopLayout.QUEUE && offset < WorkshopLayout.QUEUE + upgrades(unit, Upgrade.QUEUE);
            case STORAGE -> offset >= WorkshopLayout.STORAGE;
            case EMPTY -> false;
        };
    }

    private final class TableSlot extends Slot {
        private final int tableSlot;

        TableSlot(int slot, int x, int y) {
            super(inventory, slot, x, y);
            tableSlot = slot;
        }

        @Override
        public boolean isActive() {
            if (tableSlot == WorkshopLayout.FUEL) {
                return true;
            }
            if (tableSlot >= WorkshopLayout.UPGRADES) {
                return page == 2;
            }
            return page == 0 && activeUnitSlot(tableSlot);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isActive()) {
                return false;
            }
            if (table != null) {
                return table.canPlaceItem(tableSlot, stack);
            }
            if (tableSlot == WorkshopLayout.FUEL) {
                return true;
            }
            if (tableSlot >= WorkshopLayout.UPGRADES) {
                if (tableSlot < WorkshopLayout.GLOBAL && (tableSlot - WorkshopLayout.UPGRADES) % 8 == 0) {
                    return Module.of(stack) != Module.EMPTY;
                }
                Upgrade upgrade = Upgrade.of(stack);
                int section = tableSlot >= WorkshopLayout.GLOBAL ? 4 : (tableSlot - WorkshopLayout.UPGRADES) / 8;
                return upgrade != null && (section == 4 ? upgrade.global() : upgrade.supports(module(section)));
            }
            int offset = (tableSlot - 1) % WorkshopLayout.UNIT_SIZE;
            return offset != WorkshopLayout.CRAFT_OUTPUT && offset != WorkshopLayout.SMELT_OUTPUT;
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            if (tableSlot >= WorkshopLayout.UPGRADES && tableSlot < WorkshopLayout.GLOBAL
                && (tableSlot - WorkshopLayout.UPGRADES) % 8 == 0) {
                return 1;
            }
            if (tableSlot >= WorkshopLayout.UPGRADES) {
                Upgrade upgrade = Upgrade.of(stack);
                if (upgrade != null) {
                    int section = tableSlot >= WorkshopLayout.GLOBAL ? 4 : (tableSlot - WorkshopLayout.UPGRADES) / 8;
                    int start = section == 4 ? WorkshopLayout.GLOBAL : WorkshopLayout.parent(section) + 1;
                    int count = 0;
                    for (int index = 0; index < (section == 4 ? 8 : 7); index++) {
                        int slot = start + index;
                        ItemStack existing = inventory.getItem(slot);
                        if (slot != tableSlot && Upgrade.of(existing) == upgrade) {
                            count += existing.getCount();
                        }
                    }
                    return Math.max(0, Math.min(stack.getMaxStackSize(), upgrade.maximum() - count));
                }
            }
            return Math.min(64, stack.getMaxStackSize());
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (table != null && tableSlot > 0 && tableSlot < WorkshopLayout.UPGRADES
                && (tableSlot - 1) % WorkshopLayout.UNIT_SIZE == WorkshopLayout.SMELT_OUTPUT) {
                table.awardExperience((tableSlot - 1) / WorkshopLayout.UNIT_SIZE, player);
            }
            super.onTake(player, stack);
        }
    }

    private final class CraftingSlot extends Slot {
        private final int unit;

        CraftingSlot(int unit, int x, int y) {
            super(results, unit, x, y);
            this.unit = unit;
        }

        @Override
        public boolean isActive() {
            return page == 0 && module(unit) == Module.CRAFTING;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive() && hasItem() && (table == null || table.crafting(unit)
                .map(craft -> ItemStack.matches(craft.result(), getItem())).orElse(false));
        }

        @Override
        public ItemStack remove(int count) {
            return results.removeItemNoUpdate(unit);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (table != null) {
                table.craft(unit, player, false);
            }
            setChanged();
            broadcastChanges();
        }
    }
}
