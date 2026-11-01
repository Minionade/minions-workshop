package dev.minionade.workshop;

public final class WorkshopPower {
    public static final int ENERGY_PER_FUEL = 8;
    public static final int FLUID_PER_MILLIBUCKET = 81;
    public static final int FLUID_PER_BUCKET = 81_000;
    public static final int LAVA_CAPACITY = 8 * FLUID_PER_BUCKET;
    public static final int LAVA_FUEL_PER_BUCKET = 20_000;
    // Exact common denominator for 1/8 fuel per energy and 20/81 fuel per fluid unit.
    public static final int UNITS_PER_FUEL = 648;
    public static final int POWER_CAPACITY = WorkshopRules.MAX_FUEL * UNITS_PER_FUEL;
    private static final int UNITS_PER_ENERGY = UNITS_PER_FUEL / ENERGY_PER_FUEL;
    private static final int UNITS_PER_FLUID = LAVA_FUEL_PER_BUCKET * UNITS_PER_FUEL / FLUID_PER_BUCKET;
    private int amount;
    private int lava;

    public record State(int amount, int lava) {
        public State {
            if (amount < 0 || amount > POWER_CAPACITY || lava < 0 || lava > LAVA_CAPACITY) {
                throw new IllegalArgumentException("Invalid workshop power state");
            }
        }
    }

    public State snapshot() {
        return new State(amount, lava);
    }

    public void restore(State state) {
        amount = state.amount();
        lava = state.lava();
    }

    public int fuel() {
        return amount / UNITS_PER_FUEL;
    }

    public int energy() {
        return amount / UNITS_PER_ENERGY;
    }

    public int lava() {
        return lava;
    }

    public boolean addFuel(int fuel) {
        if (fuel <= 0) {
            throw new IllegalArgumentException("Fuel must be positive");
        }
        long units = (long) fuel * UNITS_PER_FUEL;
        if (units > POWER_CAPACITY - amount) {
            return false;
        }
        amount += (int) units;
        return true;
    }

    public boolean consumeFuel(int fuel) {
        if (fuel <= 0) {
            throw new IllegalArgumentException("Fuel must be positive");
        }
        long units = (long) fuel * UNITS_PER_FUEL;
        if (units > amount) {
            return false;
        }
        amount -= (int) units;
        return true;
    }

    public long insertEnergy(long maximum, boolean simulate) {
        return insertEnergy(maximum, 1, simulate);
    }

    public long insertEnergy(long maximum, int energyPerUnit, boolean simulate) {
        requireNonnegative(maximum);
        if (energyPerUnit <= 0) {
            throw new IllegalArgumentException("Energy conversion ratio must be positive");
        }
        long units = (long) UNITS_PER_ENERGY * energyPerUnit;
        int inserted = (int) Math.min(maximum, (POWER_CAPACITY - amount) / units);
        if (!simulate) {
            amount += (int) (inserted * units);
        }
        return inserted;
    }

    public long insertLava(long maximum, boolean simulate) {
        requireNonnegative(maximum);
        int inserted = (int) Math.min(maximum, LAVA_CAPACITY - lava);
        if (!simulate) {
            lava += inserted;
        }
        return inserted;
    }

    public long extractLava(long maximum, boolean simulate) {
        requireNonnegative(maximum);
        int extracted = (int) Math.min(maximum, lava);
        if (!simulate) {
            lava -= extracted;
        }
        return extracted;
    }

    public boolean burnLava() {
        int consumed = Math.min(lava, (POWER_CAPACITY - amount) / UNITS_PER_FLUID);
        lava -= consumed;
        amount += consumed * UNITS_PER_FLUID;
        return consumed > 0;
    }

    private static void requireNonnegative(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Resource amount must not be negative");
        }
    }
}
