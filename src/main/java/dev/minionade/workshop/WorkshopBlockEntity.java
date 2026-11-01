package dev.minionade.workshop;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
/*import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
*///? }
//? if >=26.3 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
*///? }

public final class WorkshopBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private static final Codec<List<TransferRule>> RULES_CODEC = TransferRule.CODEC.listOf(0, WorkshopLayout.RULE_COUNT);
    private final NonNullList<ItemStack> items = NonNullList.withSize(WorkshopLayout.SIZE, ItemStack.EMPTY);
    private final TransferRule[] rules = new TransferRule[WorkshopLayout.RULE_COUNT];
    private final int[] progress = new int[8];
    private final int[] charges = new int[8];
    private final float[] experience = new float[4];
    private int fuel;
    private int ticks;

    public WorkshopBlockEntity(BlockPos pos, BlockState state) {
        super(Workshop.TABLE_ENTITY, pos, state);
        Arrays.setAll(rules, index -> new TransferRule());
    }

    public Module module(int unit) {
        return Module.of(getItem(WorkshopLayout.parent(unit)));
    }

    public int upgrades(int section, Upgrade upgrade) {
        if (section == 4 ? !upgrade.global() : !upgrade.supports(module(section))) {
            return 0;
        }
        int start = section == 4 ? WorkshopLayout.GLOBAL : WorkshopLayout.parent(section) + 1;
        int end = section == 4 ? start + 8 : start + 7;
        int count = 0;
        for (int slot = start; slot < end; slot++) {
            if (Upgrade.of(getItem(slot)) == upgrade) {
                count += getItem(slot).getCount();
            }
        }
        return Math.min(count, upgrade.maximum());
    }

    public TransferRule rule(int group, int side, int direction) {
        return rules[WorkshopLayout.rule(group, side, direction)];
    }

    public int fuel() {
        return fuel;
    }

    private int machine(int unit) {
        return unit * 2 + (module(unit) == Module.SMELTING ? 1 : 0);
    }

    public int progress(int unit) {
        return progress[machine(unit)];
    }

    public int charges(int unit) {
        return charges[machine(unit)];
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WorkshopBlockEntity table) {
        table.tick((ServerLevel) level);
    }

    private void tick(ServerLevel server) {
        boolean changed = refillFuel(server);
        boolean working = false;
        for (int unit = 0; unit < WorkshopLayout.UNIT_COUNT; unit++) {
            if (module(unit) == Module.SMELTING) {
                changed |= moveQueue(unit);
            }
            if (module(unit) == Module.SMELTING || module(unit) == Module.CRAFTING && upgrades(unit, Upgrade.AUTO_CRAFTER) > 0) {
                boolean updated = produce(unit, server);
                working |= updated;
                changed |= updated;
            }
        }
        if (++ticks % 20 == 0 && upgrades(4, Upgrade.AUTO_TRANSFER) > 0) {
            autoTransfer(server);
        }
        if (getBlockState().getValue(BlockStateProperties.LIT) != working) {
            server.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, working), 3);
        }
        if (changed) {
            setChanged();
        }
    }

    public int burnTime(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return 0;
        }
        //? if >=26.3 {
        /*if (!(level instanceof ServerLevel server)) {
            return stack.has(DataComponents.COOKING_FUEL) ? 1 : 0;
        }
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime,
            getLootContext(server, stack), 0);
        *///? } else if >=26.1 && neoforge {
        /*return stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
        *///? } else if >=26.1 {
        /*return level.fuelValues().burnDuration(stack);
        *///? } else if neoforge {
        /*return stack.getBurnTime(RecipeType.SMELTING);
        *///? } else {
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
        //? }
    }

    //? if >=26.3 {
    /*private LootContext getLootContext(ServerLevel server, ItemStack stack) {
        LootParams.Builder builder = new LootParams.Builder(server)
            .withParameter(LootContextParams.BLOCK_STATE, getBlockState())
            .withParameter(LootContextParams.BLOCK_ENTITY, this)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
            .withParameter(LootContextParams.CONTAINER, this);
        //? if neoforge {
        builder.withOptionalParameter(net.neoforged.neoforge.common.loot.NeoForgeLootContextParams.QUERIED_STACK, stack);
        //? }
        return new LootContext.Builder(builder.create(LootContextParamSets.CONTAINER_PROCESS)).create(Optional.empty());
    }
    *///? }

    private boolean refillFuel(ServerLevel server) {
        ItemStack stack = getItem(WorkshopLayout.FUEL);
        int duration = burnTime(stack);
        if (duration <= 0 || duration > WorkshopRules.MAX_FUEL - fuel) {
            return false;
        }
        ItemStack remainder = remainder(stack);
        removeItem(WorkshopLayout.FUEL, 1);
        if (!remainder.isEmpty()) {
            if (getItem(WorkshopLayout.FUEL).isEmpty()) {
                setItem(WorkshopLayout.FUEL, remainder);
            } else {
                Containers.dropItemStack(server, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
            }
        }
        fuel += duration;
        return true;
    }

    private static ItemStack remainder(ItemStack stack) {
        //? if >=26.1 {
        /*//? if neoforge {
        var remainder = stack.getCraftingRemainder();
        //? } else {
        var remainder = stack.getItem().getCraftingRemainder();
        //? }
        return remainder == null ? ItemStack.EMPTY : remainder.create();
        *///? } else if neoforge {
        /*return stack.getCraftingRemainingItem();
        *///? } else {
        var remainder = stack.getItem().getCraftingRemainingItem();
        return remainder == null ? ItemStack.EMPTY : new ItemStack(remainder);
        //? }
    }

    private boolean moveQueue(int unit) {
        int count = upgrades(unit, Upgrade.QUEUE);
        boolean changed = false;
        for (int queue = 0; queue < count; queue++) {
            int from = WorkshopLayout.slot(unit, WorkshopLayout.QUEUE + queue);
            int to = WorkshopLayout.slot(unit, queue == 0 ? WorkshopLayout.SMELT_INPUT : WorkshopLayout.QUEUE + queue - 1);
            ItemStack source = getItem(from);
            if (!source.isEmpty() && fits(source.copyWithCount(1), getItem(to))) {
                insert(to, source.split(1));
                changed = true;
            }
        }
        return changed;
    }

    public Optional<Craft> crafting(int unit) {
        if (!(level instanceof ServerLevel server) || module(unit) != Module.CRAFTING) {
            return Optional.empty();
        }
        List<ItemStack> grid = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            grid.add(getItem(WorkshopLayout.slot(unit, slot)));
        }
        CraftingInput.Positioned positioned = CraftingInput.ofPositioned(3, 3, grid);
        return server.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, positioned.input(), server)
            .map(holder -> new Craft(holder, positioned, assemble(holder, positioned.input())));
    }

    private ItemStack assemble(RecipeHolder<CraftingRecipe> holder, CraftingInput input) {
        //? if >=26.1 {
        /*return holder.value().assemble(input);
        *///? } else {
        return holder.value().assemble(input, level.registryAccess());
        //? }
    }

    private Optional<RecipeHolder<SmeltingRecipe>> smelting(int unit, ServerLevel server) {
        return server.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,
            new SingleRecipeInput(getItem(WorkshopLayout.slot(unit, WorkshopLayout.SMELT_INPUT))), server);
    }

    private ItemStack smeltResult(RecipeHolder<SmeltingRecipe> holder, int unit, ServerLevel server) {
        SingleRecipeInput input = new SingleRecipeInput(getItem(WorkshopLayout.slot(unit, WorkshopLayout.SMELT_INPUT)));
        //? if >=26.1 {
        /*return holder.value().assemble(input);
        *///? } else {
        return holder.value().assemble(input, server.registryAccess());
        //? }
    }

    private boolean produce(int unit, ServerLevel server) {
        int machine = machine(unit);
        int speed = upgrades(unit, Upgrade.SPEED);
        int maxCharges = WorkshopRules.maxCharges(upgrades(unit, Upgrade.CHARGED));
        charges[machine] = Math.min(charges[machine], maxCharges);
        boolean smelting = module(unit) == Module.SMELTING;
        ItemStack result = productionResult(unit, server);
        int output = WorkshopLayout.slot(unit, smelting ? WorkshopLayout.SMELT_OUTPUT : WorkshopLayout.CRAFT_OUTPUT);
        boolean changed = false;
        while (charges[machine] > 0 && !result.isEmpty() && fits(result, getItem(output))) {
            finishProduction(unit, result, server);
            charges[machine]--;
            changed = true;
            result = productionResult(unit, server);
        }
        boolean charging = result.isEmpty() || !fits(result, getItem(output));
        if (charging && charges[machine] >= maxCharges) {
            if (result.isEmpty() && progress[machine] != 0) {
                progress[machine] = 0;
                return true;
            }
            return changed;
        }
        int consumption = WorkshopRules.consumption(speed, charging, smelting);
        if (fuel < consumption) {
            return changed;
        }
        fuel -= consumption;
        progress[machine] += WorkshopRules.speed(speed, charging);
        if (progress[machine] >= WorkshopRules.PRODUCTION_TIME) {
            progress[machine] -= WorkshopRules.PRODUCTION_TIME;
            if (charging) {
                charges[machine]++;
            } else {
                finishProduction(unit, result, server);
            }
        }
        return true;
    }

    private ItemStack productionResult(int unit, ServerLevel server) {
        if (module(unit) == Module.SMELTING) {
            return smelting(unit, server).map(holder -> smeltResult(holder, unit, server)).orElse(ItemStack.EMPTY);
        }
        return crafting(unit).filter(craft -> consumptionPlan(unit, true) != null).map(Craft::result).orElse(ItemStack.EMPTY);
    }

    private void finishProduction(int unit, ItemStack result, ServerLevel server) {
        if (module(unit) == Module.SMELTING) {
            smelting(unit, server).ifPresent(holder -> {
                //? if >=26.1 {
                /*experience[unit] += holder.value().experience();
                *///? } else {
                experience[unit] += holder.value().getExperience();
                //? }
            });
            removeItem(WorkshopLayout.slot(unit, WorkshopLayout.SMELT_INPUT), 1);
            insert(WorkshopLayout.slot(unit, WorkshopLayout.SMELT_OUTPUT), result.copy());
        } else {
            ItemStack crafted = craft(unit, null, true);
            if (!crafted.isEmpty()) {
                insert(WorkshopLayout.slot(unit, WorkshopLayout.CRAFT_OUTPUT), crafted);
            }
        }
    }

    private int[] consumptionPlan(int unit, boolean preservePattern) {
        List<ItemStack> ingredients = new ArrayList<>(15);
        for (int slot = 0; slot < 15; slot++) {
            ingredients.add(getItem(WorkshopLayout.slot(unit, slot)));
        }
        return CraftingPlan.create(ingredients, upgrades(unit, Upgrade.STORAGE) > 0, preservePattern);
    }

    public ItemStack craft(int unit, Player player, boolean automatic) {
        Optional<Craft> match = crafting(unit);
        int[] plan = consumptionPlan(unit, automatic);
        if (match.isEmpty() || plan == null) {
            return ItemStack.EMPTY;
        }
        Craft craft = match.get();
        List<ItemStack> remainders = craft.recipe().value().getRemainingItems(craft.input().input());
        for (int slot = 0; slot < plan.length; slot++) {
            if (plan[slot] > 0) {
                removeItem(WorkshopLayout.slot(unit, slot), plan[slot]);
            }
        }
        for (int index = 0; index < remainders.size(); index++) {
            ItemStack remainder = remainders.get(index);
            if (remainder.isEmpty()) {
                continue;
            }
            int x = index % craft.input().input().width() + craft.input().left();
            int y = index / craft.input().input().width() + craft.input().top();
            int slot = WorkshopLayout.slot(unit, x + y * 3);
            if (fits(remainder, getItem(slot))) {
                insert(slot, remainder);
            } else if (!storeRemainder(unit, remainder.copy())) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
            }
        }
        ItemStack result = craft.result().copy();
        if (player != null) {
            //? if >=26.1 {
            /*result.onCraftedBy(player, result.getCount());
            *///? } else {
            result.onCraftedBy(level, player, result.getCount());
            //? }
            player.awardRecipes(List.of(craft.recipe()));
        } else {
            result.onCraftedBySystem(level);
        }
        setChanged();
        return result;
    }

    private boolean storeRemainder(int unit, ItemStack remainder) {
        if (upgrades(unit, Upgrade.STORAGE) == 0) {
            return false;
        }
        for (int index = 9; index < 15; index++) {
            int slot = WorkshopLayout.slot(unit, index);
            if (fits(remainder, getItem(slot))) {
                insert(slot, remainder);
                return true;
            }
        }
        return false;
    }

    public void clearGrid(int unit, Player player) {
        if (module(unit) != Module.CRAFTING) {
            return;
        }
        for (int grid = 0; grid < 9; grid++) {
            //? if >=26.3 {
            /*player.getInventory().placeItemBackInInventory(removeItemNoUpdate(WorkshopLayout.slot(unit, grid)), net.minecraft.util.Prediction.SERVER_ONLY);
            *///? } else {
            player.getInventory().placeItemBackInInventory(removeItemNoUpdate(WorkshopLayout.slot(unit, grid)));
            //? }
        }
        setChanged();
    }

    public void awardExperience(int unit, Player player) {
        if (level instanceof ServerLevel server) {
            float total = experience[unit];
            int amount = (int) total;
            if (server.getRandom().nextFloat() < total - amount) {
                amount++;
            }
            ExperienceOrb.award(server, player.position(), amount);
            experience[unit] = 0;
            setChanged();
        }
    }

    private static boolean fits(ItemStack source, ItemStack target) {
        return !source.isEmpty() && (target.isEmpty()
            ? source.getCount() <= source.getMaxStackSize()
            : ItemStack.isSameItemSameComponents(source, target)
                && target.getCount() + source.getCount() <= target.getMaxStackSize());
    }

    private void insert(int slot, ItemStack stack) {
        if (getItem(slot).isEmpty()) {
            setItem(slot, stack);
        } else {
            getItem(slot).grow(stack.getCount());
            setChanged();
        }
    }

    public boolean activeSlot(int slot) {
        if (slot == WorkshopLayout.FUEL || slot >= WorkshopLayout.UPGRADES) {
            return true;
        }
        int unit = (slot - 1) / WorkshopLayout.UNIT_SIZE;
        int offset = (slot - 1) % WorkshopLayout.UNIT_SIZE;
        return switch (module(unit)) {
            case CRAFTING -> offset < 9 || offset < 15 && upgrades(unit, Upgrade.STORAGE) > 0
                || offset == WorkshopLayout.CRAFT_OUTPUT && upgrades(unit, Upgrade.AUTO_CRAFTER) > 0;
            case SMELTING -> offset == WorkshopLayout.SMELT_INPUT || offset == WorkshopLayout.SMELT_OUTPUT
                || offset >= WorkshopLayout.QUEUE && offset < WorkshopLayout.QUEUE + upgrades(unit, Upgrade.QUEUE);
            case STORAGE -> offset >= WorkshopLayout.STORAGE;
            case EMPTY -> false;
        };
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == WorkshopLayout.FUEL) {
            return burnTime(stack) > 0;
        }
        if (slot >= WorkshopLayout.UPGRADES) {
            if (slot < WorkshopLayout.GLOBAL && (slot - WorkshopLayout.UPGRADES) % 8 == 0) {
                return Module.of(stack) != Module.EMPTY;
            }
            Upgrade upgrade = Upgrade.of(stack);
            int section = slot >= WorkshopLayout.GLOBAL ? 4 : (slot - WorkshopLayout.UPGRADES) / 8;
            return upgrade != null && (section == 4 ? upgrade.global() : upgrade.supports(module(section)));
        }
        if (!activeSlot(slot)) {
            return false;
        }
        int offset = (slot - 1) % WorkshopLayout.UNIT_SIZE;
        return offset != WorkshopLayout.CRAFT_OUTPUT && offset != WorkshopLayout.SMELT_OUTPUT;
    }

    private boolean canOutput(int slot, ItemStack stack) {
        if (slot == WorkshopLayout.FUEL) {
            return burnTime(stack) == 0;
        }
        if (slot >= WorkshopLayout.UPGRADES || !activeSlot(slot)) {
            return false;
        }
        int unit = (slot - 1) / WorkshopLayout.UNIT_SIZE;
        int offset = (slot - 1) % WorkshopLayout.UNIT_SIZE;
        return module(unit) == Module.STORAGE || offset == WorkshopLayout.SMELT_OUTPUT
            || offset == WorkshopLayout.CRAFT_OUTPUT || module(unit) == Module.CRAFTING && offset >= 9 && offset < 15;
    }

    private int group(int slot) {
        return slot == WorkshopLayout.FUEL ? 4 : (slot - 1) / WorkshopLayout.UNIT_SIZE;
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        // Transfer adapters cache their slot mapping; permissions remain dynamic per operation.
        return IntStream.range(0, WorkshopLayout.UPGRADES).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        if (slot < 0 || slot >= WorkshopLayout.UPGRADES || !canPlaceItem(slot, stack)) {
            return false;
        }
        TransferRule rule = rule(group(slot), face.ordinal(), 0);
        if (!rule.has(TransferRule.ENABLED) || !rule.accepts(stack, upgrades(4, Upgrade.FILTER) > 0)) {
            return false;
        }
        if (slot != WorkshopLayout.FUEL && (slot - 1) % WorkshopLayout.UNIT_SIZE < 9) {
            return ItemStack.isSameItemSameComponents(stack, getItem(slot));
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        if (slot < 0 || slot >= WorkshopLayout.UPGRADES || !canOutput(slot, stack)) {
            return false;
        }
        TransferRule rule = rule(group(slot), face.ordinal(), 1);
        return rule.has(TransferRule.ENABLED) && rule.accepts(stack, upgrades(4, Upgrade.FILTER) > 0);
    }

    private void autoTransfer(ServerLevel server) {
        int capacity = WorkshopRules.transferSize(upgrades(4, Upgrade.TRANSFER));
        for (Direction face : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(face);
            if (!server.hasChunkAt(adjacentPos)) {
                continue;
            }
            Container adjacent = net.minecraft.world.level.block.entity.HopperBlockEntity.getContainerAt(server, adjacentPos);
            if (adjacent == null || adjacent == this) {
                continue;
            }
            int[] adjacentSlots = adjacent instanceof WorldlyContainer sided
                ? sided.getSlotsForFace(face.getOpposite()) : IntStream.range(0, adjacent.getContainerSize()).toArray();
            for (int group = 0; group < 5; group++) {
                int currentGroup = group;
                int[] ownSlots = Arrays.stream(getSlotsForFace(face)).filter(slot -> group(slot) == currentGroup).toArray();
                if (rule(group, face.ordinal(), 0).has(TransferRule.ENABLED) && rule(group, face.ordinal(), 0).has(TransferRule.AUTO)) {
                    move(adjacent, this, adjacentSlots, ownSlots, face.getOpposite(), face, capacity);
                }
                if (rule(group, face.ordinal(), 1).has(TransferRule.ENABLED) && rule(group, face.ordinal(), 1).has(TransferRule.AUTO)) {
                    move(this, adjacent, ownSlots, adjacentSlots, face, face.getOpposite(), capacity);
                }
            }
        }
    }

    private static void move(Container source, Container target, int[] sourceSlots, int[] targetSlots,
                             Direction sourceFace, Direction targetFace, int capacity) {
        int remaining = capacity;
        for (int from : sourceSlots) {
            ItemStack stack = source.getItem(from);
            if (stack.isEmpty() || source instanceof WorldlyContainer sided && !sided.canTakeItemThroughFace(from, stack, sourceFace)) {
                continue;
            }
            for (int pass = 0; pass < 2; pass++) {
                for (int to : targetSlots) {
                    ItemStack existing = target.getItem(to);
                    if ((pass == 0) == existing.isEmpty() || !target.canPlaceItem(to, stack)
                        || target instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(to, stack, targetFace)
                        || !existing.isEmpty() && !ItemStack.isSameItemSameComponents(stack, existing)) {
                        continue;
                    }
                    //? if >=26.1 {
                    /*int limit = Math.min(stack.getMaxStackSize(), target.getMaxStackSize(stack));
                    *///? } else {
                    int limit = Math.min(stack.getMaxStackSize(), target.getMaxStackSize());
                    //? }
                    int count = Math.min(remaining, Math.min(stack.getCount(), limit - existing.getCount()));
                    if (count <= 0) {
                        continue;
                    }
                    ItemStack moved = source.removeItem(from, count);
                    if (existing.isEmpty()) {
                        target.setItem(to, moved);
                    } else {
                        existing.grow(moved.getCount());
                    }
                    source.setChanged();
                    target.setChanged();
                    remaining -= moved.getCount();
                    if (remaining == 0) {
                        return;
                    }
                    stack = source.getItem(from);
                    if (stack.isEmpty()) {
                        break;
                    }
                }
                if (stack.isEmpty()) {
                    break;
                }
            }
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, count);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.minions_workshop.table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WorkshopMenu(id, inventory, this);
    }

    public void dropContents() {
        if (level instanceof ServerLevel server) {
            Containers.dropContents(server, worldPosition, this);
            for (float xp : experience) {
                ExperienceOrb.award(server, Vec3.atCenterOf(worldPosition), (int) xp);
            }
            clearContent();
            Arrays.fill(experience, 0);
        }
    }

    //? if >=26.1 {
    /*@Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropContents();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("Fuel", fuel);
        output.putIntArray("Progress", progress.clone());
        output.putIntArray("Charges", charges.clone());
        output.store("TransferRules", RULES_CODEC, Arrays.asList(rules));
        for (int unit = 0; unit < 4; unit++) {
            output.putFloat("Experience" + unit, experience[unit]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        fuel = Math.clamp(input.getIntOr("Fuel", 0), 0, WorkshopRules.MAX_FUEL);
        restoreArray(progress, input.getIntArray("Progress").orElse(new int[0]), WorkshopRules.PRODUCTION_TIME - 1);
        restoreArray(charges, input.getIntArray("Charges").orElse(new int[0]), WorkshopRules.maxCharges(8));
        restoreRules(input.read("TransferRules", RULES_CODEC).orElse(List.of()));
        for (int unit = 0; unit < 4; unit++) {
            experience[unit] = restoreExperience(unit, input.getFloatOr("Experience" + unit, 0));
        }
    }
    *///? } else {
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Fuel", fuel);
        tag.putIntArray("Progress", progress.clone());
        tag.putIntArray("Charges", charges.clone());
        tag.put("TransferRules", RULES_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), Arrays.asList(rules)).getOrThrow());
        for (int unit = 0; unit < 4; unit++) {
            tag.putFloat("Experience" + unit, experience[unit]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        fuel = Math.clamp(tag.getInt("Fuel"), 0, WorkshopRules.MAX_FUEL);
        restoreArray(progress, tag.getIntArray("Progress"), WorkshopRules.PRODUCTION_TIME - 1);
        restoreArray(charges, tag.getIntArray("Charges"), WorkshopRules.maxCharges(8));
        restoreRules(tag.contains("TransferRules")
            ? RULES_CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag.get("TransferRules"))
                .resultOrPartial(error -> Workshop.LOGGER.warn("Invalid workshop transfer rules at {}: {}", worldPosition, error)).orElse(List.of())
            : List.of());
        for (int unit = 0; unit < 4; unit++) {
            experience[unit] = restoreExperience(unit, tag.getFloat("Experience" + unit));
        }
    }
    //? }

    private static void restoreArray(int[] destination, int[] source, int maximum) {
        int count = Math.min(source.length, destination.length);
        for (int index = 0; index < count; index++) {
            destination[index] = Math.clamp(source[index], 0, maximum);
        }
        Arrays.fill(destination, count, destination.length, 0);
    }

    private void restoreRules(List<TransferRule> saved) {
        Arrays.setAll(rules, index -> index < saved.size() ? saved.get(index) : new TransferRule());
    }

    private float restoreExperience(int unit, float saved) {
        if (!Float.isFinite(saved)) {
            Workshop.LOGGER.warn("Invalid workshop experience for unit {} at {}: {}", unit, worldPosition, saved);
            return 0;
        }
        return Math.max(0, saved);
    }

    public record Craft(RecipeHolder<CraftingRecipe> recipe, CraftingInput.Positioned input, ItemStack result) {}
}
