package dev.minionade.workshop.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.minionade.workshop.Module;
import dev.minionade.workshop.TransferRule;
import dev.minionade.workshop.Upgrade;
import dev.minionade.workshop.Workshop;
import dev.minionade.workshop.WorkshopLayout;
import dev.minionade.workshop.WorkshopMenu;
import dev.minionade.workshop.WorkshopMenu.Face;
import dev.minionade.workshop.WorkshopMenu.FaceMode;
import dev.minionade.workshop.WorkshopRules;
import dev.minionade.workshop.WorkshopSidebar;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=26.1 {
/*import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
*///? }

public final class WorkshopScreen extends AbstractContainerScreen<WorkshopMenu> {
    private static final int SURFACE = 0xFFC6C6C6;
    private static final int TEXT = 0xFF404040;
    private static final int SHADOW = 0xFF555555;
    private static final ResourceLocation FURNACE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final ResourceLocation FLAME = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private record Position(int x, int y) {}
    private final Map<Integer, Button> buttons = new LinkedHashMap<>();
    private final Map<Integer, Position> positions = new LinkedHashMap<>();
    private final WorkshopSidebar sidebar = new WorkshopSidebar();
    private List<WorkshopSidebar.Bounds> panes = List.of();
    private long lastFrameMillis;

    public WorkshopScreen(WorkshopMenu menu, Inventory inventory, Component title) {
        //? if >=26.1 {
        /*super(menu, inventory, title, WorkshopMenu.IMAGE_WIDTH, WorkshopMenu.IMAGE_HEIGHT);
        *///? } else {
        super(menu, inventory, title);
        imageWidth = WorkshopMenu.IMAGE_WIDTH;
        imageHeight = WorkshopMenu.IMAGE_HEIGHT;
        //? }
        menu.setSidePanelReady(false);
    }

    private static Component text(String key) {
        return Component.translatable("gui.minions_workshop." + key);
    }

    public List<Rect2i> sideTabAreas() {
        return panes.stream().map(pane -> new Rect2i(pane.x(), pane.y(), pane.width(), pane.height())).toList();
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        positions.clear();
        for (int page : new int[] {2, 1, 4, 3}) {
            buttons.put(page, addRenderableWidget(new IconTab(page)));
        }
        for (Face face : Face.values()) {
            add(20 + face.ordinal(), new FaceButton(face), 8 + face.column * 20, 28 + face.row * 20);
        }
        add(43, new AutoButton(0), 84, 68);
        add(44, new AutoButton(1), 104, 68);
        choice(6, 8, 26, 62, 16, text("fuel"), text("filter.scope.help"), menu::filterFuel);
        add(7, new FilterFaceButton(), 74, 26);
        for (int flow = 0; flow < 2; flow++) {
            int y = flow == 0 ? WorkshopSidebar.FILTER_INPUT_Y : WorkshopSidebar.FILTER_OUTPUT_Y;
            button(flow == 0 ? 42 : 47, 76, y - 16, 60, 14, text("blacklist.short"), text("blacklist"));
            for (int filter = 0; filter < WorkshopLayout.FILTER_SIZE; filter++) {
                add((flow == 0 ? 50 : 80) + filter, new GhostButton(flow, filter),
                    WorkshopSidebar.FILTER_SLOT_X + filter % 5 * 24, y + filter / 5 * WorkshopSidebar.FILTER_ROW_STEP);
            }
        }
        for (int unit = 0; unit < WorkshopLayout.UNIT_COUNT; unit++) {
            button(70 + unit, 148, 108, 18, 18, Component.literal("x"), text("clear_grid"));
        }
        lastFrameMillis = System.nanoTime() / 1_000_000L;
        updateLayout();
    }

    private void send(int id) {
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        } else {
            Workshop.LOGGER.warn("Cannot send workshop action {} without an active game mode", id);
        }
    }

    private void add(int id, Button button, int x, int y) {
        positions.put(id, new Position(x, y));
        buttons.put(id, addRenderableWidget(button));
    }

    private void button(int id, int x, int y, int width, int height, Component label, Component tooltip) {
        add(id, Button.builder(label, ignored -> send(id))
            .bounds(0, 0, width, height).tooltip(Tooltip.create(tooltip)).build(), x, y);
    }

    private void choice(int id, int x, int y, int width, int height, Component label, Component tooltip, BooleanSupplier selected) {
        add(id, new ChoiceButton(id, width, height, label, tooltip, selected), x, y);
    }

    private WorkshopSidebar.Bounds pane(int page) {
        return panes.stream().filter(bounds -> bounds.page() == page).findFirst().orElseThrow();
    }

    private void updateLayout() {
        long now = System.nanoTime() / 1_000_000L;
        sidebar.advance(menu.page(), Math.max(0, now - lastFrameMillis));
        lastFrameMillis = now;
        leftPos = sidebar.bodyLeft(width);
        topPos = Math.max(0, (height - imageHeight) / 2);
        panes = sidebar.bounds(width, height);
        menu.setSidePanelReady(menu.page() == 2 && sidebar.fullyOpen(2));
        boolean transfer = (menu.page() == 1 || menu.page() == 3) && sidebar.fullyOpen(menu.page());
        boolean filtering = menu.page() == 4 && sidebar.fullyOpen(4);
        boolean automatic = menu.upgrades(4, Upgrade.AUTO_TRANSFER) > 0;
        boolean filters = menu.upgrades(4, Upgrade.FILTER) > 0;
        buttons.forEach((id, button) -> {
            if (id < 5) {
                WorkshopSidebar.Bounds bounds = pane(id);
                button.setPosition(bounds.x(), bounds.y());
                button.setWidth(bounds.width());
                return;
            }
            Position position = positions.get(id);
            boolean station = id >= 70 && id < 74;
            boolean filterControl = id == 6 || id == 7 || id == 42 || id == 47 || button instanceof GhostButton;
            WorkshopSidebar.Bounds bounds = pane(filterControl ? 4 : menu.page() == 3 ? 3 : 1);
            button.setPosition((station ? leftPos : bounds.x()) + position.x(),
                (station ? topPos : bounds.y()) + position.y());
            button.visible = station
                ? menu.selectedUnit() == id - 70 && menu.module(menu.selectedUnit()) == Module.CRAFTING
                : filterControl ? filtering : transfer;
            button.active = true;
            if (button instanceof FaceButton face) {
                Component label = face.label();
                button.setMessage(label);
                button.setTooltip(Tooltip.create(label.copy().append("\n").append(text("face.help"))));
            } else if (id == 6) {
                Component scope = menu.filterFuel() ? text("fuel.shared")
                    : Component.translatable("gui.minions_workshop.unit", menu.selectedUnit() + 1);
                button.setMessage(menu.filterFuel() ? text("fuel") : scope);
                button.setTooltip(Tooltip.create(scope.copy().append("\n").append(text("filter.scope.help"))));
            } else if (id == 7) {
                Component face = text("side." + Direction.values()[menu.side()].getSerializedName());
                button.setMessage(face);
                button.setTooltip(Tooltip.create(face.copy().append("\n").append(text("filter.face.help"))));
            } else if (id == 43 || id == 44) {
                Component label = text(id == 43 ? "automatic.pull" : "automatic.push");
                button.active = automatic;
                button.setMessage(Component.translatable("gui.minions_workshop.toggle", label,
                    text(menu.faceAutomatic(menu.side(), id - 43) ? "yes" : "no")));
                button.setTooltip(Tooltip.create(button.getMessage().copy().append("\n")
                    .append(text(automatic ? "automatic.face.help" : "automatic.locked"))));
            } else if (id == 42 || id == 47) {
                boolean whitelist = (menu.flags(id == 42 ? 0 : 1) & TransferRule.WHITELIST) != 0;
                button.active = filters;
                button.visible &= filters;
                button.setMessage(text(whitelist ? "whitelist.short" : "blacklist.short"));
                button.setTooltip(Tooltip.create(text(whitelist ? "whitelist" : "blacklist")));
            } else if (button instanceof GhostButton ghost) {
                button.visible &= filters;
                Component label = Component.translatable("gui.minions_workshop.filter.direction.slot",
                    text(ghost.flow == 0 ? "input" : "output"), ghost.filter + 1);
                Component tooltip = Component.empty().append(label).append("\n");
                ItemStack sample = menu.filter(ghost.flow, ghost.filter);
                if (!sample.isEmpty()) {
                    tooltip = tooltip.copy().append(sample.getHoverName()).append("\n");
                }
                button.setMessage(label);
                button.setTooltip(Tooltip.create(tooltip.copy().append(text("mode." + menu.mode(ghost.flow, ghost.filter).getSerializedName()))
                    .append("\n").append(text("filter.help"))));
            }
        });
    }

    //? if >=26.1 {
    /*@Override
    public void extractBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        updateLayout();
        drawBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        drawExtraTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int x, int y) {
        return panes.stream().noneMatch(bounds -> bounds.contains(mouseX, mouseY))
            && super.hasClickedOutside(mouseX, mouseY, x, y);
    }
    *///? } else {
    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        drawBackground(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        updateLayout();
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
        drawExtraTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int x, int y, int button) {
        return panes.stream().noneMatch(bounds -> bounds.contains(mouseX, mouseY))
            && super.hasClickedOutside(mouseX, mouseY, x, y, button);
    }
    //? }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot != null && WorkshopMenu.isModuleSlot(hoveredSlot.index)) {
            int unit = (hoveredSlot.index - WorkshopLayout.UPGRADES) / WorkshopLayout.UPGRADE_SIZE;
            tooltip.add(Component.translatable("gui.minions_workshop.unit", unit + 1).withStyle(ChatFormatting.GRAY));
            tooltip.add(text("module.select").copy().withStyle(ChatFormatting.GRAY));
            tooltip.add(text("module.remove").copy().withStyle(ChatFormatting.GRAY));
        }
        return tooltip;
    }

    private static int color(int page) {
        return switch (page) {
            case 1 -> 0xFF9B8255;
            case 2 -> 0xFF6D965F;
            case 3 -> 0xFF9B684B;
            case 4 -> 0xFF5B85A3;
            default -> throw new IllegalArgumentException("Invalid sidebar page: " + page);
        };
    }

    private void drawBackground(GuiGraphics graphics) {
        for (WorkshopSidebar.Bounds bounds : panes) {
            panel(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), color(bounds.page()), false);
            graphics.enableScissor(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height());
            if (bounds.page() == 2) {
                panelLabel(graphics, "module.upgrades", bounds.x() + 8, bounds.y() + 27, 96);
                panelLabel(graphics, "global", bounds.x() + 8, bounds.y() + 82, 96);
            } else if (bounds.page() == 4) {
                if (menu.upgrades(4, Upgrade.FILTER) > 0) {
                    panelLabel(graphics, "input", bounds.x() + 8, bounds.y() + 49, 66);
                    panelLabel(graphics, "output", bounds.x() + 8, bounds.y() + 103, 66);
                } else {
                    int y = bounds.y() + 54;
                    for (FormattedCharSequence line : font.split(text("filter.locked"), 128)) {
                        graphics.drawString(font, line, bounds.x() + 8, y, 0xFFFFFFFF, true);
                        y += font.lineHeight + 2;
                    }
                }
            } else {
                for (FaceMode mode : FaceMode.values()) {
                    int y = bounds.y() + 28 + mode.ordinal() * 9;
                    graphics.fill(bounds.x() + 80, y + 2, bounds.x() + 85, y + 7, modeColor(mode));
                    panelLabel(graphics, "face.mode." + mode.getSerializedName(), bounds.x() + 89, y, 48);
                }
            }
            graphics.disableScissor();
        }
        panel(graphics, leftPos, topPos, imageWidth, imageHeight, SURFACE, false);
        for (int unit = 0; unit < WorkshopLayout.UNIT_COUNT; unit++) {
            int x = leftPos + WorkshopMenu.MODULE_TAB_X + unit * WorkshopMenu.MODULE_TAB_STEP;
            int y = topPos + WorkshopMenu.MODULE_TAB_Y;
            panel(graphics, x, y, 28, 24, menu.selectedUnit() == unit ? SURFACE : 0xFF999999, false);
            if (menu.moduleItem(unit).isEmpty()) {
                graphics.drawString(font, Component.literal("+"), x + 8, y + 7, TEXT, false);
            }
            graphics.drawString(font, Component.literal(Integer.toString(unit + 1)), x + 21, y + 13, TEXT, false);
        }
        for (Slot slot : menu.slots) {
            if (slot.isActive() && !WorkshopMenu.isModuleSlot(slot.index)) {
                boolean result = slot.index >= WorkshopLayout.SIZE && slot.index < WorkshopLayout.SIZE + 4
                    || slot.index > 0 && slot.index < WorkshopLayout.UPGRADES
                    && (slot.index - 1) % WorkshopLayout.UNIT_SIZE == WorkshopLayout.SMELT_OUTPUT;
                int inset = result ? 5 : 1;
                slotWell(graphics, leftPos + slot.x - inset, topPos + slot.y - inset, result ? 26 : 18);
            }
        }
        graphics.drawString(font, title, leftPos + 8, topPos + 6, TEXT, false);
        drawStation(graphics);
        drawFuel(graphics);
        graphics.drawString(font, playerInventoryTitle, leftPos + WorkshopMenu.INVENTORY_X,
            topPos + WorkshopMenu.INVENTORY_Y - 10, TEXT, false);
    }

    private void drawStation(GuiGraphics graphics) {
        int unit = menu.selectedUnit();
        Module module = menu.module(unit);
        Component heading = module == Module.EMPTY ? text("empty") : menu.moduleItem(unit).getHoverName();
        int headingX = module == Module.SMELTING && menu.upgrades(unit, Upgrade.QUEUE) > 0 ? 50 : 8;
        graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(heading, 160 - headingX)),
            leftPos + headingX, topPos + 44, TEXT, false);
        if (module == Module.EMPTY) {
            int y = 59;
            for (FormattedCharSequence line : font.split(text("module.install"), 160)) {
                graphics.drawString(font, line, leftPos + 8 + (160 - font.width(line)) / 2, topPos + y, TEXT, false);
                y += font.lineHeight + 2;
            }
            graphics.renderItem(new ItemStack(Items.CRAFTING_TABLE), leftPos + 38, topPos + 104);
            graphics.renderItem(new ItemStack(Items.FURNACE), leftPos + 80, topPos + 104);
            graphics.renderItem(new ItemStack(Items.CHEST), leftPos + 122, topPos + 104);
        } else if (module == Module.CRAFTING) {
            label(graphics, "craft", 104, 56);
            boolean automatic = menu.upgrades(unit, Upgrade.AUTO_CRAFTER) > 0;
            drawArrow(graphics, 76, 72, automatic ? menu.progress(unit) * 24 / WorkshopRules.PRODUCTION_TIME
                : menu.slots.get(WorkshopLayout.SIZE + unit).hasItem() ? 24 : 0);
            if (automatic) {
                label(graphics, "automatic.short", 138, 56);
            }
        } else if (module == Module.SMELTING) {
            drawArrow(graphics, 80, 72, menu.progress(unit) * 24 / WorkshopRules.PRODUCTION_TIME);
            if (menu.upgrades(unit, Upgrade.QUEUE) > 0) {
                label(graphics, "queue", 4, 44);
            }
        }
        if (menu.upgrades(unit, Upgrade.CHARGED) > 0) {
            graphics.drawString(font, Component.translatable("gui.minions_workshop.charges", menu.charges(unit)),
                leftPos + 102, topPos + 98, 0xFF28652E, false);
        }
    }

    private void drawArrow(GuiGraphics graphics, int x, int y, int progress) {
        texture(graphics, FURNACE, leftPos + x, topPos + y, 79, 34, 24, 16);
        if (progress > 0) {
            //? if >=26.1 {
            /*graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0,
                leftPos + x, topPos + y, Math.min(24, progress), 16);
            *///? } else {
            graphics.blitSprite(ARROW, 24, 16, 0, 0, leftPos + x, topPos + y, Math.min(24, progress), 16);
            //? }
        }
    }

    private void drawFuel(GuiGraphics graphics) {
        texture(graphics, FURNACE, leftPos + 31, topPos + 129, 56, 36, 14, 14);
        if (menu.fuel() > 0) {
            sprite(graphics, FLAME, leftPos + 31, topPos + 129, 14, 14);
        }
        label(graphics, "fuel.shared", 49, 128);
        graphics.fill(leftPos + 48, topPos + 138, leftPos + 168, topPos + 145, SHADOW);
        graphics.fill(leftPos + 49, topPos + 139, leftPos + 167, topPos + 144, 0xFF8B8B8B);
        int filled = menu.fuel() * 118 / WorkshopRules.MAX_FUEL;
        graphics.fill(leftPos + 49, topPos + 139, leftPos + 49 + filled, topPos + 144, 0xFFF3AD3B);
        graphics.fill(leftPos + 49, topPos + 144, leftPos + 168, topPos + 145, 0xFFFFFFFF);
    }

    private void drawExtraTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int unit = 0; unit < WorkshopLayout.UNIT_COUNT; unit++) {
            int x = WorkshopMenu.MODULE_TAB_X + unit * WorkshopMenu.MODULE_TAB_STEP;
            if (menu.moduleItem(unit).isEmpty() && inside(mouseX, mouseY, x, WorkshopMenu.MODULE_TAB_Y, 28, 24)) {
                tooltip(graphics, List.of(Component.translatable("gui.minions_workshop.unit", unit + 1), text("module.install")),
                    mouseX, mouseY);
                return;
            }
        }
        if (inside(mouseX, mouseY, 31, 128, 137, 18)) {
            tooltip(graphics, List.of(text("fuel.shared"),
                Component.translatable("gui.minions_workshop.fuel.amount", menu.fuel(), WorkshopRules.MAX_FUEL),
                text("fuel.help")), mouseX, mouseY);
        }
        if (menu.page() == 2 && sidebar.fullyOpen(2)) {
            WorkshopSidebar.Bounds bounds = pane(2);
            if (mouseX >= bounds.x() + 8 && mouseX < bounds.x() + 104) {
                if (mouseY >= bounds.y() + 27 && mouseY < bounds.y() + 37) {
                    tooltip(graphics, List.of(text(menu.module(menu.selectedUnit()) == Module.EMPTY
                        ? "module.upgrades.empty" : "module.upgrades.help")), mouseX, mouseY);
                } else if (mouseY >= bounds.y() + 82 && mouseY < bounds.y() + 92) {
                    tooltip(graphics, List.of(text("global.help")), mouseX, mouseY);
                }
            }
        }
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
            && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private void tooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
        //? if >=26.1 {
        /*graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
        *///? } else {
        graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        //? }
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, text(key), leftPos + x, topPos + y, TEXT, false);
    }

    private void panelLabel(GuiGraphics graphics, String key, int x, int y, int width) {
        graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(text(key), width)),
            x, y, 0xFFFFFFFF, true);
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int color, boolean pressed) {
        graphics.fill(x + 1, y, x + width - 1, y + height, 0xFF202020);
        graphics.fill(x, y + 1, x + width, y + height - 1, 0xFF202020);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, pressed ? SHADOW : 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, pressed ? SHADOW : 0xFFFFFFFF);
        graphics.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, pressed ? 0xFFFFFFFF : SHADOW);
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 1, pressed ? 0xFFFFFFFF : SHADOW);
    }

    private static void slotWell(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF8B8B8B);
        graphics.fill(x + 1, y + size - 1, x + size, y + size, 0xFFFFFFFF);
        graphics.fill(x + size - 1, y + 1, x + size, y + size, 0xFFFFFFFF);
    }

    private static void sprite(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        //? if >=26.1 {
        /*graphics.blitSprite(RenderPipelines.GUI_TEXTURED, texture, x, y, width, height);
        *///? } else {
        graphics.blitSprite(texture, x, y, width, height);
        //? }
    }

    private static void texture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        //? if >=26.1 {
        /*graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 256, 256);
        *///? } else {
        graphics.blit(texture, x, y, u, v, width, height);
        //? }
    }

    private static int modeColor(FaceMode mode) {
        return switch (mode) {
            case NONE -> 0xFF8B8B8B;
            case INPUT -> 0xFF3A6FD8;
            case OUTPUT -> 0xFFE08A1E;
            case BOTH -> 0xFF8E44AD;
        };
    }

    private final class IconTab extends Button {
        private final int page;

        IconTab(int page) {
            super(0, 0, 24, 24, text(page == 2 ? "upgrades" : page == 1 ? "transfer" : page == 3 ? "fuel.transfer" : "filters"),
                ignored -> send(menu.page() == page ? 0 : page), DEFAULT_NARRATION);
            this.page = page;
            setTooltip(Tooltip.create(getMessage()));
        }

        //? if >=26.1 {
        /*@Override
        protected void extractContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            draw(graphics);
        }
        *///? } else {
        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            draw(graphics);
        }
        //? }

        private void draw(GuiGraphics graphics) {
            if (isHoveredOrFocused()) {
                graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + 22, 0x28FFFFFF);
            }
            ItemStack icon = page == 2 ? new ItemStack(Workshop.UPGRADES.get(Upgrade.BLANK))
                : page == 4 ? new ItemStack(Workshop.UPGRADES.get(Upgrade.FILTER))
                : new ItemStack(page == 1 ? Items.HOPPER : Items.COAL);
            graphics.renderItem(icon, getX() + 4, getY() + 4);
            if (width > 30) {
                graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(getMessage(), width - 30)),
                    getX() + 24, getY() + 8, 0xFFFFFFFF, true);
            }
        }
    }

    private class ChoiceButton extends Button {
        private final BooleanSupplier selected;

        ChoiceButton(int id, int width, int height, Component label, Component tooltip, BooleanSupplier selected) {
            super(0, 0, width, height, label, ignored -> send(id), DEFAULT_NARRATION);
            this.selected = selected;
            setTooltip(Tooltip.create(tooltip));
        }

        //? if >=26.1 {
        /*@Override
        protected void extractContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            draw(graphics);
        }
        *///? } else {
        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            draw(graphics);
        }
        //? }

        private void draw(GuiGraphics graphics) {
            boolean pressed = selected.getAsBoolean();
            panel(graphics, getX(), getY(), width, height, isHoveredOrFocused() || pressed ? SURFACE : 0xFFA5A5A5, pressed);
            FormattedCharSequence label = Language.getInstance().getVisualOrder(font.substrByWidth(getMessage(), width - 6));
            graphics.drawString(font, label, getX() + (width - font.width(label)) / 2,
                getY() + (height - font.lineHeight) / 2, TEXT, false);
        }
    }

    private final class FilterFaceButton extends ChoiceButton {
        FilterFaceButton() {
            super(7, 62, 16, text("side.north"), text("filter.face.help"), () -> false);
        }

        //? if >=26.1 {
        /*@Override
        protected boolean isValidClickButton(MouseButtonInfo button) {
            return button.button() == InputConstants.MOUSE_BUTTON_LEFT || button.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            send(event.button() == InputConstants.MOUSE_BUTTON_RIGHT ? 8 : 7);
        }
        *///? } else {
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || !isMouseOver(mouseX, mouseY) || button < 0 || button > 1) {
                return false;
            }
            playDownSound(minecraft.getSoundManager());
            send(button == InputConstants.MOUSE_BUTTON_RIGHT ? 8 : 7);
            return true;
        }
        //? }
    }

    private final class FaceButton extends Button {
        private final Face face;

        FaceButton(Face face) {
            super(0, 0, 18, 18, Component.empty(),
                ignored -> send(100 + face.direction(menu.front()).ordinal()), DEFAULT_NARRATION);
            this.face = face;
        }

        private Component label() {
            int side = face.direction(menu.front()).ordinal();
            return Component.translatable("gui.minions_workshop.face",
                text("face." + face.name().toLowerCase(Locale.ROOT)),
                text("side." + Direction.values()[side].getSerializedName()),
                text("face.mode." + menu.faceMode(side).getSerializedName()));
        }

        private void click(int button, boolean all) {
            int side = face.direction(menu.front()).ordinal();
            send(button == InputConstants.MOUSE_BUTTON_MIDDLE ? 20 + side
                : 100 + side + (button == InputConstants.MOUSE_BUTTON_RIGHT ? 6 : 0) + (all ? 12 : 0));
        }

        //? if >=26.1 {
        /*@Override
        protected boolean isValidClickButton(MouseButtonInfo button) {
            return button.button() == InputConstants.MOUSE_BUTTON_LEFT
                || button.button() == InputConstants.MOUSE_BUTTON_RIGHT || button.button() == InputConstants.MOUSE_BUTTON_MIDDLE;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            click(event.button(), event.hasShiftDown());
        }

        @Override
        protected void extractContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            draw(graphics);
        }
        *///? } else {
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || !isMouseOver(mouseX, mouseY) || button < 0 || button > 2) {
                return false;
            }
            playDownSound(minecraft.getSoundManager());
            click(button, hasShiftDown());
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            draw(graphics);
        }
        //? }

        private void draw(GuiGraphics graphics) {
            int side = face.direction(menu.front()).ordinal();
            FaceMode mode = menu.faceMode(side);
            panel(graphics, getX(), getY(), width, height, modeColor(mode), mode != FaceMode.NONE);
            boolean furnace = menu.module(menu.selectedUnit()) == Module.SMELTING || menu.page() == 3;
            graphics.renderItem(new ItemStack(furnace ? Items.FURNACE : Items.CRAFTING_TABLE), getX() + 1, getY() + 1);
            if (menu.side() == side) {
                graphics.fill(getX() + 2, getY() + 15, getX() + 16, getY() + 16, 0xFFFFFFFF);
            }
            if (isHoveredOrFocused()) {
                graphics.fill(getX() + 1, getY() + 1, getX() + 17, getY() + 17, 0x40FFFFFF);
            }
        }
    }

    private final class AutoButton extends Button {
        private final int flow;

        AutoButton(int flow) {
            super(0, 0, 18, 18, Component.empty(), ignored -> send(43 + flow), DEFAULT_NARRATION);
            this.flow = flow;
        }

        //? if >=26.1 {
        /*@Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            send(43 + flow + (event.hasShiftDown() ? 2 : 0));
        }

        @Override
        protected void extractContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            draw(graphics);
        }
        *///? } else {
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || !isMouseOver(mouseX, mouseY) || button != InputConstants.MOUSE_BUTTON_LEFT) {
                return false;
            }
            playDownSound(minecraft.getSoundManager());
            send(43 + flow + (hasShiftDown() ? 2 : 0));
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            draw(graphics);
        }
        //? }

        private void draw(GuiGraphics graphics) {
            boolean automatic = menu.faceAutomatic(menu.side(), flow);
            panel(graphics, getX(), getY(), width, height, active && automatic ? 0xFF3FA34D : 0xFF8B8B8B, automatic);
            ResourceLocation icon = Workshop.id("textures/gui/icons/auto_" + (flow == 0 ? "pull" : "push") + ".png");
            //? if >=26.1 {
            /*graphics.blit(RenderPipelines.GUI_TEXTURED, icon, getX() + 1, getY() + 1, 0, 0, 16, 16, 16, 16);
            *///? } else {
            graphics.blit(icon, getX() + 1, getY() + 1, 0, 0, 16, 16, 16, 16);
            //? }
            if (active && isHoveredOrFocused()) {
                graphics.fill(getX() + 1, getY() + 1, getX() + 17, getY() + 17, 0x40FFFFFF);
            }
        }
    }

    private final class GhostButton extends Button {
        private final int flow;
        private final int filter;

        GhostButton(int flow, int filter) {
            super(0, 0, 18, 18, Component.empty(), ignored -> send((flow == 0 ? 50 : 80) + filter), DEFAULT_NARRATION);
            this.flow = flow;
            this.filter = filter;
        }

        private void click(boolean cycle) {
            send((flow == 0 ? 50 : 80) + (cycle ? 10 : 0) + filter);
        }

        //? if >=26.1 {
        /*@Override
        protected boolean isValidClickButton(MouseButtonInfo button) {
            return button.button() == InputConstants.MOUSE_BUTTON_LEFT || button.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            click(event.button() == InputConstants.MOUSE_BUTTON_RIGHT);
        }

        @Override
        protected void extractContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            draw(graphics);
        }
        *///? } else {
        @Override
        protected boolean isValidClickButton(int button) {
            return button == 0 || button == 1;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || !isMouseOver(mouseX, mouseY) || button < 0 || button > 1) {
                return false;
            }
            playDownSound(minecraft.getSoundManager());
            click(button == InputConstants.MOUSE_BUTTON_RIGHT);
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            draw(graphics);
        }
        //? }

        private void draw(GuiGraphics graphics) {
            slotWell(graphics, getX(), getY(), 18);
            graphics.renderItem(menu.filter(flow, filter), getX() + 1, getY() + 1);
            graphics.drawString(font, text("mode." + menu.mode(flow, filter).getSerializedName() + ".short"),
                getX() + 11, getY() + 9, 0xFFFFFFFF, true);
            if (isHoveredOrFocused()) {
                graphics.fill(getX() + 1, getY() + 1, getX() + 17, getY() + 17, 0x80FFFFFF);
            }
        }
    }
}
