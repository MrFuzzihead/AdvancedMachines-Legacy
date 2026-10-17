package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.BeforeClass;
import org.junit.Test;

import cpw.mods.fml.common.registry.GameRegistry;
import ic2.api.recipe.RecipeInputItemStack;
import ic2.api.recipe.RecipeOutput;
import ic2.api.recipe.Recipes;
import ic2.core.BasicMachineRecipeManager;

/**
 * Integration tests that drive the real {@link TileEntityAdvancedMacerator} with a throw-away IC2
 * recipe manager. They cover the supplement bugs from ANALYSIS.md §1.1, §1.2, §1.10 and §1.12:
 * non-matching supplements must never eat the normal output, blocked outputs must not be replaced by
 * a supplement, state must survive NBT, and quartz/netherrack must work without global identity
 * recipes.
 * <p>
 * Unregistered {@link Item} instances are used for the stacks; only identity, counts and copies
 * matter to the code under test, and the vanilla registries are not bootstrapped in a unit test JVM.
 */
public class TileEntityAdvancedMaceratorTest {

    private static final Item I_COPPER_ORE = new Item();
    private static final Item I_TIN_ORE = new Item();
    private static final Item I_BONE = new Item();
    private static final Item I_FILLER = new Item();
    private static final Item I_COPPER = new Item();
    private static final Item I_TIN = new Item();
    private static final Item I_BRONZE = new Item();
    private static final Item I_HYDRATED = new Item();
    private static final Item I_QUARTZ = new Item();
    private static final Item I_QUARTZ_ORE = new Item();
    private static final Item I_SAND = new Item();
    private static final Item I_NETHERRACK = new Item();
    private static final Item I_REDSTONE = new Item();
    private static final Item I_COAL_DUST = new Item();
    private static final Item I_WATER_BUCKET = new Item();
    private static final Item I_WATER_CELL = new Item();
    private static final Item I_ICE = new Item();
    private static final Item I_GLOWSTONE = new Item();

    private static final ItemStack COPPER = new ItemStack(I_COPPER);
    private static final ItemStack TIN = new ItemStack(I_TIN);
    private static final ItemStack BRONZE = new ItemStack(I_BRONZE);
    private static final ItemStack HYDRATED = new ItemStack(I_HYDRATED);
    private static final ItemStack QUARTZ = new ItemStack(I_QUARTZ);
    private static final ItemStack GLOWSTONE = new ItemStack(I_GLOWSTONE);
    private static final ItemStack COAL_DUST = new ItemStack(I_COAL_DUST);
    private static final ItemStack TIN_ORE = new ItemStack(I_TIN_ORE);

    private static final MaceratorSupplement.References REFS = new MaceratorSupplement.References(
        COPPER,
        TIN,
        BRONZE,
        HYDRATED,
        new ItemStack(I_QUARTZ, 2),
        new ItemStack(I_QUARTZ_ORE),
        new ItemStack(I_SAND),
        new ItemStack(I_NETHERRACK),
        new ItemStack(I_REDSTONE),
        COAL_DUST,
        new ItemStack(I_WATER_BUCKET),
        new ItemStack(I_WATER_CELL),
        new ItemStack(I_ICE),
        GLOWSTONE);

    @BeforeClass
    public static void installRecipeManager() {
        Recipes.macerator = new BasicMachineRecipeManager();
        Recipes.macerator.addRecipe(new RecipeInputItemStack(new ItemStack(I_COPPER_ORE)), null, COPPER);
        Recipes.macerator.addRecipe(new RecipeInputItemStack(new ItemStack(I_TIN_ORE)), null, TIN);
        Recipes.macerator.addRecipe(new RecipeInputItemStack(new ItemStack(I_BONE)), null, COAL_DUST);
        // writeToNBT()/readFromNBT() look up the tile entity's registry mapping, so register it.
        GameRegistry.registerTileEntity(TileEntityAdvancedMacerator.class, "advancedMaceratorUnitTest");
    }

    private static TileEntityAdvancedMacerator newMacerator() {
        return new TileEntityAdvancedMacerator(REFS);
    }

