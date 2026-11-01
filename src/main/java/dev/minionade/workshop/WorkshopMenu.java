package dev.minionade.workshop;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class WorkshopMenu extends AbstractContainerMenu {
    public enum Face {
        TOP(1, 0), LEFT(0, 1), FRONT(1, 1), RIGHT(2, 1), BOTTOM(1, 2), BACK(2, 2);

        public final int column;
        public final int row;

        Face(int column, int row) {
            this.column = column;
            this.row = row;
        }

        public Direction direction(Direction front) {
            if (front.getAxis().isVertical()) {
                throw new IllegalArgumentException("Workshop facing must be horizontal");
            }
            return switch (this) {
                case TOP -> Direction.UP;
                case BOTTOM -> Direction.DOWN;
                case FRONT -> front;
                case BACK -> front.getOpposite();
                case LEFT -> front.getClockWise();
                case RIGHT -> front.getCounterClockWise();
            };
        }
    }

    public enum FaceMode implements StringRepresentable {
        NONE("none"), INPUT("input"), OUTPUT("output"), BOTH("both");

        private final String name;

        FaceMode(String name) {
            this.name = name;
        }

        public FaceMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 238;
    public static final int PANEL_WIDTH = 176;
    public static final int MODULE_TAB_X = 8;
    public static final int MODULE_TAB_Y = 18;
    public static final int MODULE_TAB_STEP = 32;
    public static final int FUEL_X = 8;
    public static final int FUEL_Y = 128;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 158;
    public static final int HOTBAR_Y = 216;
    private final WorkshopBlockEntity table;
    private final Container inventory;
    private final SimpleContainer results = new SimpleContainer(4);
    private final SimpleContainer filters = new SimpleContainer(WorkshopLayout.FILTER_SIZE * 2);
    private final int[] progress = new int[4];
    private final int[] charges = new int[4];
    private final int[] modes = new int[WorkshopLayout.FILTER_SIZE];
    private final int[] outputModes = new int[WorkshopLayout.FILTER_SIZE];
    private final int[] faceFlags = new int[6];
    private final int playerStart;
    private final int playerEnd;
    private int page;
    private int selectedUnit;
    private int group;
    private int side;
    private int direction;
    private int flags;
    private int fuel;
    private int energy;
    private int lava;
    private int front = Direction.NORTH.ordinal();
    private boolean filterFuel;
    private boolean sidePanelReady = true;

    public WorkshopMenu(int id, Inventory player) {
        this(id, player, null);
    }

    public WorkshopMenu(int id, Inventory player, WorkshopBlockEntity table) {
        super(Workshop.TABLE_MENU, id);
        this.table = table;
        this.inventory = table == null ? new SimpleContainer(WorkshopLayout.SIZE) : table;
        addSlot(new TableSlot(WorkshopLayout.FUEL, FUEL_X, FUEL_Y));
        for (int unit = 0; unit < 4; unit++) {
            for (int offset = 0; offset < WorkshopLayout.UNIT_SIZE; offset++) {
                int sx;
                int sy;
                if (offset < 9) {
                    sx = 8 + offset % 3 * 18;
                    sy = 54 + offset / 3 * 18;
                } else if (offset < 15) {
                    sx = 8 + (offset - WorkshopLayout.CRAFT_STORAGE) * 18;
                    sy = 108;
                } else if (offset == WorkshopLayout.CRAFT_OUTPUT) {
                    sx = 142;
                    sy = 72;
                } else if (offset == WorkshopLayout.SMELT_INPUT || offset == WorkshopLayout.SMELT_OUTPUT) {
                    sx = offset == WorkshopLayout.SMELT_INPUT ? 54 : 114;
                    sy = 72;
                } else if (offset < WorkshopLayout.STORAGE) {
                    sx = 8;
                    sy = 54 + (offset - WorkshopLayout.QUEUE) * 18;
                } else {
                    sx = 34 + (offset - WorkshopLayout.STORAGE) % 6 * 18;
                    sy = 54 + (offset - WorkshopLayout.STORAGE) / 6 * 18;
                }
                addSlot(new TableSlot(WorkshopLayout.slot(unit, offset), sx, sy));
            }
        }
        for (int unit = 0; unit < 4; unit++) {
            for (int offset = 0; offset < 8; offset++) {
                int x = offset == 0 ? MODULE_TAB_X + unit * MODULE_TAB_STEP + 4
                    : WorkshopSidebar.TAB_X + 8 + (offset - 1) % 4 * 18;
                int y = offset == 0 ? MODULE_TAB_Y + 4
                    : WorkshopSidebar.TAB_Y + 40 + (offset - 1) / 4 * 18;
                addSlot(new TableSlot(WorkshopLayout.parent(unit) + offset, x, y));
            }
        }
        for (int slot = 0; slot < 8; slot++) {
            addSlot(new TableSlot(WorkshopLayout.GLOBAL + slot,
                WorkshopSidebar.TAB_X + 8 + slot % 4 * 18, WorkshopSidebar.TAB_Y + 95 + slot / 4 * 18));
        }
        for (int unit = 0; unit < 4; unit++) {
            addSlot(new CraftingSlot(unit, 110, 72));
        }
        for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
            addSlot(filterSlot(filter));
        }
        playerStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(player, 9 + row * 9 + column, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(player, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
        playerEnd = slots.size();
        for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
            addSlot(filterSlot(WorkshopLayout.FILTER_SIZE + filter));
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
            sync(() -> table == null ? modes[index] : table.rule(group, side, 0).filter(index).mode().ordinal(),
                value -> modes[index] = value);
        }
        sync(() -> selectedUnit, value -> selectedUnit = value);
        for (Direction face : Direction.values()) {
            int index = face.ordinal();
            sync(() -> faceFlags(index), value -> faceFlags[index] = value);
        }
        sync(() -> table == null ? front : table.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).ordinal(),
            value -> front = value);
        sync(() -> filterFuel ? 1 : 0, value -> filterFuel = value != 0);
        for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
            int index = filter;
            sync(() -> table == null ? outputModes[index] : table.rule(group, side, 1).filter(index).mode().ordinal(),
                value -> outputModes[index] = value);
        }
        sync(() -> energy() & 0xFFFF, value -> energy = energy & 0xFFFF0000 | value & 0xFFFF);
        sync(() -> energy() >>> 16, value -> energy = energy & 0xFFFF | (value & 0xFFFF) << 16);
        sync(this::lava, value -> lava = value);
    }

    private Slot filterSlot(int index) {
        return new Slot(filters, index, -1000, -1000) {
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
        };
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

    public boolean filterFuel() {
        return filterFuel;
    }

    private void updateGroup() {
        group = page == 3 || page == 4 && filterFuel ? 4 : selectedUnit();
    }

    public int selectedUnit() {
        return Math.clamp(selectedUnit, 0, WorkshopLayout.UNIT_COUNT - 1);
    }

    public void setSidePanelReady(boolean ready) {
        sidePanelReady = ready;
    }

    public static boolean isModuleSlot(int slot) {
        return slot >= WorkshopLayout.UPGRADES && slot < WorkshopLayout.GLOBAL
            && (slot - WorkshopLayout.UPGRADES) % WorkshopLayout.UPGRADE_SIZE == 0;
    }

    public ItemStack moduleItem(int unit) {
        return inventory.getItem(WorkshopLayout.parent(unit));
    }

    public int side() {
        return side;
    }

    public Direction front() {
        return Direction.values()[front];
    }

    private int faceFlags(int face) {
        return table == null ? faceFlags[face]
            : table.rule(group, face, 0).flags() | table.rule(group, face, 1).flags() << 3;
    }

    public FaceMode faceMode(int face) {
        int state = faceFlags(face);
        int mode = (state & TransferRule.ENABLED) != 0 ? 1 : 0;
        if ((state & TransferRule.ENABLED << 3) != 0) {
            mode |= 2;
        }
        return FaceMode.values()[mode];
    }

    public boolean faceAutomatic(int face, int direction) {
        validateDirection(direction);
        return (faceFlags(face) & TransferRule.AUTO << (direction * 3)) != 0;
    }

    private static void validateDirection(int direction) {
        if (direction < 0 || direction > 1) {
            throw new IllegalArgumentException("Invalid transfer direction: " + direction);
        }
    }

    public int direction() {
        return direction;
    }

    public int flags() {
        return table == null ? flags : selectedRule().flags();
    }

    public int flags(int flow) {
        validateDirection(flow);
        return faceFlags(side) >> (flow * 3) & 7;
    }

    public int fuel() {
        return table == null ? fuel : table.fuel();
    }

    public int energy() {
        return table == null ? energy : table.power().energy();
    }

    public int lava() {
        return table == null ? lava : table.power().lava() / WorkshopPower.FLUID_PER_MILLIBUCKET;
    }

    public int progress(int unit) {
        return table == null ? progress[unit] : table.progress(unit);
    }

    public int charges(int unit) {
        return table == null ? charges[unit] : table.charges(unit);
    }

    public ItemStack filter(int index) {
        return filter(direction, index);
    }

    public ItemStack filter(int flow, int index) {
        validateDirection(flow);
        return filters.getItem(flow * WorkshopLayout.FILTER_SIZE + index);
    }

    public TransferRule.MatchMode mode(int index) {
        return mode(direction, index);
    }

    public TransferRule.MatchMode mode(int flow, int index) {
        validateDirection(flow);
        int value = (flow == 0 ? modes : outputModes)[index];
        return TransferRule.MatchMode.values()[Math.clamp(value, 0, TransferRule.MatchMode.values().length - 1)];
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

    private void setFaceMode(int face, FaceMode mode) {
        for (int flow = 0; flow < 2; flow++) {
            TransferRule rule = table.rule(group, face, flow);
            boolean enabled = mode == FaceMode.BOTH || mode == (flow == 0 ? FaceMode.INPUT : FaceMode.OUTPUT);
            if (rule.has(TransferRule.ENABLED) != enabled) {
                rule.toggle(TransferRule.ENABLED);
            }
        }
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
            for (int flow = 0; flow < 2; flow++) {
                for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
                    int index = flow * WorkshopLayout.FILTER_SIZE + filter;
                    ItemStack sample = table.rule(group, side, flow).filter(filter).item();
                    if (!ItemStack.matches(filters.getItem(index), sample)) {
                        filters.setItem(index, sample.copy());
                    }
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
        boolean transferPage = page == 1 || page == 3;
        boolean filterPage = page == 4;
        if (button >= 0 && button < 5) {
            if (button == 4 && transferPage) {
                filterFuel = page == 3;
            }
            page = button;
            updateGroup();
            if (page == 4) {
                direction = 0;
            }
        } else if (button == 9) {
            if (!exchangeLavaBucket(player)) {
                return false;
            }
        } else if (filterPage && button == 6) {
            filterFuel = !filterFuel;
            updateGroup();
        } else if (filterPage && (button == 7 || button == 8)) {
            side = Math.floorMod(side + (button == 7 ? 1 : -1), faceFlags.length);
        } else if (transferPage && button >= 20 && button < 26) {
            side = button - 20;
        } else if (transferPage && button >= 30 && button < 32) {
            direction = button - 30;
        } else if (transferPage && button == 40) {
            selectedRule().toggle(TransferRule.ENABLED);
        } else if (transferPage && button == 41 && upgrades(4, Upgrade.AUTO_TRANSFER) > 0) {
            selectedRule().toggle(TransferRule.AUTO);
        } else if (filterPage && (button == 42 || button == 47) && upgrades(4, Upgrade.FILTER) > 0) {
            table.rule(group, side, button == 42 ? 0 : 1).toggle(TransferRule.WHITELIST);
        } else if (transferPage && button >= 43 && button <= 46 && upgrades(4, Upgrade.AUTO_TRANSFER) > 0) {
            int flow = (button - 43) % 2;
            boolean automatic = !faceAutomatic(side, flow);
            for (int face = 0; face < faceFlags.length; face++) {
                if (button >= 45 || face == side) {
                    TransferRule rule = table.rule(group, face, flow);
                    if (rule.has(TransferRule.AUTO) != automatic) {
                        rule.toggle(TransferRule.AUTO);
                    }
                }
            }
        } else if (filterPage && button >= 50 && button < 70 && upgrades(4, Upgrade.FILTER) > 0) {
            TransferRule rule = table.rule(group, side, 0);
            if (button < 60) {
                rule.setFilter(button - 50, getCarried());
            } else {
                rule.cycleMode(button - 60);
            }
        } else if (filterPage && button >= 80 && button < 100 && upgrades(4, Upgrade.FILTER) > 0) {
            TransferRule rule = table.rule(group, side, 1);
            if (button < 90) {
                rule.setFilter(button - 80, getCarried());
            } else {
                rule.cycleMode(button - 90);
            }
        } else if (button == 70 + selectedUnit() && module(selectedUnit()) == Module.CRAFTING) {
            table.clearGrid(button - 70, player);
        } else if (transferPage && button >= 100 && button < 124) {
            side = (button - 100) % faceFlags.length;
            boolean reset = (button - 100) / faceFlags.length % 2 == 1;
            FaceMode mode = reset ? FaceMode.NONE : faceMode(side).next();
            for (int face = 0; face < faceFlags.length; face++) {
                if (button >= 112 || face == side) {
                    setFaceMode(face, mode);
                }
            }
        } else {
            Workshop.LOGGER.warn("Rejected invalid workshop action {} on page {}", button, page);
            return false;
        }
        table.setChanged();
        if (transferPage && group == 4 && button >= 20) {
            table.resourceConfigurationChanged();
        }
        broadcastChanges();
        return true;
    }

    private boolean exchangeLavaBucket(Player player) {
        String error = null;
        ItemStack carried = getCarried();
        boolean filling = carried.is(Items.LAVA_BUCKET);
        if (upgrades(4, Upgrade.FLUID) == 0) {
            error = "lava.locked";
        } else if (!filling && !carried.is(Items.BUCKET)) {
            error = "lava.help";
        } else if (filling && table.power().insertLava(WorkshopPower.FLUID_PER_BUCKET, true) < WorkshopPower.FLUID_PER_BUCKET) {
            error = "lava.full";
        } else if (!filling && table.power().lava() < WorkshopPower.FLUID_PER_BUCKET) {
            error = "lava.empty";
        }
        ItemStack result = new ItemStack(filling ? Items.BUCKET : Items.LAVA_BUCKET);
        if (error == null && carried.getCount() > 1 && !fitsPlayerInventory(result)) {
            error = "lava.inventory_full";
        }
        if (error != null) {
            //? if >=26.1 {
            /*player.sendSystemMessage(Component.translatable("gui.minions_workshop." + error));
            *///? } else {
            player.displayClientMessage(Component.translatable("gui.minions_workshop." + error), true);
            //? }
            return false;
        }
        if (carried.getCount() == 1) {
            setCarried(result);
        } else {
            if (!moveItemStackTo(result, playerStart, playerEnd, true)) {
                throw new IllegalStateException("Workshop bucket exchange lost its reserved inventory space");
            }
            carried.shrink(1);
        }
        if (filling) {
            table.power().insertLava(WorkshopPower.FLUID_PER_BUCKET, false);
        } else {
            table.power().extractLava(WorkshopPower.FLUID_PER_BUCKET, false);
        }
        return true;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !isModuleSlot(slot.index) && (slot.container == inventory || slot.index >= playerStart && slot.index < playerEnd);
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (isModuleSlot(slot) && type == ClickType.PICKUP && (button == 0 || button == 1)) {
            int unit = (slot - WorkshopLayout.UPGRADES) / WorkshopLayout.UPGRADE_SIZE;
            ItemStack installed = inventory.getItem(slot);
            ItemStack carried = getCarried();
            selectedUnit = unit;
            updateGroup();
            if (button == 0 && installed.isEmpty() && Module.of(carried) != Module.EMPTY) {
                inventory.setItem(slot, carried.split(1));
                if (carried.isEmpty()) {
                    setCarried(ItemStack.EMPTY);
                }
                page = 0;
                group = unit;
            } else if (button == 1 && !installed.isEmpty()) {
                if (carried.isEmpty()) {
                    setCarried(inventory.removeItemNoUpdate(slot));
                } else if (ItemStack.isSameItemSameComponents(installed, carried)
                    && carried.getCount() + installed.getCount() <= carried.getMaxStackSize()) {
                    carried.grow(installed.getCount());
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
            }
            inventory.setChanged();
            broadcastChanges();
            return;
        }
        super.clicked(slot, button, type, player);
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
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
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
        for (int index = playerStart; index < playerEnd; index++) {
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
            if (isModuleSlot(tableSlot)) {
                return true;
            }
            if (tableSlot >= WorkshopLayout.UPGRADES) {
                return page == 2 && (table != null || sidePanelReady) && (tableSlot >= WorkshopLayout.GLOBAL
                    || (tableSlot - WorkshopLayout.UPGRADES) / WorkshopLayout.UPGRADE_SIZE == selectedUnit());
            }
            int unit = (tableSlot - 1) / WorkshopLayout.UNIT_SIZE;
            return unit == selectedUnit() && activeUnitSlot(tableSlot);
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
            return unit == selectedUnit() && module(unit) == Module.CRAFTING;
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
