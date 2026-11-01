//? if neoforge {
/*package dev.minionade.workshop;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
//? if >=26.1 {
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
//? } else {
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
//? }

class WorkshopResourceTest {
    @BeforeAll
    static void bootstrap() {
        CraftingAndTransferTest.bootstrapMinecraft();
        assertNotNull(Workshop.TABLE_ENTITY);
    }

    static WorkshopBlockEntity table() {
        return new WorkshopBlockEntity(BlockPos.ZERO, Workshop.TABLE.defaultBlockState());
    }

    static void enable(WorkshopBlockEntity table) {
        table.setItem(WorkshopLayout.GLOBAL, new ItemStack(Workshop.UPGRADES.get(Upgrade.ENERGY)));
        table.setItem(WorkshopLayout.GLOBAL + 1, new ItemStack(Workshop.UPGRADES.get(Upgrade.FLUID)));
        table.rule(4, Direction.NORTH.ordinal(), 0).toggle(TransferRule.ENABLED);
        table.rule(4, Direction.NORTH.ordinal(), 1).toggle(TransferRule.ENABLED);
    }

    @Test
    void energyAndFluidRequireTheirOwnUpgradesAndSharedFuelFacePermissions() {
        WorkshopBlockEntity table = table();
        assertEquals(0, energy(table, 10, false));
        assertEquals(0, fill(table, 1000, false));
        enable(table);
        assertEquals(10, energy(table, 10, false));
        assertEquals(1000, fill(table, 1000, false));
        assertFalse(table.canReceiveEnergy(Direction.SOUTH));
        assertFalse(table.canTransferLava(Direction.SOUTH, 0));
        assertFalse(table.canReceiveEnergy(null));
        assertFalse(table.canTransferLava(null, 0));
        table.rule(4, Direction.NORTH.ordinal(), 0).toggle(TransferRule.ENABLED);
        assertEquals(0, energy(table, 10, false));
        assertEquals(0, fill(table, 1000, false));
        assertEquals(1000, drain(table, 1000, false));
    }

    @Test
    void nativeSimulationAndCapacityChecksConserveBothResources() {
        WorkshopBlockEntity table = table();
        enable(table);
        WorkshopPower.State before = table.power().snapshot();
        assertEquals(64_000, energy(table, Integer.MAX_VALUE, true));
        assertEquals(8000, fill(table, Integer.MAX_VALUE, true));
        assertEquals(before, table.power().snapshot());
        assertEquals(64_000, energy(table, Integer.MAX_VALUE, false));
        assertEquals(8000, fill(table, Integer.MAX_VALUE, false));
        assertEquals(0, energy(table, 1, false));
        assertEquals(0, fill(table, 1, false));
        assertEquals(8000, drain(table, Integer.MAX_VALUE, true));
        assertEquals(WorkshopPower.LAVA_CAPACITY, table.power().lava());
        assertEquals(8000, drain(table, Integer.MAX_VALUE, false));
        assertEquals(0, table.power().lava());
    }

    @Test
    void cachedHandlersObserveUpgradeRemovalWithoutDestroyingStoredPowerOrLava() {
        WorkshopBlockEntity table = table();
        var energy = table.resourceHandlers.energy(Direction.NORTH);
        var fluid = table.resourceHandlers.fluid(Direction.NORTH);
        enable(table);
        energy(table, 7, false);
        fill(table, 1234, false);
        WorkshopPower.State before = table.power().snapshot();
        table.removeItem(WorkshopLayout.GLOBAL, 1);
        table.removeItemNoUpdate(WorkshopLayout.GLOBAL + 1);
        assertEquals(0, energy(table, 100, false));
        assertEquals(0, fill(table, 100, false));
        assertEquals(0, drain(table, 100, false));
        assertEquals(before, table.power().snapshot());
        assertSame(energy, table.resourceHandlers.energy(Direction.NORTH));
        assertSame(fluid, table.resourceHandlers.fluid(Direction.NORTH));
        table.setItem(WorkshopLayout.GLOBAL, new ItemStack(Workshop.UPGRADES.get(Upgrade.ENERGY)));
        table.setItem(WorkshopLayout.GLOBAL + 1, new ItemStack(Workshop.UPGRADES.get(Upgrade.FLUID)));
        assertEquals(1, energy(table, 1, false));
        assertEquals(1, drain(table, 1, false));
        assertEquals(1, table.fuel());
        assertEquals(1233 * WorkshopPower.FLUID_PER_MILLIBUCKET, table.power().lava());
    }

    @Test
    void theFuelTankRejectsWaterAndItemFiltersDoNotBlockResourcePorts() {
        WorkshopBlockEntity table = table();
        enable(table);
        table.setItem(WorkshopLayout.GLOBAL + 2, new ItemStack(Workshop.UPGRADES.get(Upgrade.FILTER)));
        table.rule(4, Direction.NORTH.ordinal(), 0).toggle(TransferRule.WHITELIST);
        //? if >=26.1 {
        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(0, table.resourceHandlers.fluid(Direction.NORTH).insert(0, FluidResource.of(Fluids.WATER), 1000, transaction));
            transaction.commit();
        }
        //? } else {
        assertEquals(0, table.resourceHandlers.fluid(Direction.NORTH).fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
        //? }
        assertEquals(1, energy(table, 1, false));
        assertEquals(1000, fill(table, 1000, false));
        assertEquals(WorkshopPower.FLUID_PER_BUCKET, table.power().lava());
    }

    @Test
    void exactResourceStateAndPermissionsSurviveNativeWorldSaving() {
        WorkshopBlockEntity table = table();
        enable(table);
        energy(table, 5, false);
        fill(table, 2345, false);
        table.power().burnLava();
        CompoundTag tag = table.saveWithFullMetadata(CraftingAndTransferTest.registries);
        WorkshopBlockEntity restored = assertInstanceOf(WorkshopBlockEntity.class,
            BlockEntity.loadStatic(BlockPos.ZERO, Workshop.TABLE.defaultBlockState(), tag, CraftingAndTransferTest.registries));
        assertEquals(table.power().snapshot(), restored.power().snapshot());
        assertTrue(restored.canReceiveEnergy(Direction.NORTH));
        assertTrue(restored.canTransferLava(Direction.NORTH, 1));
        assertEquals(table.power().energy(), restored.power().energy());
        assertEquals(table.power().lava(), restored.power().lava());
    }

    @Test
    void malformedNewResourceCountersAreBounded() {
        WorkshopBlockEntity table = table();
        CompoundTag tag = table.saveWithFullMetadata(CraftingAndTransferTest.registries);
        tag.putInt("PowerUnits", Integer.MAX_VALUE);
        tag.putInt("Lava", -1);
        WorkshopBlockEntity restored = assertInstanceOf(WorkshopBlockEntity.class,
            BlockEntity.loadStatic(BlockPos.ZERO, Workshop.TABLE.defaultBlockState(), tag, CraftingAndTransferTest.registries));
        assertEquals(8000, restored.fuel());
        assertEquals(0, restored.power().lava());
    }

    //? if >=26.1 {
    @Test
    void aSharedJournalRestoresEnergyAndFluidAcrossNestedTransactions() {
        WorkshopBlockEntity table = table();
        enable(table);
        var energy = table.resourceHandlers.energy(Direction.NORTH);
        var fluid = table.resourceHandlers.fluid(Direction.NORTH);
        WorkshopPower.State before = table.power().snapshot();
        try (Transaction outer = Transaction.openRoot()) {
            assertEquals(7, energy.insert(7, outer));
            try (Transaction inner = Transaction.open(outer)) {
                assertEquals(1000, fluid.insert(0, FluidResource.of(Fluids.LAVA), 1000, inner));
                assertEquals(1, energy.insert(1, inner));
                inner.commit();
            }
        }
        assertEquals(before, table.power().snapshot());
        try (Transaction outer = Transaction.openRoot()) {
            assertEquals(7, energy.insert(7, outer));
            try (Transaction inner = Transaction.open(outer)) {
                assertEquals(1000, fluid.insert(0, FluidResource.of(Fluids.LAVA), 1000, inner));
                assertEquals(1, energy.insert(1, inner));
            }
            outer.commit();
        }
        assertEquals(7, table.power().energy());
        assertEquals(0, table.power().lava());
    }
    //? }

    private static int energy(WorkshopBlockEntity table, int maximum, boolean simulate) {
        //? if >=26.1 {
        try (Transaction transaction = Transaction.openRoot()) {
            int accepted = table.resourceHandlers.energy(Direction.NORTH).insert(maximum, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return accepted;
        }
        //? } else {
        return table.resourceHandlers.energy(Direction.NORTH).receiveEnergy(maximum, simulate);
        //? }
    }

    private static int fill(WorkshopBlockEntity table, int amount, boolean simulate) {
        //? if >=26.1 {
        try (Transaction transaction = Transaction.openRoot()) {
            int accepted = table.resourceHandlers.fluid(Direction.NORTH).insert(0, FluidResource.of(Fluids.LAVA), amount, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return accepted;
        }
        //? } else {
        return table.resourceHandlers.fluid(Direction.NORTH).fill(new FluidStack(Fluids.LAVA, amount), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
        //? }
    }

    private static int drain(WorkshopBlockEntity table, int amount, boolean simulate) {
        //? if >=26.1 {
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = table.resourceHandlers.fluid(Direction.NORTH).extract(0, FluidResource.of(Fluids.LAVA), amount, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return extracted;
        }
        //? } else {
        return table.resourceHandlers.fluid(Direction.NORTH).drain(amount, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE).getAmount();
        //? }
    }
}
*///? }
