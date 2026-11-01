//? if neoforge {
/*package dev.minionade.workshop.neoforge;

import dev.minionade.workshop.WorkshopBlockEntity;
import dev.minionade.workshop.WorkshopPower;
import dev.minionade.workshop.WorkshopRules;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
//? if >=26.1 {
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
//? } else {
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
//? }

public final class WorkshopNeoForgeResources
    //? if >=26.1 {
    extends SnapshotJournal<WorkshopPower.State>
    //? }
{
    private final WorkshopBlockEntity table;
    //? if >=26.1 {
    private final Map<Direction, EnergyHandler> energy = new EnumMap<>(Direction.class);
    private final Map<Direction, ResourceHandler<FluidResource>> fluid = new EnumMap<>(Direction.class);
    //? } else {
    private final Map<Direction, IEnergyStorage> energy = new EnumMap<>(Direction.class);
    private final Map<Direction, IFluidHandler> fluid = new EnumMap<>(Direction.class);
    //? }

    public WorkshopNeoForgeResources(WorkshopBlockEntity table) {
        this.table = table;
        for (Direction side : Direction.values()) {
            energy.put(side, new Energy(side));
            fluid.put(side, new Lava(side));
        }
    }

    //? if >=26.1 {
    public EnergyHandler energy(Direction side) {
        return energy.get(side);
    }

    public ResourceHandler<FluidResource> fluid(Direction side) {
        return fluid.get(side);
    }

    @Override
    protected WorkshopPower.State createSnapshot() {
        return table.power().snapshot();
    }

    @Override
    protected void revertToSnapshot(WorkshopPower.State state) {
        table.power().restore(state);
    }

    @Override
    protected void onRootCommit(WorkshopPower.State original) {
        table.setChanged();
    }

    public int insertEnergyUnits(Direction side, int maximum, int energyPerUnit, TransactionContext transaction) {
        checkAmount(maximum);
        if (!table.canReceiveEnergy(side)) {
            return 0;
        }
        int accepted = (int) table.power().insertEnergy(maximum, energyPerUnit, true);
        if (accepted > 0) {
            updateSnapshots(transaction);
            table.power().insertEnergy(accepted, energyPerUnit, false);
        }
        return accepted;
    }
    //? } else {
    public IEnergyStorage energy(Direction side) {
        return energy.get(side);
    }

    public IFluidHandler fluid(Direction side) {
        return fluid.get(side);
    }
    //? }

    private static void checkAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Resource amount must not be negative");
        }
    }

    private static void checkTank(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("Invalid workshop tank: " + index);
        }
    }

    private final class Energy
        //? if >=26.1 {
        implements EnergyHandler
        //? } else {
        implements IEnergyStorage
        //? }
    {
        private final Direction side;

        private Energy(Direction side) {
            this.side = side;
        }

        //? if >=26.1 {
        @Override
        public long getAmountAsLong() {
            return table.power().energy();
        }

        @Override
        public long getCapacityAsLong() {
            return (long) WorkshopRules.MAX_FUEL * WorkshopPower.ENERGY_PER_FUEL;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            return insertEnergyUnits(side, amount, 1, transaction);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            checkAmount(amount);
            return 0;
        }
        //? } else {
        @Override
        public int receiveEnergy(int maximum, boolean simulate) {
            checkAmount(maximum);
            if (!canReceive()) {
                return 0;
            }
            int accepted = (int) table.power().insertEnergy(maximum, simulate);
            if (!simulate && accepted > 0) {
                table.setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maximum, boolean simulate) {
            checkAmount(maximum);
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return table.power().energy();
        }

        @Override
        public int getMaxEnergyStored() {
            return WorkshopRules.MAX_FUEL * WorkshopPower.ENERGY_PER_FUEL;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return table.canReceiveEnergy(side);
        }
        //? }
    }

    private final class Lava
        //? if >=26.1 {
        implements ResourceHandler<FluidResource>
        //? } else {
        implements IFluidHandler
        //? }
    {
        private final Direction side;

        private Lava(Direction side) {
            this.side = side;
        }

        //? if >=26.1 {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            checkTank(index);
            return table.power().lava() == 0 ? FluidResource.EMPTY : FluidResource.of(Fluids.LAVA);
        }

        @Override
        public long getAmountAsLong(int index) {
            checkTank(index);
            return table.power().lava() / WorkshopPower.FLUID_PER_MILLIBUCKET;
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            checkTank(index);
            return WorkshopPower.LAVA_CAPACITY / WorkshopPower.FLUID_PER_MILLIBUCKET;
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            checkTank(index);
            return resource.equals(FluidResource.of(Fluids.LAVA));
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            checkAmount(amount);
            if (!isValid(index, resource) || !table.canTransferLava(side, 0)) {
                return 0;
            }
            int accepted = (int) (table.power().insertLava((long) amount * WorkshopPower.FLUID_PER_MILLIBUCKET, true)
                / WorkshopPower.FLUID_PER_MILLIBUCKET);
            if (accepted > 0) {
                updateSnapshots(transaction);
                table.power().insertLava((long) accepted * WorkshopPower.FLUID_PER_MILLIBUCKET, false);
            }
            return accepted;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            checkAmount(amount);
            if (!isValid(index, resource) || !table.canTransferLava(side, 1)) {
                return 0;
            }
            int extracted = (int) (table.power().extractLava((long) amount * WorkshopPower.FLUID_PER_MILLIBUCKET, true)
                / WorkshopPower.FLUID_PER_MILLIBUCKET);
            if (extracted > 0) {
                updateSnapshots(transaction);
                table.power().extractLava((long) extracted * WorkshopPower.FLUID_PER_MILLIBUCKET, false);
            }
            return extracted;
        }
        //? } else {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            checkTank(index);
            int amount = table.power().lava() / WorkshopPower.FLUID_PER_MILLIBUCKET;
            return amount == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.LAVA, amount);
        }

        @Override
        public int getTankCapacity(int index) {
            checkTank(index);
            return WorkshopPower.LAVA_CAPACITY / WorkshopPower.FLUID_PER_MILLIBUCKET;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack resource) {
            checkTank(index);
            return resource.getFluid() == Fluids.LAVA && resource.getComponentsPatch().isEmpty();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource) || !table.canTransferLava(side, 0)) {
                return 0;
            }
            int accepted = (int) (table.power().insertLava((long) resource.getAmount() * WorkshopPower.FLUID_PER_MILLIBUCKET, true)
                / WorkshopPower.FLUID_PER_MILLIBUCKET);
            if (action.execute() && accepted > 0) {
                table.power().insertLava((long) accepted * WorkshopPower.FLUID_PER_MILLIBUCKET, false);
                table.setChanged();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return isFluidValid(0, resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            checkAmount(maximum);
            if (!table.canTransferLava(side, 1)) {
                return FluidStack.EMPTY;
            }
            int extracted = (int) (table.power().extractLava((long) maximum * WorkshopPower.FLUID_PER_MILLIBUCKET, true)
                / WorkshopPower.FLUID_PER_MILLIBUCKET);
            if (action.execute() && extracted > 0) {
                table.power().extractLava((long) extracted * WorkshopPower.FLUID_PER_MILLIBUCKET, false);
                table.setChanged();
            }
            return extracted == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.LAVA, extracted);
        }
        //? }
    }
}
*///? }
