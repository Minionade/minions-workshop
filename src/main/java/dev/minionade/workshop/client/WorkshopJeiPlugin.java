package dev.minionade.workshop.client;

import dev.minionade.workshop.Workshop;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class WorkshopJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return Workshop.id("workshop");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(WorkshopScreen.class, new IGuiContainerHandler<WorkshopScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(WorkshopScreen screen) {
                return screen.sideTabAreas();
            }
        });
    }
}
