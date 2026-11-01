package dev.minionade.workshop;

public final class WorkshopLayout {
    public static final int UNIT_COUNT = 4;
    public static final int FUEL = 0;
    public static final int UNIT_SIZE = 45;
    public static final int GRID = 0;
    public static final int CRAFT_STORAGE = 9;
    public static final int CRAFT_OUTPUT = 15;
    public static final int SMELT_INPUT = 16;
    public static final int SMELT_OUTPUT = 17;
    public static final int QUEUE = 18;
    public static final int STORAGE = 21;
    public static final int UPGRADES = 1 + UNIT_COUNT * UNIT_SIZE;
    public static final int UPGRADE_SIZE = 8;
    public static final int GLOBAL = UPGRADES + UNIT_COUNT * UPGRADE_SIZE;
    public static final int SIZE = GLOBAL + 8;
    public static final int FILTER_SIZE = 10;
    public static final int RULE_COUNT = 5 * 6 * 2;

    private WorkshopLayout() {}

    public static int slot(int unit, int offset) {
        if (unit < 0 || unit >= UNIT_COUNT || offset < 0 || offset >= UNIT_SIZE) {
            throw new IllegalArgumentException("Invalid workshop unit or slot: " + unit + "/" + offset);
        }
        return 1 + unit * UNIT_SIZE + offset;
    }

    public static int parent(int unit) {
        if (unit < 0 || unit >= UNIT_COUNT) {
            throw new IllegalArgumentException("Invalid workshop unit: " + unit);
        }
        return UPGRADES + unit * UPGRADE_SIZE;
    }

    public static int rule(int group, int side, int direction) {
        if (group < 0 || group > 4 || side < 0 || side >= 6 || direction < 0 || direction > 1) {
            throw new IllegalArgumentException("Invalid transfer selection: " + group + "/" + side + "/" + direction);
        }
        return (group * 6 + side) * 2 + direction;
    }
}