    private static void setSupplement(TileEntityAdvancedMacerator te, ItemStack stack) {
        te.setInventorySlotContents(TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX, stack);
    }

    /**
     * §1.1 regression: a supplement that does not match the current recipe must leave the normal
     * output intact.
     */
    @Test
    public void nonMatchingSupplementKeepsBaseOutput() {
        TileEntityAdvancedMacerator te = newMacerator();
        te.inputSlot.put(new ItemStack(I_COPPER_ORE));
        setSupplement(te, new ItemStack(I_SAND));

        RecipeOutput output = te.getOutput();

        assertNotNull(output);
        assertEquals(1, output.items.size());
        assertTrue(
            output.items.get(0)
                .isItemEqual(COPPER));
        assertFalse(output.metadata.getBoolean("advSupplement"));
    }

    @Test
    public void matchingBronzeSupplementReplacesOutputAndStartsCharge() {
        TileEntityAdvancedMacerator te = newMacerator();
        te.inputSlot.put(new ItemStack(I_COPPER_ORE));
        setSupplement(te, new ItemStack(I_TIN_ORE));

        RecipeOutput output = te.getOutput();

        assertNotNull(output);
        assertEquals(1, output.items.size());
        assertTrue(
            output.items.get(0)
                .isItemEqual(BRONZE));
        assertTrue(output.metadata.getBoolean("advSupplement"));

        te.operateOnce(output, output.items);

        assertEquals(3, te.supplementedItemsLeft);
        assertEquals(1, te.getStackInSlot(TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX).stackSize);
        assertTrue(te.inputSlot.isEmpty());
    }

    @Test
    public void fourthBronzeOperationConsumesTheSupplementItem() {
        TileEntityAdvancedMacerator te = newMacerator();
        setSupplement(te, new ItemStack(I_TIN_ORE));

        for (int operation = 0; operation < 4; operation++) {
            te.inputSlot.put(new ItemStack(I_COPPER_ORE));
            RecipeOutput output = te.getOutput();
            assertNotNull("operation " + operation, output);
            te.operateOnce(output, output.items);
        }

        assertEquals(0, te.supplementedItemsLeft);
        assertNull(te.getStackInSlot(TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX));
    }

    /**
     * §1.12: quartz ore has no IC2 macerator recipe, and the old implementation faked one with a
     * global identity recipe. The supplement must still work with no base recipe.
     */
    @Test
    public void quartzSupplementWorksWithoutABaseRecipe() {
        TileEntityAdvancedMacerator te = newMacerator();
        te.inputSlot.put(new ItemStack(I_QUARTZ_ORE));
        setSupplement(te, new ItemStack(I_SAND));

        RecipeOutput output = te.getOutput();

        assertNotNull(output);
        assertTrue(output.metadata.getBoolean("advNoBaseRecipe"));
        assertEquals(1, output.items.size());
        assertEquals(2, output.items.get(0).stackSize);
        assertTrue(
            output.items.get(0)
                .isItemEqual(QUARTZ));

        te.operateOnce(output, output.items);

        assertTrue(te.inputSlot.isEmpty());
        assertNull(te.getStackInSlot(TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX));
    }

    @Test
    public void glowstoneSupplementDrainsSixExtraNetherrack() {
        TileEntityAdvancedMacerator te = newMacerator();
        te.inputSlot.put(new ItemStack(I_NETHERRACK, 8));
        setSupplement(te, new ItemStack(I_REDSTONE));

        RecipeOutput output = te.getOutput();

        assertNotNull(output);
        assertTrue(
            output.items.get(0)
                .isItemEqual(GLOWSTONE));
        assertEquals(6, output.metadata.getInteger("advDrain"));

        te.operateOnce(output, output.items);

        assertEquals(1, te.inputSlot.get().stackSize);
    }

