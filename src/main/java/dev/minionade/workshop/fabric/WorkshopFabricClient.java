//? if fabric {
package dev.minionade.workshop.fabric;

import dev.minionade.workshop.Workshop;
import dev.minionade.workshop.client.WorkshopScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class WorkshopFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(Workshop.TABLE_MENU, WorkshopScreen::new);
    }
}
//? }
