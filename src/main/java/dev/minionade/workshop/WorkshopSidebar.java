package dev.minionade.workshop;

import java.util.ArrayList;
import java.util.List;

public final class WorkshopSidebar {
    public static final int TAB_SIZE = 24;
    public static final int TAB_X = 174;
    public static final int TAB_Y = 4;
    public static final int ANIMATION_MILLIS = 160;
    public static final int FILTER_SLOT_X = 15;
    public static final int FILTER_INPUT_Y = 62;
    public static final int FILTER_OUTPUT_Y = 116;
    public static final int FILTER_ROW_STEP = 18;
    private static final int[] ORDER = {2, 1, 4, 3};
    private final float[] progress = new float[5];

    public record Bounds(int page, int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    public void advance(int page, long elapsedMillis) {
        if (page < 0 || page > 4 || elapsedMillis < 0) {
            throw new IllegalArgumentException("Invalid sidebar animation state");
        }
        float step = Math.min(1F, (float) elapsedMillis / ANIMATION_MILLIS);
        for (int tab : ORDER) {
            progress[tab] = tab == page ? Math.min(1F, progress[tab] + step) : Math.max(0F, progress[tab] - step);
        }
    }

    public boolean fullyOpen(int page) {
        return page > 0 && page < progress.length && progress[page] >= 1F;
    }

    public int bodyLeft(int viewportWidth) {
        int maximum = TAB_SIZE;
        for (int tab : ORDER) {
            maximum = Math.max(maximum, width(tab));
        }
        return Math.max(0, Math.min((viewportWidth - WorkshopMenu.IMAGE_WIDTH) / 2,
            viewportWidth - TAB_X - maximum - 2));
    }

    public List<Bounds> bounds(int viewportWidth, int viewportHeight) {
        int x = bodyLeft(viewportWidth) + TAB_X;
        int y = Math.max(0, (viewportHeight - WorkshopMenu.IMAGE_HEIGHT) / 2) + TAB_Y;
        List<Bounds> bounds = new ArrayList<>(ORDER.length);
        for (int tab : ORDER) {
            int height = size(tab, tab == 2 ? 138 : tab == 4 ? 156 : 94);
            bounds.add(new Bounds(tab, x, y, width(tab), height));
            y += height + 2;
        }
        return List.copyOf(bounds);
    }

    private int width(int page) {
        return size(page, page == 2 ? 112 : 144);
    }

    private int size(int page, int expanded) {
        float t = progress[page];
        float eased = t * t * (3F - 2F * t);
        return Math.round(TAB_SIZE + (expanded - TAB_SIZE) * eased);
    }
}