    @Test
    public void hydrationSupplementLastsEightOperations() {
        TileEntityAdvancedMacerator te = newMacerator();
        te.inputSlot.put(new ItemStack(I_BONE));
        setSupplement(te, new ItemStack(I_WATER_BUCKET));

        RecipeOutput output = te.getOutput();

        assertNotNull(output);
        assertTrue(
            output.items.get(0)
                .isItemEqual(HYDRATED));

        te.operateOnce(output, output.items);

        assertEquals(7, te.supplementedItemsLeft);
        assertEquals(1, te.getStackInSlot(TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX).stackSize);
    }

    /**
     * §1.2 regression: when the output slots are full the helper refuses to hand out a result; the
     * supplement logic must not fabricate one, otherwise the input is consumed and the result is
     * destroyed.
     */
    @Test
    public void blockedOutputDoesNotProduceASupplementResult() {
        TileEntityAdvancedMacerator te = newMacerator();
        te.inputSlot.put(new ItemStack(I_COPPER_ORE));
        setSupplement(te, new ItemStack(I_TIN_ORE));
        te.outputSlot.put(new ItemStack(I_FILLER, 64));
        te.getOutputSlots()
            .get(1)
            .put(new ItemStack(I_FILLER, 64));

        assertNull(te.getOutput());
        assertFalse(te.inputSlot.isEmpty());
        assertNotNull(te.getStackInSlot(TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX));
    }

    @Test
    public void noRecipeAndNoApplicableSupplementProducesNothing() {
        TileEntityAdvancedMacerator te = newMacerator();
        te.inputSlot.put(new ItemStack(I_FILLER));
        setSupplement(te, new ItemStack(I_SAND));

        assertNull(te.getOutput());
    }

    @Test
    public void supplementChargeSurvivesNbtRoundTrip() {
        TileEntityAdvancedMacerator te = newMacerator();
        te.supplementedItemsLeft = 5;

        NBTTagCompound nbt = new NBTTagCompound();
        te.writeToNBT(nbt);

        TileEntityAdvancedMacerator loaded = newMacerator();
        loaded.readFromNBT(nbt);

        assertEquals(5, loaded.supplementedItemsLeft);
    }

    @Test
    public void changingSupplementResetsTheStaleCharge() {
        TileEntityAdvancedMacerator te = newMacerator();
        setSupplement(te, new ItemStack(I_SAND));
        te.inputSlot.put(new ItemStack(I_QUARTZ_ORE));

        RecipeOutput first = te.getOutput();
        assertNotNull(first);
        // sand charge is known to the machine now (1 quartz operation per sand)
        assertEquals(1, te.supplementedItemsLeft);

        // Pretend the charge got partially consumed, then swap the supplement item entirely.
        te.supplementedItemsLeft = 5;
        setSupplement(te, new ItemStack(I_REDSTONE));
        te.inputSlot.put(new ItemStack(I_NETHERRACK, 8));

        RecipeOutput second = te.getOutput();
        assertNotNull(second);
        // stale sand charge dropped; the glowstone rule starts a fresh 1-use charge
        assertEquals(1, te.supplementedItemsLeft);
    }

    /**
     * §1.11: the supplement slot is player-managed only. Automation must not be able to pull items
     * out of it or push items in.
     */
    @Test
    public void automationCannotManageTheSupplementSlot() {
        TileEntityAdvancedMacerator te = newMacerator();
        ItemStack supplement = new ItemStack(I_TIN_ORE);

        assertFalse(te.canInsertItem(TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX, supplement, 0));
        assertFalse(te.canExtractItem(TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX, supplement, 0));
    }

    /** The extra output slot stays automatable (bottom side extraction) after the index mix-up fix. */
    @Test
    public void automationCanExtractTheExtraOutputSlot() {
        TileEntityAdvancedMacerator te = newMacerator();
        int extraOutput = 7; // discharge(0) output(1) upgrades(2-5) input(6) outputextra(7) supplement(8)
        ItemStack stack = new ItemStack(I_BRONZE);
        te.getOutputSlots()
            .get(1)
            .put(stack);

        assertTrue(te.canExtractItem(extraOutput, stack, 0));
        assertFalse(te.canInsertItem(extraOutput, stack, 0));
    }
}
