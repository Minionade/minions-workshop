//? if neoforge {
/*package dev.minionade.workshop;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
//? if >=26.1 {
import net.minecraft.world.entity.EntityEquipment;
//? }

class WorkshopMenuTest {
    @BeforeAll
    static void loadModAndDatapacks() {
        CraftingAndTransferTest.bootstrapMinecraft();
        assertNotNull(Workshop.TABLE_MENU);
    }

    private static WorkshopMenu menu() {
        //? if >=26.1 {
        return new WorkshopMenu(0, new Inventory(null, new EntityEquipment()));
        //? } else {
        return new WorkshopMenu(0, new Inventory(null));
        //? }
    }

    private static void select(WorkshopMenu menu, int unit, int page) {
        menu.setData(0, page);
        menu.setData(1, page == 3 ? 4 : unit);
        menu.setData(24, unit);
    }

    private static void click(WorkshopMenu menu, int unit, int button) {
        menu.clicked(WorkshopLayout.parent(unit), button, ClickType.PICKUP, null);
    }

    @Test
    void installingSplitsOneBlockAndLeftClickOnlySelectsAnInstalledTab() {
        WorkshopMenu menu = menu();
        select(menu, 0, 2);
        menu.setCarried(new ItemStack(Items.CRAFTING_TABLE, 8));
        click(menu, 2, 0);
        assertEquals(1, menu.moduleItem(2).getCount());
        assertEquals(7, menu.getCarried().getCount());
        assertEquals(2, menu.selectedUnit());
        assertEquals(2, menu.group());
        assertEquals(0, menu.page());

        select(menu, 1, 2);
        menu.setCarried(new ItemStack(Items.FURNACE, 8));
        click(menu, 2, 0);
        assertTrue(menu.moduleItem(2).is(Items.CRAFTING_TABLE));
        assertEquals(8, menu.getCarried().getCount());
        assertEquals(2, menu.selectedUnit());
        assertEquals(2, menu.group());
        assertEquals(2, menu.page());

        menu.setCarried(new ItemStack(Items.DIAMOND, 5));
        click(menu, 3, 0);
        assertTrue(menu.moduleItem(3).isEmpty());
        assertEquals(5, menu.getCarried().getCount());
        assertEquals(3, menu.selectedUnit());
    }

    @Test
    void rightClickReturnsBlocksWithoutSwappingDeletingOrOverfillingTheCursor() {
        WorkshopMenu menu = menu();
        menu.slots.get(WorkshopLayout.parent(0)).set(new ItemStack(Items.CHEST));
        menu.setCarried(new ItemStack(Items.DIAMOND, 12));
        click(menu, 0, 1);
        assertTrue(menu.moduleItem(0).is(Items.CHEST));
        assertEquals(12, menu.getCarried().getCount());

        menu.setCarried(new ItemStack(Items.CHEST, 64));
        click(menu, 0, 1);
        assertEquals(64, menu.getCarried().getCount());
        assertEquals(1, menu.moduleItem(0).getCount());

        menu.setCarried(new ItemStack(Items.CHEST, 63));
        click(menu, 0, 1);
        assertEquals(64, menu.getCarried().getCount());
        assertTrue(menu.moduleItem(0).isEmpty());

        menu.slots.get(WorkshopLayout.parent(1)).set(new ItemStack(Items.FURNACE));
        menu.setCarried(ItemStack.EMPTY);
        click(menu, 1, 1);
        assertTrue(menu.getCarried().is(Items.FURNACE));
        assertEquals(1, menu.getCarried().getCount());
        assertTrue(menu.moduleItem(1).isEmpty());
    }

    @Test
    void removingAStationPreservesItsHiddenContentsAndUpgrades() {
        WorkshopMenu menu = menu();
        int input = WorkshopLayout.slot(0, WorkshopLayout.GRID);
        int stored = WorkshopLayout.slot(0, WorkshopLayout.STORAGE);
        int upgrade = WorkshopLayout.parent(0) + 1;
        menu.slots.get(WorkshopLayout.parent(0)).set(new ItemStack(Items.CRAFTING_TABLE));
        menu.slots.get(input).set(new ItemStack(Items.OAK_PLANKS, 16));
        menu.slots.get(stored).set(new ItemStack(Items.DIAMOND, 32));
        menu.slots.get(upgrade).set(new ItemStack(Workshop.UPGRADES.get(Upgrade.CHARGED), 4));
        click(menu, 0, 1);
        assertTrue(menu.moduleItem(0).isEmpty());
        assertEquals(16, menu.slots.get(input).getItem().getCount());
        assertEquals(32, menu.slots.get(stored).getItem().getCount());
        assertEquals(4, menu.slots.get(upgrade).getItem().getCount());
        assertFalse(menu.slots.get(input).isActive());
        click(menu, 0, 0);
        assertEquals(Module.CRAFTING, menu.module(0));
        assertTrue(menu.slots.get(input).isActive());
        assertEquals(4, menu.upgrades(0, Upgrade.CHARGED));
    }

    @Test
    void onlyTheSelectedStationAndItsCraftingPreviewAreActive() {
        WorkshopMenu menu = menu();
        for (int unit = 0; unit < 4; unit++) {
            menu.slots.get(WorkshopLayout.parent(unit)).set(new ItemStack(
                unit < 2 ? Items.CRAFTING_TABLE : unit == 2 ? Items.FURNACE : Items.CHEST));
        }
        assertEquals(281, menu.slots.size());
        for (int selected = 0; selected < 4; selected++) {
            select(menu, selected, 0);
            for (int unit = 0; unit < 4; unit++) {
                assertTrue(menu.slots.get(WorkshopLayout.parent(unit)).isActive());
                assertEquals(unit == selected && unit < 2, menu.slots.get(WorkshopLayout.SIZE + unit).isActive());
                if (unit != selected) {
                    for (int offset = 0; offset < WorkshopLayout.UNIT_SIZE; offset++) {
                        Slot slot = menu.slots.get(WorkshopLayout.slot(unit, offset));
                        assertFalse(slot.isActive());
                        assertFalse(slot.mayPickup(null));
                        assertFalse(slot.mayPlace(new ItemStack(Items.DIAMOND)));
                    }
                }
            }
        }
    }

    @Test
    void globalsFuelInventoryAndModuleTabsRemainAvailableForEmptyStations() {
        WorkshopMenu menu = menu();
        for (int page = 0; page < 5; page++) {
            select(menu, 2, page);
            for (int unit = 0; unit < 4; unit++) {
                assertTrue(menu.slots.get(WorkshopLayout.parent(unit)).isActive());
                for (int upgrade = 1; upgrade < 8; upgrade++) {
                    assertEquals(page == 2 && unit == 2,
                        menu.slots.get(WorkshopLayout.parent(unit) + upgrade).isActive());
                }
            }
            for (int slot = WorkshopLayout.GLOBAL; slot < WorkshopLayout.SIZE; slot++) {
                assertEquals(page == 2, menu.slots.get(slot).isActive());
            }
            assertTrue(menu.slots.get(WorkshopLayout.FUEL).isActive());
            for (int slot = 235; slot < 271; slot++) {
                assertTrue(menu.slots.get(slot).isActive());
            }
        }
        select(menu, 2, 2);
        assertTrue(menu.slots.get(WorkshopLayout.GLOBAL).mayPlace(new ItemStack(Workshop.UPGRADES.get(Upgrade.FILTER))));
        assertFalse(menu.slots.get(WorkshopLayout.parent(2) + 1).mayPlace(new ItemStack(Workshop.UPGRADES.get(Upgrade.CHARGED))));
    }

    @Test
    void fuelTransferContextDoesNotHijackTheSelectedMainTab() {
        WorkshopMenu menu = menu();
        menu.slots.get(WorkshopLayout.parent(3)).set(new ItemStack(Items.CHEST));
        select(menu, 0, 3);
        click(menu, 3, 0);
        assertEquals(3, menu.selectedUnit());
        assertEquals(4, menu.group());
        assertEquals(3, menu.page());
        select(menu, 3, 1);
        assertEquals(3, menu.selectedUnit());
        assertEquals(3, menu.group());
        assertEquals(1, menu.page());
    }

    @Test
    void openingSidePanelsKeepsTheStationVisibleAndHidesUnsettledUpgradeSlots() {
        WorkshopMenu menu = menu();
        menu.slots.get(WorkshopLayout.parent(0)).set(new ItemStack(Items.CRAFTING_TABLE));
        for (int page = 0; page < 5; page++) {
            select(menu, 0, page);
            assertTrue(menu.slots.get(WorkshopLayout.slot(0, WorkshopLayout.GRID)).isActive());
            assertTrue(menu.slots.get(WorkshopLayout.SIZE).isActive());
        }
        select(menu, 0, 2);
        menu.setSidePanelReady(false);
        assertFalse(menu.slots.get(WorkshopLayout.parent(0) + 1).isActive());
        assertFalse(menu.slots.get(WorkshopLayout.GLOBAL).isActive());
        assertTrue(menu.slots.get(WorkshopLayout.slot(0, WorkshopLayout.GRID)).isActive());
        menu.setSidePanelReady(true);
        assertTrue(menu.slots.get(WorkshopLayout.parent(0) + 1).isActive());
        assertTrue(menu.slots.get(WorkshopLayout.GLOBAL).isActive());
    }

    @Test
    void activeSlotsNeverOverlapAndFitTheCompactMinecraftPanel() {
        WorkshopMenu menu = menu();
        for (int unit = 0; unit < 4; unit++) {
            menu.slots.get(WorkshopLayout.parent(unit)).set(new ItemStack(
                unit < 2 ? Items.CRAFTING_TABLE : unit == 2 ? Items.FURNACE : Items.CHEST));
            if (unit < 2) {
                menu.slots.get(WorkshopLayout.parent(unit) + 1).set(new ItemStack(Workshop.UPGRADES.get(Upgrade.STORAGE)));
                menu.slots.get(WorkshopLayout.parent(unit) + 2).set(new ItemStack(Workshop.UPGRADES.get(Upgrade.AUTO_CRAFTER)));
            } else if (unit == 2) {
                menu.slots.get(WorkshopLayout.parent(unit) + 1).set(new ItemStack(Workshop.UPGRADES.get(Upgrade.QUEUE), 3));
            }
        }
        for (int page = 0; page < 5; page++) {
            for (int selected = 0; selected < 4; selected++) {
                select(menu, selected, page);
                Set<Integer> pixels = new HashSet<>();
                for (Slot slot : menu.slots) {
                    if (!slot.isActive()) {
                        continue;
                    }
                    if (slot.index >= WorkshopLayout.UPGRADES && slot.index < WorkshopLayout.SIZE
                        && !WorkshopMenu.isModuleSlot(slot.index)) {
                        assertTrue(slot.x >= WorkshopSidebar.TAB_X + 4 && slot.x + 16 <= WorkshopSidebar.TAB_X + 108);
                        assertTrue(slot.y >= WorkshopSidebar.TAB_Y + 4 && slot.y + 16 <= WorkshopSidebar.TAB_Y + 134);
                    } else {
                        assertTrue(slot.x >= 4 && slot.x + 16 <= WorkshopMenu.PANEL_WIDTH - 4, "Slot " + slot.index);
                        assertTrue(slot.y >= 4 && slot.y + 16 <= WorkshopMenu.IMAGE_HEIGHT - 4, "Slot " + slot.index);
                    }
                    for (int x = slot.x; x < slot.x + 16; x++) {
                        for (int y = slot.y; y < slot.y + 16; y++) {
                            assertTrue(pixels.add(y * 320 + x),
                                "Overlapping slot " + slot.index + ", page " + page + ", unit " + selected);
                        }
                    }
                }
            }
        }
        assertTrue(WorkshopMenu.IMAGE_WIDTH <= 320);
        assertTrue(WorkshopMenu.IMAGE_HEIGHT <= 240);
    }

    @Test
    void quickMovingItemsCannotFillAHiddenStation() {
        WorkshopMenu menu = menu();
        for (int unit = 0; unit < 2; unit++) {
            menu.slots.get(WorkshopLayout.parent(unit)).set(new ItemStack(Items.CHEST));
        }
        select(menu, 1, 0);
        menu.slots.get(WorkshopLayout.FUEL).set(new ItemStack(Items.COAL, 64));
        menu.slots.get(235).set(new ItemStack(Items.DIAMOND, 12));
        assertEquals(12, menu.quickMoveStack(null, 235).getCount());
        assertTrue(menu.slots.get(235).getItem().isEmpty());
        assertTrue(menu.slots.get(WorkshopLayout.slot(0, WorkshopLayout.STORAGE)).getItem().isEmpty());
        assertEquals(12, menu.slots.get(WorkshopLayout.slot(1, WorkshopLayout.STORAGE)).getItem().getCount());
        for (int unit = 0; unit < 4; unit++) {
            assertFalse(menu.canTakeItemForPickAll(new ItemStack(Items.CHEST), menu.slots.get(WorkshopLayout.parent(unit))));
        }
    }

    @Test
    void faceDiagramTracksTheWorkshopPlacementDirection() {
        WorkshopMenu menu = menu();
        for (Direction front : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            menu.setData(31, front.ordinal());
            assertEquals(front, menu.front());
            Set<Direction> faces = new HashSet<>();
            Set<Integer> positions = new HashSet<>();
            for (WorkshopMenu.Face face : WorkshopMenu.Face.values()) {
                assertTrue(faces.add(face.direction(menu.front())));
                assertTrue(positions.add(face.row * 3 + face.column));
                assertTrue(face.column >= 0 && face.column <= 2 && face.row >= 0 && face.row <= 2);
            }
            assertEquals(front, WorkshopMenu.Face.FRONT.direction(menu.front()));
            assertEquals(front.getOpposite(), WorkshopMenu.Face.BACK.direction(menu.front()));
            assertEquals(front.getClockWise(), WorkshopMenu.Face.LEFT.direction(menu.front()));
            assertEquals(front.getCounterClockWise(), WorkshopMenu.Face.RIGHT.direction(menu.front()));
            assertEquals(Direction.UP, WorkshopMenu.Face.TOP.direction(menu.front()));
            assertEquals(Direction.DOWN, WorkshopMenu.Face.BOTTOM.direction(menu.front()));
        }
        assertThrows(IllegalArgumentException.class, () -> WorkshopMenu.Face.FRONT.direction(Direction.UP));
    }

    @Test
    void packedFaceStatesKeepModesAndAutomaticDirectionsIndependent() {
        WorkshopMenu menu = menu();
        for (Direction face : Direction.values()) {
            for (int state = 0; state < 64; state++) {
                menu.setData(25 + face.ordinal(), state);
                int expectedMode = (state & TransferRule.ENABLED) != 0 ? 1 : 0;
                if ((state & TransferRule.ENABLED << 3) != 0) {
                    expectedMode |= 2;
                }
                assertEquals(expectedMode, menu.faceMode(face.ordinal()).ordinal());
                assertEquals((state & TransferRule.AUTO) != 0, menu.faceAutomatic(face.ordinal(), 0));
                assertEquals((state & TransferRule.AUTO << 3) != 0, menu.faceAutomatic(face.ordinal(), 1));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> menu.faceAutomatic(0, 2));
    }

    @Test
    void faceModesCycleThroughInputOutputBothAndDisabled() {
        WorkshopMenu.FaceMode mode = WorkshopMenu.FaceMode.NONE;
        for (WorkshopMenu.FaceMode expected : new WorkshopMenu.FaceMode[] {
            WorkshopMenu.FaceMode.INPUT, WorkshopMenu.FaceMode.OUTPUT, WorkshopMenu.FaceMode.BOTH, WorkshopMenu.FaceMode.NONE
        }) {
            mode = mode.next();
            assertEquals(expected, mode);
        }
    }

    @Test
    void inputAndOutputFiltersSynchronizeIndependentlyWithoutMovingPlayerSlots() {
        WorkshopMenu menu = menu();
        select(menu, 0, 4);
        menu.setData(2, Direction.NORTH.ordinal());
        menu.slots.get(225).set(new ItemStack(Items.OAK_PLANKS));
        menu.slots.get(271).set(new ItemStack(Items.IRON_INGOT));
        menu.setData(14, TransferRule.MatchMode.ITEM.ordinal());
        menu.setData(33, TransferRule.MatchMode.TAG.ordinal());
        menu.setData(25 + Direction.NORTH.ordinal(), TransferRule.WHITELIST | TransferRule.AUTO << 3);
        assertTrue(menu.filter(0, 0).is(Items.OAK_PLANKS));
        assertTrue(menu.filter(1, 0).is(Items.IRON_INGOT));
        assertEquals(TransferRule.MatchMode.ITEM, menu.mode(0, 0));
        assertEquals(TransferRule.MatchMode.TAG, menu.mode(1, 0));
        assertEquals(TransferRule.WHITELIST, menu.flags(0));
        assertEquals(TransferRule.AUTO, menu.flags(1));
        for (int slot = 235; slot < 271; slot++) {
            assertTrue(menu.slots.get(slot).isActive());
        }
        for (int slot : new int[] {225, 271}) {
            assertFalse(menu.slots.get(slot).isActive());
            assertFalse(menu.slots.get(slot).mayPickup(null));
            assertFalse(menu.slots.get(slot).mayPlace(new ItemStack(Items.DIAMOND)));
            assertFalse(menu.canTakeItemForPickAll(new ItemStack(Items.DIAMOND), menu.slots.get(slot)));
            assertTrue(menu.quickMoveStack(null, slot).isEmpty());
        }
        assertThrows(IllegalArgumentException.class, () -> menu.filter(2, 0));
        assertThrows(IllegalArgumentException.class, () -> menu.mode(2, 0));
        assertThrows(IllegalArgumentException.class, () -> menu.flags(2));
    }

    @Test
    void filterScopeFollowsTheStationUnlessSharedFuelIsSelected() {
        WorkshopMenu menu = menu();
        menu.slots.get(WorkshopLayout.parent(1)).set(new ItemStack(Items.CHEST));
        select(menu, 0, 4);
        click(menu, 1, 0);
        assertEquals(4, menu.page());
        assertEquals(1, menu.selectedUnit());
        assertEquals(1, menu.group());
        assertFalse(menu.filterFuel());
        menu.setData(32, 1);
        menu.setData(1, 4);
        click(menu, 3, 0);
        assertEquals(4, menu.page());
        assertEquals(3, menu.selectedUnit());
        assertEquals(4, menu.group());
        assertTrue(menu.filterFuel());
        assertTrue(menu.slots.get(WorkshopLayout.FUEL).isActive());
    }
}
*///? }
