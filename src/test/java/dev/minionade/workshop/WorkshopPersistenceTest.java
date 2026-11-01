//? if neoforge {
/*package dev.minionade.workshop;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
//? if >=26.1 {
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
//? } else {
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
//? }

class WorkshopPersistenceTest {
    @BeforeAll
    static void loadModAndDatapacks() {
        CraftingAndTransferTest.bootstrapMinecraft();
        assertNotNull(Workshop.TABLE);
        assertNotNull(Workshop.TABLE_ENTITY);
        assertNotNull(Workshop.TABLE_MENU);
        assertEquals(Upgrade.values().length, Workshop.UPGRADES.size());
    }

    private static WorkshopBlockEntity table() {
        return new WorkshopBlockEntity(BlockPos.ZERO, Workshop.TABLE.defaultBlockState());
    }

    private static WorkshopBlockEntity load(CompoundTag tag) {
        return assertInstanceOf(WorkshopBlockEntity.class,
            BlockEntity.loadStatic(BlockPos.ZERO, Workshop.TABLE.defaultBlockState(), tag, CraftingAndTransferTest.registries));
    }

    @Test
    void everyRealSlotAndEveryIndependentGhostRuleSurvivesSaving() {
        WorkshopBlockEntity original = table();
        original.setItem(WorkshopLayout.FUEL, new ItemStack(Items.COAL, 10));
        for (int slot = 1; slot < WorkshopLayout.UPGRADES; slot++) {
            original.setItem(slot, new ItemStack(Items.DIAMOND, slot % 64 + 1));
        }
        for (int unit = 0; unit < 4; unit++) {
            original.setItem(WorkshopLayout.parent(unit), new ItemStack(
                unit == 1 ? Items.FURNACE : unit == 2 ? Items.CHEST : Items.CRAFTING_TABLE));
            for (int upgrade = 1; upgrade < WorkshopLayout.UPGRADE_SIZE; upgrade++) {
                original.setItem(WorkshopLayout.parent(unit) + upgrade, new ItemStack(Workshop.UPGRADES.get(Upgrade.CHARGED)));
            }
        }
        for (int slot = 0; slot < 8; slot++) {
            Upgrade upgrade = slot < 6 ? Upgrade.TRANSFER : slot == 6 ? Upgrade.FILTER : Upgrade.AUTO_TRANSFER;
            original.setItem(WorkshopLayout.GLOBAL + slot, new ItemStack(Workshop.UPGRADES.get(upgrade)));
        }
        for (int group = 0; group < 5; group++) {
            for (int side = 0; side < 6; side++) {
                for (int direction = 0; direction < 2; direction++) {
                    int index = WorkshopLayout.rule(group, side, direction);
                    TransferRule rule = original.rule(group, side, direction);
                    rule.toggle(TransferRule.ENABLED);
                    if (direction == 1) {
                        rule.toggle(TransferRule.AUTO);
                    }
                    if (side % 2 == 0) {
                        rule.toggle(TransferRule.WHITELIST);
                    }
                    ItemStack sample = new ItemStack(Items.DIAMOND, 32);
                    sample.set(DataComponents.CUSTOM_NAME, Component.literal("Rule " + index));
                    rule.setFilter(0, sample);
                    for (int mode = 0; mode < index % 4; mode++) {
                        rule.cycleMode(0);
                    }
                    assertEquals(32, sample.getCount());
                }
            }
        }
        WorkshopBlockEntity restored = load(original.saveWithFullMetadata(CraftingAndTransferTest.registries));
        assertEquals(221, restored.getContainerSize());
        for (int slot = 0; slot < WorkshopLayout.SIZE; slot++) {
            assertTrue(ItemStack.matches(original.getItem(slot), restored.getItem(slot)), "Real slot " + slot);
        }
        for (int group = 0; group < 5; group++) {
            for (int side = 0; side < 6; side++) {
                for (int direction = 0; direction < 2; direction++) {
                    TransferRule expected = original.rule(group, side, direction);
                    TransferRule actual = restored.rule(group, side, direction);
                    assertEquals(expected.flags(), actual.flags());
                    for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
                        assertTrue(ItemStack.matches(expected.filter(filter).item(), actual.filter(filter).item()));
                        assertEquals(expected.filter(filter).mode(), actual.filter(filter).mode());
                    }
                    assertEquals(1, actual.filter(0).item().getCount());
                }
            }
        }
    }

    @Test
    void malformedSavedCountersAreBoundedAndNonfiniteExperienceIsRejected() {
        CompoundTag tag = table().saveWithFullMetadata(CraftingAndTransferTest.registries);
        tag.putInt("Fuel", WorkshopRules.MAX_FUEL + 1);
        tag.putIntArray("Progress", new int[] {-100, 400, 999, 30, 20, Integer.MAX_VALUE, 3, 7});
        tag.putIntArray("Charges", new int[] {-1, 99, 32, 4, 6, 30, 31, 0});
        tag.putFloat("Experience0", Float.NaN);
        tag.putFloat("Experience1", Float.POSITIVE_INFINITY);
        tag.putFloat("Experience2", Float.NEGATIVE_INFINITY);
        tag.putFloat("Experience3", -1);
        WorkshopBlockEntity restored = load(tag);
        assertEquals(WorkshopRules.MAX_FUEL, restored.fuel());
        CompoundTag saved = restored.saveWithFullMetadata(CraftingAndTransferTest.registries);
        //? if >=26.1 {
        assertArrayEquals(new int[] {0, 399, 399, 30, 20, 399, 3, 7}, saved.getIntArray("Progress").orElseThrow());
        assertArrayEquals(new int[] {0, 32, 32, 4, 6, 30, 31, 0}, saved.getIntArray("Charges").orElseThrow());
        //? } else {
        assertArrayEquals(new int[] {0, 399, 399, 30, 20, 399, 3, 7}, saved.getIntArray("Progress"));
        assertArrayEquals(new int[] {0, 32, 32, 4, 6, 30, 31, 0}, saved.getIntArray("Charges"));
        //? }
        for (int unit = 0; unit < 4; unit++) {
            //? if >=26.1 {
            assertEquals(0F, saved.getFloat("Experience" + unit).orElseThrow());
            //? } else {
            assertEquals(0F, saved.getFloat("Experience" + unit));
            //? }
        }
    }

    @Test
    void savingDoesNotExposeLiveMachineCounters() {
        WorkshopBlockEntity table = table();
        table.setItem(WorkshopLayout.parent(0), new ItemStack(Items.CRAFTING_TABLE));
        table.setItem(WorkshopLayout.parent(0) + 1, new ItemStack(Workshop.UPGRADES.get(Upgrade.CHARGED)));
        CompoundTag tag = table.saveWithFullMetadata(CraftingAndTransferTest.registries);
        tag.putIntArray("Progress", new int[] {17, 0, 0, 0, 0, 0, 0, 0});
        tag.putIntArray("Charges", new int[] {1, 0, 0, 0, 0, 0, 0, 0});
        WorkshopBlockEntity restored = load(tag);
        CompoundTag snapshot = restored.saveWithFullMetadata(CraftingAndTransferTest.registries);
        //? if >=26.1 {
        snapshot.getIntArray("Progress").orElseThrow()[0] = 0;
        snapshot.getIntArray("Charges").orElseThrow()[0] = 0;
        //? } else {
        snapshot.getIntArray("Progress")[0] = 0;
        snapshot.getIntArray("Charges")[0] = 0;
        //? }
        assertEquals(17, restored.progress(0));
        assertEquals(1, restored.charges(0));
    }

    @Test
    void nativeInventoryAdaptersObservePermissionAndModuleChanges() {
        WorkshopBlockEntity table = table();
        int storage = WorkshopLayout.slot(2, WorkshopLayout.STORAGE);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        //? if >=26.1 {
        var handler = new WorldlyContainerWrapper(table, Direction.NORTH);
        assertEquals(WorkshopLayout.UPGRADES, handler.size());
        //? } else {
        var handler = new SidedInvWrapper(table, Direction.NORTH);
        assertEquals(WorkshopLayout.UPGRADES, handler.getSlots());
        //? }
        assertEquals(0, insert(handler, storage, diamond, 3, false));
        table.setItem(WorkshopLayout.parent(2), new ItemStack(Items.CHEST));
        table.rule(2, Direction.NORTH.ordinal(), 0).toggle(TransferRule.ENABLED);
        assertEquals(3, insert(handler, storage, diamond, 3, true));
        assertTrue(table.getItem(storage).isEmpty());
        assertEquals(3, insert(handler, storage, diamond, 3, false));
        table.setItem(WorkshopLayout.GLOBAL, new ItemStack(Workshop.UPGRADES.get(Upgrade.FILTER)));
        TransferRule input = table.rule(2, Direction.NORTH.ordinal(), 0);
        input.toggle(TransferRule.WHITELIST);
        input.setFilter(0, diamond);
        assertEquals(0, insert(handler, storage, new ItemStack(Items.EMERALD), 3, false));
        assertEquals(61, insert(handler, storage, diamond, 64, false));
        assertEquals(64, table.getItem(storage).getCount());
        assertEquals(0, extract(handler, storage, diamond, 5, false));
        TransferRule output = table.rule(2, Direction.NORTH.ordinal(), 1);
        output.toggle(TransferRule.ENABLED);
        assertEquals(5, extract(handler, storage, diamond, 5, true));
        assertEquals(64, table.getItem(storage).getCount());
        assertEquals(5, extract(handler, storage, diamond, 5, false));
        assertEquals(59, table.getItem(storage).getCount());
        table.setItem(WorkshopLayout.parent(2), new ItemStack(Items.CRAFTING_TABLE));
        assertEquals(0, extract(handler, storage, diamond, 5, false));
        int pattern = WorkshopLayout.slot(2, WorkshopLayout.GRID);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        table.setItem(pattern, planks.copy());
        assertEquals(0, extract(handler, pattern, planks, 1, false));
        assertEquals(1, table.getItem(pattern).getCount());
    }

    private static int insert(
        //? if >=26.1 {
        WorldlyContainerWrapper handler,
        //? } else {
        SidedInvWrapper handler,
        //? }
        int slot, ItemStack stack, int amount, boolean simulate
    ) {
        //? if >=26.1 {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(slot, ItemResource.of(stack), amount, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return inserted;
        }
        //? } else {
        return amount - handler.insertItem(slot, stack.copyWithCount(amount), simulate).getCount();
        //? }
    }

    private static int extract(
        //? if >=26.1 {
        WorldlyContainerWrapper handler,
        //? } else {
        SidedInvWrapper handler,
        //? }
        int slot, ItemStack stack, int amount, boolean simulate
    ) {
        //? if >=26.1 {
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(slot, ItemResource.of(stack), amount, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return extracted;
        }
        //? } else {
        return handler.extractItem(slot, amount, simulate).getCount();
        //? }
    }
}
*///? }
