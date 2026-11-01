package dev.minionade.workshop;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldDataConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
//? if >=26.1 {
/*import net.minecraft.server.permissions.LevelBasedPermissionSet;
*///? }

class CraftingAndTransferTest {
    static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        if (registries != null) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        PackRepository packs = ServerPacksSource.createVanillaTrustedRepository();
        packs.reload();
        WorldLoader.InitConfig config = new WorldLoader.InitConfig(
            new WorldLoader.PackConfig(packs, WorldDataConfiguration.DEFAULT, false, true),
            Commands.CommandSelection.DEDICATED,
            //? if >=26.1 {
            /*LevelBasedPermissionSet.OWNER
            *///? } else {
            2
            //? }
        );
        registries = WorldLoader.load(config, context -> new WorldLoader.DataLoadOutput<>(true, RegistryAccess.EMPTY),
            (resources, managers, layers, loaded) -> {
                resources.close();
                return layers.compositeAccess();
            }, Runnable::run, Runnable::run).join();
    }

    private static List<ItemStack> grid() {
        return new ArrayList<>(Collections.nCopies(15, ItemStack.EMPTY));
    }

    @Test
    void automaticCraftingNeverConsumesTheLastPatternItem() {
        List<ItemStack> items = grid();
        items.set(0, new ItemStack(Items.OAK_PLANKS));
        assertNull(CraftingPlan.create(items, false, true));
        items.get(0).grow(1);
        int[] plan = CraftingPlan.create(items, false, true);
        assertEquals(1, plan[0]);
        assertEquals(1, Arrays.stream(plan).sum());
        assertEquals(2, items.get(0).getCount());
    }

    @Test
    void duplicateIngredientsReserveTheirCombinedStorageRequirement() {
        List<ItemStack> items = grid();
        for (int index = 0; index < 4; index++) {
            items.set(index, new ItemStack(Items.OAK_PLANKS));
        }
        items.set(9, new ItemStack(Items.OAK_PLANKS, 3));
        assertNull(CraftingPlan.create(items, true, true));
        assertEquals(3, items.get(9).getCount());
        items.get(9).grow(1);
        int[] plan = CraftingPlan.create(items, true, true);
        assertEquals(4, plan[9]);
        assertEquals(4, Arrays.stream(plan).sum());
        assertEquals(0, Arrays.stream(plan, 0, 9).sum());
    }

    @Test
    void manualCraftingCanUseTheLastGridItemsButNotDisabledStorage() {
        List<ItemStack> items = grid();
        items.set(0, new ItemStack(Items.OAK_PLANKS));
        items.set(1, new ItemStack(Items.OAK_PLANKS));
        items.set(9, new ItemStack(Items.OAK_PLANKS, 64));
        int[] plan = CraftingPlan.create(items, false, false);
        assertEquals(1, plan[0]);
        assertEquals(1, plan[1]);
        assertEquals(0, plan[9]);
    }

    @Test
    void storedIngredientsMustHaveMatchingComponents() {
        List<ItemStack> items = grid();
        ItemStack named = new ItemStack(Items.DIAMOND);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Pattern"));
        items.set(0, named);
        items.set(9, new ItemStack(Items.DIAMOND, 64));
        assertNull(CraftingPlan.create(items, true, true));
        items.get(9).set(DataComponents.CUSTOM_NAME, Component.literal("Pattern"));
        assertEquals(1, CraftingPlan.create(items, true, true)[9]);
    }

    @Test
    void ghostFiltersCopyItemsWithoutConsumingOrAliasingThem() {
        TransferRule rule = new TransferRule();
        ItemStack carried = new ItemStack(Items.DIAMOND, 32);
        rule.setFilter(0, carried);
        assertEquals(32, carried.getCount());
        assertEquals(1, rule.filter(0).item().getCount());
        carried.set(DataComponents.CUSTOM_NAME, Component.literal("Changed"));
        assertFalse(rule.filter(0).item().has(DataComponents.CUSTOM_NAME));
        rule.setFilter(0, ItemStack.EMPTY);
        assertTrue(rule.filter(0).item().isEmpty());
    }

    @Test
    void filtersRespectComponentModesAndWhitelistPolarity() {
        TransferRule rule = new TransferRule();
        rule.setFilter(0, new ItemStack(Items.DIAMOND));
        ItemStack named = new ItemStack(Items.DIAMOND);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));
        assertFalse(rule.accepts(new ItemStack(Items.DIAMOND), true));
        assertTrue(rule.accepts(named, true));
        rule.toggle(TransferRule.WHITELIST);
        assertTrue(rule.accepts(new ItemStack(Items.DIAMOND), true));
        assertFalse(rule.accepts(named, true));
        rule.cycleMode(0);
        assertTrue(rule.accepts(named, true));
        assertFalse(rule.accepts(new ItemStack(Items.EMERALD), true));
        assertTrue(rule.accepts(new ItemStack(Items.EMERALD), false));
    }

    @Test
    void transferRulesAndMatchModesAreIndependent() {
        TransferRule input = new TransferRule();
        TransferRule output = new TransferRule();
        input.toggle(TransferRule.ENABLED);
        input.toggle(TransferRule.AUTO);
        input.cycleMode(0);
        assertEquals(TransferRule.ENABLED | TransferRule.AUTO, input.flags());
        assertEquals(0, output.flags());
        assertEquals(TransferRule.MatchMode.ITEM, input.filter(0).mode());
        assertEquals(TransferRule.MatchMode.PRECISE, output.filter(0).mode());
        assertThrows(IllegalArgumentException.class, () -> input.toggle(8));
    }

    @Test
    void tagFiltersUseLoadedVanillaItemTags() {
        TransferRule rule = new TransferRule();
        rule.setFilter(0, new ItemStack(Items.OAK_PLANKS));
        rule.toggle(TransferRule.WHITELIST);
        for (int mode = 0; mode < 3; mode++) {
            rule.cycleMode(0);
        }
        assertEquals(TransferRule.MatchMode.TAG, rule.filter(0).mode());
        assertTrue(rule.accepts(new ItemStack(Items.BIRCH_PLANKS), true));
        assertFalse(rule.accepts(new ItemStack(Items.DIAMOND), true));
    }
}
