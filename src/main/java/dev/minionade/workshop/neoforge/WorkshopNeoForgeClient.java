//? if neoforge {
/*package dev.minionade.workshop.neoforge;

import dev.minionade.workshop.Workshop;
import dev.minionade.workshop.client.WorkshopScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class WorkshopNeoForgeClient {
    private WorkshopNeoForgeClient() {}

    public static void register(IEventBus bus) {
        bus.addListener(WorkshopNeoForgeClient::screens);
    }

    private static void screens(RegisterMenuScreensEvent event) {
        event.register(Workshop.TABLE_MENU, WorkshopScreen::new);
    }
}
*///? }
