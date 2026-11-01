//? if fabric {
package dev.minionade.workshop.fabric;

import dev.minionade.workshop.WorkshopBlockEntity;
import dev.minionade.workshop.WorkshopPower;
import dev.minionade.workshop.WorkshopRules;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import team.reborn.energy.api.EnergyStorage;

public final class WorkshopFabricResources extends SnapshotParticipant<WorkshopPower.State> {
    private final WorkshopBlockEntity table;
    private final Map<Direction, EnergyStorage> energy = new EnumMap<>(Direction.class);
    private final Map<Direction, Storage<FluidVariant>> fluid = new EnumMap<>(Direction.class);

    public WorkshopFabricResources(WorkshopBlockEntity table) {
        this.table = table;
        for (Direction side : Direction.values()) {
            energy.put(side, new Energy(side));
            fluid.put(side, new Lava(side));
        }
    }

    public EnergyStorage energy(Direction side) {
        return side == null ? EnergyStorage.EMPTY : energy.get(side);
    }

    public Storage<FluidVariant> fluid(Direction side) {
        return side == null ? Storage.empty() : fluid.get(side);
    }

    @Override
    protected WorkshopPower.State createSnapshot() {
        return table.power().snapshot();
    }

    @Override
    protected void readSnapshot(WorkshopPower.State state) {
        table.power().restore(state);
    }

    @Override
    protected void onFinalCommit() {
        table.setChanged();
    }

    private final class Energy implements EnergyStorage {
        private final Direction side;

        private Energy(Direction side) {
            this.side = side;
        }

        @Override
        public boolean supportsInsertion() {
            return table.canReceiveEnergy(side);
        }

        @Override
        public long insert(long maximum, TransactionContext transaction) {
            StoragePreconditions.notNegative(maximum);
            if (!supportsInsertion()) {
                return 0;
            }
            long accepted = table.power().insertEnergy(maximum, true);
            if (accepted > 0) {
                updateSnapshots(transaction);
                table.power().insertEnergy(accepted, false);
            }
            return accepted;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        public long extract(long maximum, TransactionContext transaction) {
            StoragePreconditions.notNegative(maximum);
            return 0;
        }

        @Override
        public long getAmount() {
            return table.power().energy();
        }

        @Override
        public long getCapacity() {
            return (long) WorkshopRules.MAX_FUEL * WorkshopPower.ENERGY_PER_FUEL;
        }
    }

    private final class Lava implements Storage<FluidVariant>, StorageView<FluidVariant> {
        private final Direction side;

        private Lava(Direction side) {
            this.side = side;
        }

        @Override
        public boolean supportsInsertion() {
            return table.canTransferLava(side, 0);
        }

        @Override
        public long insert(FluidVariant resource, long maximum, TransactionContext transaction) {
            StoragePreconditions.notNegative(maximum);
            if (!supportsInsertion() || !resource.equals(FluidVariant.of(Fluids.LAVA))) {
                return 0;
            }
            long accepted = table.power().insertLava(maximum, true);
            if (accepted > 0) {
                updateSnapshots(transaction);
                table.power().insertLava(accepted, false);
            }
            return accepted;
        }

        @Override
        public boolean supportsExtraction() {
            return table.canTransferLava(side, 1);
        }

        @Override
        public long extract(FluidVariant resource, long maximum, TransactionContext transaction) {
            StoragePreconditions.notNegative(maximum);
            if (!supportsExtraction() || !resource.equals(FluidVariant.of(Fluids.LAVA))) {
                return 0;
            }
            long extracted = table.power().extractLava(maximum, true);
            if (extracted > 0) {
                updateSnapshots(transaction);
                table.power().extractLava(extracted, false);
            }
            return extracted;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            return List.<StorageView<FluidVariant>>of(this).iterator();
        }

        @Override
        public boolean isResourceBlank() {
            return table.power().lava() == 0;
        }

        @Override
        public FluidVariant getResource() {
            return isResourceBlank() ? FluidVariant.blank() : FluidVariant.of(Fluids.LAVA);
        }

        @Override
        public long getAmount() {
            return table.power().lava();
        }

        @Override
        public long getCapacity() {
            return WorkshopPower.LAVA_CAPACITY;
        }
    }
}
//? }
