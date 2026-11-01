package dev.minionade.workshop;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkshopRulesTest {
    @Test
    void unitSlotsAreDisjointAndDoNotOverlapUpgrades() {
        Set<Integer> slots = new HashSet<>();
        for (int unit = 0; unit < WorkshopLayout.UNIT_COUNT; unit++) {
            for (int offset = 0; offset < WorkshopLayout.UNIT_SIZE; offset++) {
                int slot = WorkshopLayout.slot(unit, offset);
                assertTrue(slot > WorkshopLayout.FUEL && slot < WorkshopLayout.UPGRADES);
                assertTrue(slots.add(slot));
            }
        }
        assertEquals(180, slots.size());
        assertEquals(221, WorkshopLayout.SIZE);
    }

    @Test
    void transferSelectionsCoverExactlySixtyRules() {
        Set<Integer> rules = new HashSet<>();
        for (int group = 0; group < 5; group++) {
            for (int side = 0; side < 6; side++) {
                for (int direction = 0; direction < 2; direction++) {
                    assertTrue(rules.add(WorkshopLayout.rule(group, side, direction)));
                }
            }
        }
        assertEquals(WorkshopLayout.RULE_COUNT, rules.size());
        assertThrows(IllegalArgumentException.class, () -> WorkshopLayout.rule(5, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> WorkshopLayout.slot(4, 0));
    }

    @Test
    void productionRetainsUpstreamRatesAndLimits() {
        assertEquals(4, WorkshopRules.speed(0, false));
        assertEquals(36, WorkshopRules.speed(8, false));
        assertEquals(9, WorkshopRules.speed(8, true));
        assertEquals(1, WorkshopRules.consumption(0, false, false));
        assertEquals(2, WorkshopRules.consumption(0, false, true));
        assertEquals(68, WorkshopRules.consumption(8, true, true));
        assertEquals(0, WorkshopRules.maxCharges(0));
        assertEquals(32, WorkshopRules.maxCharges(8));
        assertEquals(32, WorkshopRules.maxCharges(64));
    }

    @Test
    void transferCapacityIsCappedAtAStackPerSecond() {
        assertEquals(1, WorkshopRules.transferSize(0));
        assertEquals(2, WorkshopRules.transferSize(1));
        assertEquals(64, WorkshopRules.transferSize(6));
        assertEquals(64, WorkshopRules.transferSize(64));
        assertEquals(1, WorkshopRules.transferSize(-1));
    }
}
