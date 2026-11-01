package dev.minionade.workshop;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkshopSidebarTest {
    private static WorkshopSidebar.Bounds pane(WorkshopSidebar sidebar, int page, int width, int height) {
        return sidebar.bounds(width, height).stream().filter(bounds -> bounds.page() == page).findFirst().orElseThrow();
    }

    @Test
    void collapsedTabsHaveTheirActualIconBoundsAndStableOrder() {
        WorkshopSidebar sidebar = new WorkshopSidebar();
        assertEquals(72, sidebar.bodyLeft(320));
        var bounds = sidebar.bounds(320, 240);
        assertEquals(List.of(2, 1, 4, 3), bounds.stream().map(WorkshopSidebar.Bounds::page).toList());
        for (int index = 0; index < bounds.size(); index++) {
            assertEquals(246, bounds.get(index).x());
            assertEquals(5 + index * 26, bounds.get(index).y());
            assertEquals(24, bounds.get(index).width());
            assertEquals(24, bounds.get(index).height());
        }
    }

    @Test
    void tabsExpandAndCollapseOverTheExactAnimationDuration() {
        WorkshopSidebar sidebar = new WorkshopSidebar();
        sidebar.advance(1, 80);
        assertEquals(84, pane(sidebar, 1, 320, 240).width());
        assertEquals(59, pane(sidebar, 1, 320, 240).height());
        assertFalse(sidebar.fullyOpen(1));
        sidebar.advance(1, 80);
        assertTrue(sidebar.fullyOpen(1));
        assertEquals(144, pane(sidebar, 1, 320, 240).width());
        assertEquals(94, pane(sidebar, 1, 320, 240).height());
        sidebar.advance(0, 80);
        assertFalse(sidebar.fullyOpen(1));
        assertEquals(84, pane(sidebar, 1, 320, 240).width());
        sidebar.advance(0, 80);
        assertEquals(24, pane(sidebar, 1, 320, 240).width());
        assertEquals(24, pane(sidebar, 1, 320, 240).height());
    }

    @Test
    void openingAnotherPanelCollapsesThePreviousOneAndMovesLaterTabs() {
        WorkshopSidebar sidebar = new WorkshopSidebar();
        sidebar.advance(2, 160);
        assertTrue(sidebar.fullyOpen(2));
        assertEquals(112, pane(sidebar, 2, 320, 240).width());
        assertEquals(145, pane(sidebar, 1, 320, 240).y());
        sidebar.advance(1, 80);
        assertFalse(sidebar.fullyOpen(1));
        assertFalse(sidebar.fullyOpen(2));
        assertTrue(pane(sidebar, 2, 320, 240).width() < 112);
        assertTrue(pane(sidebar, 1, 320, 240).width() > 24);
        sidebar.advance(1, 80);
        assertTrue(sidebar.fullyOpen(1));
        assertFalse(sidebar.fullyOpen(2));
        assertEquals(24, pane(sidebar, 2, 320, 240).width());
        assertEquals(31, pane(sidebar, 1, 320, 240).y());
    }

    @Test
    void everyAnimationFrameAndInterruptedTransitionFitsTheMinimumGuiViewport() {
        for (int width : new int[] {320, 426, 640, 960}) {
            WorkshopSidebar sidebar = new WorkshopSidebar();
            for (int page : new int[] {2, 1, 3, 4, 0, 1, 2, 4, 3, 2, 0}) {
                for (int frame = 0; frame < 10; frame++) {
                    sidebar.advance(page, 16);
                    int bottom = 0;
                    assertTrue(sidebar.bodyLeft(width) >= 0);
                    assertTrue(sidebar.bodyLeft(width) + WorkshopMenu.IMAGE_WIDTH <= width);
                    for (WorkshopSidebar.Bounds bounds : sidebar.bounds(width, 240)) {
                        assertTrue(bounds.x() >= 0 && bounds.x() + bounds.width() <= width);
                        assertTrue(bounds.y() >= bottom && bounds.y() + bounds.height() <= 240);
                        bottom = bounds.y() + bounds.height();
                    }
                }
            }
            for (int frame = 0; frame < 100; frame++) {
                sidebar.advance(frame % 4 + 1, 9);
                for (WorkshopSidebar.Bounds bounds : sidebar.bounds(width, 240)) {
                    assertTrue(bounds.x() + bounds.width() <= width);
                    assertTrue(bounds.y() + bounds.height() <= 240);
                }
            }
        }
    }

    @Test
    void exclusionBoundsFollowThePanelRatherThanOnlyItsCollapsedIcon() {
        WorkshopSidebar sidebar = new WorkshopSidebar();
        var collapsed = pane(sidebar, 4, 640, 480);
        sidebar.advance(4, 160);
        var expanded = pane(sidebar, 4, 640, 480);
        assertEquals(collapsed.x(), expanded.x());
        assertTrue(expanded.width() > collapsed.width());
        assertTrue(expanded.height() > collapsed.height());
        assertTrue(expanded.contains(expanded.x() + 143, expanded.y() + 155));
        assertFalse(expanded.contains(expanded.x() + 144, expanded.y() + 155));
        assertFalse(expanded.contains(expanded.x() + 143, expanded.y() + 156));
        sidebar.advance(0, 160);
        assertEquals(collapsed, pane(sidebar, 4, 640, 480));
    }

    @Test
    void largeElapsedTimesSettleWithoutOvershootAndInvalidStatesAreRejected() {
        WorkshopSidebar sidebar = new WorkshopSidebar();
        sidebar.advance(2, Integer.MAX_VALUE);
        assertTrue(sidebar.fullyOpen(2));
        sidebar.advance(3, Integer.MAX_VALUE);
        assertTrue(sidebar.fullyOpen(3));
        assertFalse(sidebar.fullyOpen(2));
        assertThrows(IllegalArgumentException.class, () -> sidebar.advance(5, 16));
        assertThrows(IllegalArgumentException.class, () -> sidebar.advance(-1, 16));
        assertThrows(IllegalArgumentException.class, () -> sidebar.advance(1, -1));
    }

    @Test
    void bothFilterSectionsFitAboveTheJeiFooterWithoutOverlapping() {
        WorkshopSidebar sidebar = new WorkshopSidebar();
        sidebar.advance(4, 160);
        var filters = pane(sidebar, 4, 320, 240);
        assertEquals(144, filters.width());
        assertEquals(156, filters.height());
        assertTrue(filters.y() + filters.height() <= 240 - 26);
        for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
            int x = WorkshopSidebar.FILTER_SLOT_X + filter % 5 * 24;
            int inputY = WorkshopSidebar.FILTER_INPUT_Y + filter / 5 * WorkshopSidebar.FILTER_ROW_STEP;
            int outputY = WorkshopSidebar.FILTER_OUTPUT_Y + filter / 5 * WorkshopSidebar.FILTER_ROW_STEP;
            assertTrue(x >= 8 && x + 18 <= filters.width() - 8);
            assertTrue(inputY + 18 < 103);
            assertTrue(outputY >= 116 && outputY + 18 <= filters.height() - 4);
        }
    }
}
