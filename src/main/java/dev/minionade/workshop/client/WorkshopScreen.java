package dev.minionade.workshop.client;

import dev.minionade.workshop.Module;
import dev.minionade.workshop.TransferRule;
import dev.minionade.workshop.Upgrade;
import dev.minionade.workshop.Workshop;
import dev.minionade.workshop.WorkshopLayout;
import dev.minionade.workshop.WorkshopMenu;
import dev.minionade.workshop.WorkshopRules;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
//? if >=26.1 {
/*import net.minecraft.client.renderer.RenderPipelines;
*///? }

public final class WorkshopScreen extends AbstractContainerScreen<WorkshopMenu> {
    private static final ResourceLocation BACKGROUND = Workshop.id("textures/gui/background.png");
    private final Map<Integer, Button> buttons = new LinkedHashMap<>();

    public WorkshopScreen(WorkshopMenu menu, Inventory inventory, Component title) {
        //? if >=26.1 {
        /*super(menu, inventory, title, 256, 256);
        *///? } else {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 256;
        //? }
    }

    private static Component text(String key) {
        return Component.translatable("gui.minions_workshop." + key);
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        button(0, 258, 12, 22, 20, text("workshop.short"), text("workshop"));
        button(1, 258, 36, 22, 20, text("transfer.short"), text("transfer"));
        button(2, 258, 60, 22, 20, text("upgrades.short"), text("upgrades"));
        for (int unit = 0; unit < 4; unit++) {
            button(70 + unit, 64 + unit % 2 * 128, 5 + unit / 2 * 87, 16, 14, Component.literal("x"), text("clear_grid"));
        }
        for (int group = 0; group < 5; group++) {
            Component label = group == 4 ? text("fuel") : Component.translatable("gui.minions_workshop.unit", group + 1);
            button(10 + group, 8 + group * 48, 12, 44, 20, label, label);
        }
        for (Direction direction : Direction.values()) {
            int index = direction.ordinal();
            button(20 + index, 8 + index * 40, 38, 36, 20,
                text("side." + direction.getSerializedName() + ".short"), text("side." + direction.getSerializedName()));
        }
        button(30, 8, 64, 80, 18, text("input"), text("input"));
        button(31, 92, 64, 80, 18, text("output"), text("output"));
        button(40, 178, 64, 70, 18, text("enabled"), text("enabled"));
        button(41, 8, 86, 90, 18, text("automatic"), text("automatic"));
        button(42, 104, 86, 144, 18, text("blacklist"), text("blacklist"));
        for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
            button(50 + filter, 10 + filter * 23, 116, 20, 20, Component.empty(), text("filter.help"));
            button(60 + filter, 10 + filter * 23, 140, 20, 16, Component.literal("P"), text("mode.precise"));
        }
        updateButtons();
    }

    private void button(int id, int x, int y, int width, int height, Component label, Component tooltip) {
        Button button = Button.builder(label, ignored -> {
            if (minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            }
        }).bounds(leftPos + x, topPos + y, width, height).tooltip(Tooltip.create(tooltip)).build();
        buttons.put(id, addRenderableWidget(button));
    }

    private void updateButtons() {
        buttons.forEach((id, button) -> {
            button.visible = id < 3 || (id >= 70 ? menu.page() == 0 && menu.module(id - 70) == Module.CRAFTING : menu.page() == 1);
            button.active = true;
            if (id < 3) {
                button.active = menu.page() != id;
            } else if (id >= 10 && id < 15) {
                button.active = menu.group() != id - 10 && (id == 14 || menu.module(id - 10) != Module.EMPTY);
            } else if (id >= 20 && id < 26) {
                button.active = menu.side() != id - 20;
            } else if (id == 30 || id == 31) {
                button.active = menu.direction() != id - 30;
            } else if (id == 40 || id == 41) {
                int flag = id == 40 ? TransferRule.ENABLED : TransferRule.AUTO;
                button.setMessage(Component.translatable("gui.minions_workshop.toggle",
                    text(id == 40 ? "enabled" : "automatic"), text((menu.flags() & flag) != 0 ? "yes" : "no")));
            }
            if (id == 41) {
                button.visible &= menu.upgrades(4, Upgrade.AUTO_TRANSFER) > 0;
            }
            if (id == 42 || id >= 50 && id < 70) {
                button.visible &= menu.upgrades(4, Upgrade.FILTER) > 0;
            }
            if (id == 42) {
                button.setMessage(text((menu.flags() & TransferRule.WHITELIST) != 0 ? "whitelist" : "blacklist"));
            } else if (id >= 60 && id < 70) {
                String mode = menu.mode(id - 60).getSerializedName();
                button.setMessage(text("mode." + mode + ".short"));
                button.setTooltip(Tooltip.create(text("mode." + mode)));
            }
        });
    }

    //? if >=26.1 {
    /*@Override
    public void extractBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        drawBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        updateButtons();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        drawFilters(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
    *///? } else {
    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        drawBackground(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        updateButtons();
        super.render(graphics, mouseX, mouseY, delta);
        drawFilters(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
    //? }

    private void drawBackground(GuiGraphics graphics) {
        //? if >=26.1 {
        /*graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, 256, 256, 256, 256);
        *///? } else {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, 256, 256);
        //? }
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF777777);
                graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFFBBBBBB);
            }
        }
        if (menu.page() == 0) {
            graphics.fill(leftPos + 126, topPos + 5, leftPos + 129, topPos + 170, 0xFF777777);
            graphics.fill(leftPos + 5, topPos + 85, leftPos + 250, topPos + 88, 0xFF777777);
            for (int unit = 0; unit < 4; unit++) {
                int x = leftPos + unit % 2 * 128;
                int y = topPos + unit / 2 * 87;
                Module module = menu.module(unit);
                if (module == Module.EMPTY) {
                    graphics.drawString(font, text("empty"), x + 12, y + 26, 0xFF444444, false);
                    graphics.drawString(font, text("empty.help"), x + 12, y + 40, 0xFF444444, false);
                } else if (module != Module.STORAGE) {
                    graphics.fill(x + 66, y + 25, x + 86, y + 34, 0xFF777777);
                    int progress = menu.progress(unit) * 20 / WorkshopRules.PRODUCTION_TIME;
                    graphics.fill(x + 66, y + 25, x + 66 + progress, y + 34, 0xFFB77837);
                    if (menu.charges(unit) > 0) {
                        graphics.drawString(font, Component.translatable("gui.minions_workshop.charges", menu.charges(unit)), x + 64, y + 50, 0xFF287340, false);
                    }
                }
            }
        } else if (menu.page() == 2) {
            for (int unit = 0; unit < 4; unit++) {
                graphics.drawString(font, Component.translatable("gui.minions_workshop.unit", unit + 1),
                    leftPos + 10 + unit % 2 * 128, topPos + 10 + unit / 2 * 52, 0xFF444444, false);
            }
            graphics.drawString(font, text("global"), leftPos + 10, topPos + 134, 0xFF444444, false);
        }
        graphics.drawString(font, text("fuel"), leftPos + 216, topPos + 213, 0xFF444444, false);
        graphics.fill(leftPos + 201, topPos + 246, leftPos + 245, topPos + 251, 0xFF777777);
        graphics.fill(leftPos + 201, topPos + 246, leftPos + 201 + menu.fuel() * 44 / WorkshopRules.MAX_FUEL, topPos + 251, 0xFFB77837);
    }

    private void drawFilters(GuiGraphics graphics) {
        if (menu.page() == 1 && menu.upgrades(4, Upgrade.FILTER) > 0) {
            for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
                graphics.renderItem(menu.filter(filter), leftPos + 12 + filter * 23, topPos + 118);
            }
        }
    }
}
