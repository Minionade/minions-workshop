package dev.minionade.workshop;

public final class WorkshopRules {
    public static final int PRODUCTION_TIME = 400;
    public static final int MAX_FUEL = 8000;

    private WorkshopRules() {}

    public static int speed(int upgrades, boolean charging) {
        return (1 + Math.clamp(upgrades, 0, 8)) * (charging ? 1 : 4);
    }

    public static int consumption(int upgrades, boolean charging, boolean smelting) {
        return (1 + 2 * Math.clamp(upgrades, 0, 8)) * (charging ? 2 : 1) * (smelting ? 2 : 1);
    }

    public static int maxCharges(int upgrades) {
        return Math.clamp(upgrades, 0, 8) * 4;
    }

    public static int transferSize(int upgrades) {
        return 1 << Math.clamp(upgrades, 0, 6);
    }
}
