package dev.minionade.workshop;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WorkshopPowerTest {
    @Test
    void solidFuelStillRequiresTheWholeItemToFit() {
        WorkshopPower power = new WorkshopPower();
        assertFalse(power.addFuel(20_000));
        assertEquals(0, power.fuel());
        for (int index = 0; index < 5; index++) {
            assertTrue(power.addFuel(1600));
        }
        assertFalse(power.addFuel(1));
        assertEquals(8000, power.fuel());
        assertEquals(64_000, power.energy());
        assertTrue(power.consumeFuel(3));
        assertEquals(7997, power.fuel());
        assertFalse(power.addFuel(4));
        assertTrue(power.addFuel(3));
    }

    @Test
    void singleEnergyUnitsAccumulateWithoutLossAndSurviveSaving() {
        WorkshopPower power = new WorkshopPower();
        for (int index = 1; index <= 8; index++) {
            assertEquals(1, power.insertEnergy(1, false));
            assertEquals(index, power.energy());
            assertEquals(index / 8, power.fuel());
            WorkshopPower restored = new WorkshopPower();
            restored.restore(power.snapshot());
            assertEquals(power.snapshot(), restored.snapshot());
        }
        assertTrue(power.consumeFuel(1));
        assertEquals(0, power.energy());
    }

    @Test
    void energySimulationIsReadOnlyAndIntakeIsBoundedWithoutAnOverflow() {
        WorkshopPower power = new WorkshopPower();
        assertEquals(64_000, power.insertEnergy(Long.MAX_VALUE, true));
        assertEquals(new WorkshopPower.State(0, 0), power.snapshot());
        assertEquals(64_000, power.insertEnergy(Long.MAX_VALUE, false));
        assertEquals(0, power.insertEnergy(1, false));
        assertEquals(0, power.insertEnergy(Long.MAX_VALUE, Integer.MAX_VALUE, false));
        assertEquals(8000, power.fuel());
    }

    @Test
    void industrialEuUsesItsOwnConversionIntoTheSameBuffer() {
        WorkshopPower si = new WorkshopPower();
        assertEquals(1, si.insertEnergy(1, 4, false));
        assertEquals(4, si.energy());
        assertEquals(0, si.fuel());
        assertEquals(1, si.insertEnergy(1, 4, false));
        assertEquals(1, si.fuel());
        assertEquals(15_998, si.insertEnergy(Long.MAX_VALUE, 4, false));
        assertEquals(64_000, si.energy());

        WorkshopPower mi = new WorkshopPower();
        assertEquals(1, mi.insertEnergy(1, 10, false));
        assertEquals(10, mi.energy());
        assertEquals(1, mi.fuel());
        assertTrue(mi.consumeFuel(1));
        assertEquals(2, mi.energy());
        assertEquals(1, mi.insertEnergy(1, 2, false));
        assertEquals(4, mi.energy());
    }

    @Test
    void fractionalPowerCannotLetAWholeSolidFuelItemOverfillTheBuffer() {
        WorkshopPower power = new WorkshopPower();
        assertTrue(power.addFuel(7999));
        assertEquals(1, power.insertEnergy(1, false));
        assertEquals(7999, power.fuel());
        assertFalse(power.addFuel(1));
        assertEquals(7, power.insertEnergy(8, false));
        assertEquals(8000, power.fuel());
    }

    @Test
    void lavaInsertionAndExtractionSimulateWithoutChangingTheTank() {
        WorkshopPower power = new WorkshopPower();
        assertEquals(648_000, power.insertLava(Long.MAX_VALUE, true));
        assertEquals(0, power.lava());
        assertEquals(81_000, power.insertLava(81_000, false));
        assertEquals(81_000, power.extractLava(Long.MAX_VALUE, true));
        assertEquals(81_000, power.lava());
        assertEquals(567_000, power.insertLava(Long.MAX_VALUE, false));
        assertEquals(0, power.insertLava(1, false));
        assertEquals(648_000, power.extractLava(Long.MAX_VALUE, false));
        assertEquals(0, power.lava());
    }

    @Test
    void subMillibucketFluidAndPartialEnergyAreExactlyConserved() {
        WorkshopPower power = new WorkshopPower();
        assertEquals(1, power.insertLava(1, false));
        assertTrue(power.burnLava());
        assertEquals(0, power.fuel());
        assertEquals(160, power.snapshot().amount());
        assertFalse(power.consumeFuel(1));
        assertEquals(1, power.insertEnergy(1, false));
        assertEquals(241, power.snapshot().amount());
        assertEquals(2, power.insertLava(2, false));
        assertTrue(power.burnLava());
        assertEquals(561, power.snapshot().amount());
        assertEquals(0, power.lava());
        assertEquals(2, power.insertEnergy(2, false));
        assertTrue(power.consumeFuel(1));
        assertEquals(75, power.snapshot().amount());
    }

    @Test
    void eachMillibucketOfLavaHasExactlyTwentyFuelUnits() {
        WorkshopPower power = new WorkshopPower();
        assertEquals(81, power.insertLava(81, false));
        assertTrue(power.burnLava());
        assertEquals(20, power.fuel());
        assertEquals(160, power.energy());
        assertEquals(0, power.lava());
        assertTrue(power.consumeFuel(20));
        assertEquals(new WorkshopPower.State(0, 0), power.snapshot());
    }

    @Test
    void anEntireLavaBucketBurnsOverTimeInsteadOfBeingDiscardedAtTheBufferLimit() {
        WorkshopPower power = new WorkshopPower();
        power.insertLava(WorkshopPower.FLUID_PER_BUCKET, false);
        assertTrue(power.burnLava());
        assertEquals(8000, power.fuel());
        assertEquals(48_600, power.lava());
        assertFalse(power.burnLava());
        assertTrue(power.consumeFuel(8000));
        assertTrue(power.burnLava());
        assertEquals(8000, power.fuel());
        assertEquals(16_200, power.lava());
        assertTrue(power.consumeFuel(8000));
        assertTrue(power.burnLava());
        assertEquals(4000, power.fuel());
        assertEquals(0, power.lava());
        assertTrue(power.consumeFuel(4000));
        assertEquals(0, power.snapshot().amount());
    }

    @Test
    void mixedResourcesAndProductionNeverCreateOrDeleteFractionalPower() {
        WorkshopPower power = new WorkshopPower();
        power.insertEnergy(123, false);
        power.insertEnergy(71, 10, false);
        power.insertLava(123_456, false);
        long total = power.snapshot().amount() + (long) power.lava() * 160;
        long spent = 0;
        while (power.lava() > 0 || power.fuel() >= 17) {
            power.burnLava();
            if (power.consumeFuel(17)) {
                spent += 17L * WorkshopPower.UNITS_PER_FUEL;
            }
            assertEquals(total, spent + power.snapshot().amount() + (long) power.lava() * 160);
        }
        assertEquals(0, power.lava());
        assertTrue(power.fuel() < 17);
    }

    @Test
    void oneSnapshotRollsBackBothResourceInputs() {
        WorkshopPower power = new WorkshopPower();
        power.insertEnergy(1, false);
        power.insertLava(17, false);
        WorkshopPower.State before = power.snapshot();
        power.insertEnergy(3000, false);
        power.insertLava(80_000, false);
        power.restore(before);
        assertEquals(before, power.snapshot());
    }

    @Test
    void invalidAmountsRatiosAndSavedStatesFailExplicitly() {
        WorkshopPower power = new WorkshopPower();
        assertThrows(IllegalArgumentException.class, () -> power.insertEnergy(-1, false));
        assertThrows(IllegalArgumentException.class, () -> power.insertEnergy(1, 0, false));
        assertThrows(IllegalArgumentException.class, () -> power.insertLava(-1, false));
        assertThrows(IllegalArgumentException.class, () -> power.extractLava(-1, false));
        assertThrows(IllegalArgumentException.class, () -> power.addFuel(0));
        assertThrows(IllegalArgumentException.class, () -> power.consumeFuel(-1));
        assertThrows(IllegalArgumentException.class, () -> new WorkshopPower.State(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new WorkshopPower.State(0, WorkshopPower.LAVA_CAPACITY + 1));
        assertEquals(new WorkshopPower.State(0, 0), power.snapshot());
    }
}
