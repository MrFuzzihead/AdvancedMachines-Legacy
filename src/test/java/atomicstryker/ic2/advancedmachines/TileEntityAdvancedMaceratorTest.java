package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.BeforeClass;
import org.junit.Test;

import ic2.api.recipe.RecipeInputItemStack;
import ic2.api.recipe.RecipeOutput;
import ic2.api.recipe.Recipes;
import ic2.core.BasicMachineRecipeManager;

/**
 * Behaviour tests for the Rotary Macerator (the supplement system has been removed; it is now a
 * faster macerator with a second output slot).
 */
public class TileEntityAdvancedMaceratorTest {

    private static final Item I_COPPER_ORE = new Item();
    private static final Item I_FILLER = new Item();
    private static final Item I_COPPER = new Item();
    private static final Item I_BRONZE = new Item();

    private static final ItemStack COPPER = new ItemStack(I_COPPER);
    private static final int EXTRA_OUTPUT_INVENTORY_INDEX = 7; // discharge(0) output(1) upgrades(2-5) input(6)

    @BeforeClass
    public static void installRecipeManager() {
        Recipes.macerator = new BasicMachineRecipeManager();
        Recipes.macerator.addRecipe(new RecipeInputItemStack(new ItemStack(I_COPPER_ORE)), null, COPPER);
    }

    @Test
    public void processesTheBaseRecipe() {
        TileEntityAdvancedMacerator te = new TileEntityAdvancedMacerator();
        te.inputSlot.put(new ItemStack(I_COPPER_ORE));

        RecipeOutput output = te.getOutput();

        assertNotNull(output);
        assertEquals(1, output.items.size());
        assertTrue(
            output.items.get(0)
                .isItemEqual(COPPER));

        te.operateOnce(output, output.items);

        assertTrue("input should have been consumed", te.inputSlot.isEmpty());
        assertTrue(
            te.outputSlot.get()
                .isItemEqual(COPPER));
    }

    @Test
    public void unknownInputProducesNothing() {
        TileEntityAdvancedMacerator te = new TileEntityAdvancedMacerator();
        te.inputSlot.put(new ItemStack(I_FILLER));

        assertNull(te.getOutput());
    }

    @Test
    public void extraOutputSlotIsExtractableButNotInsertable() {
        TileEntityAdvancedMacerator te = new TileEntityAdvancedMacerator();
        ItemStack stack = new ItemStack(I_BRONZE);
        te.getOutputSlots()
            .get(1)
            .put(stack);

        assertTrue(te.canExtractItem(EXTRA_OUTPUT_INVENTORY_INDEX, stack, 0));
        assertFalse(te.canInsertItem(EXTRA_OUTPUT_INVENTORY_INDEX, stack, 0));
    }

    @Test
    public void extraOutputMovesToThePrimarySlotWhenFree() {
        TileEntityAdvancedMacerator te = new TileEntityAdvancedMacerator();
        te.getOutputSlots()
            .get(1)
            .put(new ItemStack(I_BRONZE));

        te.updateEntityServer();

        assertTrue(
            "extra output should move into the primary output",
            te.outputSlot.get()
                .isItemEqual(new ItemStack(I_BRONZE)));
        assertTrue(
            te.getOutputSlots()
                .get(1)
                .isEmpty());
    }
}
