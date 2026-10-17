package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

/**
 * Pure decision-table tests for the macerator supplement rules.
 * <p>
 * The reference/output stacks are modelled with plain, unregistered {@link Item} instances. Those are
 * only ever compared by identity (and copied), so the vanilla item/block registries — which are not
 * bootstrapped in a unit test JVM — are not needed.
 */
public class MaceratorSupplementTest {

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
    private static final Item I_TIN_ORE = new Item();

    private static final ItemStack COPPER = new ItemStack(I_COPPER);
    private static final ItemStack TIN = new ItemStack(I_TIN);
    private static final ItemStack BRONZE = new ItemStack(I_BRONZE);
    private static final ItemStack HYDRATED = new ItemStack(I_HYDRATED);
    private static final ItemStack QUARTZ_TWO = new ItemStack(I_QUARTZ, 2);
    private static final ItemStack QUARTZ_ORE = new ItemStack(I_QUARTZ_ORE);
    private static final ItemStack SAND = new ItemStack(I_SAND);
    private static final ItemStack NETHERRACK = new ItemStack(I_NETHERRACK);
    private static final ItemStack REDSTONE = new ItemStack(I_REDSTONE);
    private static final ItemStack COAL_DUST = new ItemStack(I_COAL_DUST);
    private static final ItemStack WATER_BUCKET = new ItemStack(I_WATER_BUCKET);
    private static final ItemStack WATER_CELL = new ItemStack(I_WATER_CELL);
    private static final ItemStack ICE = new ItemStack(I_ICE);
    private static final ItemStack GLOWSTONE = new ItemStack(I_GLOWSTONE);
    private static final ItemStack TIN_ORE = new ItemStack(I_TIN_ORE);

    private static final MaceratorSupplement.References REFS = new MaceratorSupplement.References(
        COPPER,
        TIN,
        BRONZE,
        HYDRATED,
        QUARTZ_TWO,
        QUARTZ_ORE,
        SAND,
        NETHERRACK,
        REDSTONE,
        COAL_DUST,
        WATER_BUCKET,
        WATER_CELL,
        ICE,
        GLOWSTONE);

    private static final MaceratorSupplement.TinSource TIN_SOURCE = supplement -> supplement.isItemEqual(TIN_ORE);

    private static ItemStack stack(ItemStack prototype, int size) {
        ItemStack copy = prototype.copy();
        copy.stackSize = size;
        return copy;
    }

    @Test
    public void noSupplementHasNoEffect() {
        assertSame(
            MaceratorSupplement.Outcome.NONE,
            MaceratorSupplement.classify(REFS, TIN_SOURCE, COPPER, COPPER, null));
    }

    /**
     * Regression for the original bug: a supplement that does not match the current input/result must
     * report no effect, not an empty-but-present result that the caller deletes the output for.
     */
    @Test
    public void nonMatchingSupplementHasNoEffect() {
        assertFalse(MaceratorSupplement.classify(REFS, TIN_SOURCE, COPPER, COPPER, SAND).hasEffect());
        assertFalse(MaceratorSupplement.classify(REFS, TIN_SOURCE, COPPER, COPPER, REDSTONE).hasEffect());
    }

    @Test
    public void copperPlusTinSourceGivesBronze() {
        MaceratorSupplement.Outcome outcome = MaceratorSupplement
            .classify(REFS, TIN_SOURCE, COPPER, COPPER, TIN_ORE);

        assertTrue(outcome.hasEffect());
        assertEquals(MaceratorSupplement.Kind.BRONZE, outcome.kind);
        assertEquals(4, outcome.usesPerSupplement);
        assertEquals(0, outcome.inputDrain);
        assertEquals(1, outcome.outputs.size());
        assertTrue(outcome.outputs.get(0).isItemEqual(BRONZE));
        assertEquals(1, outcome.outputs.get(0).stackSize);
    }

    @Test
    public void bronzeOutputFollowsResultStackSize() {
        MaceratorSupplement.Outcome outcome = MaceratorSupplement
            .classify(REFS, TIN_SOURCE, COPPER, stack(COPPER, 3), TIN_ORE);

        assertEquals(3, outcome.outputs.get(0).stackSize);
    }

    @Test
    public void copperPlusNonTinSupplementHasNoEffect() {
        assertFalse(MaceratorSupplement.classify(REFS, TIN_SOURCE, COPPER, COPPER, SAND).hasEffect());
    }

    @Test
    public void quartzOrePlusSandGivesTwoQuartz() {
        MaceratorSupplement.Outcome outcome = MaceratorSupplement
            .classify(REFS, TIN_SOURCE, QUARTZ_ORE, QUARTZ_ORE, SAND);

        assertEquals(MaceratorSupplement.Kind.QUARTZ, outcome.kind);
        assertEquals(1, outcome.usesPerSupplement);
        assertEquals(2, outcome.outputs.get(0).stackSize);
        assertTrue(outcome.outputs.get(0).isItemEqual(QUARTZ_TWO));
    }

    @Test
    public void quartzSupplementWorksWithoutABaseRecipe() {
        // result == null models "the IC2 macerator has no recipe for this input at all"
        MaceratorSupplement.Outcome outcome = MaceratorSupplement
            .classify(REFS, TIN_SOURCE, QUARTZ_ORE, null, SAND);

        assertEquals(MaceratorSupplement.Kind.QUARTZ, outcome.kind);
    }

    @Test
    public void quartzOrePlusOtherSupplementHasNoEffect() {
        assertFalse(MaceratorSupplement.classify(REFS, TIN_SOURCE, QUARTZ_ORE, QUARTZ_ORE, REDSTONE).hasEffect());
    }

    @Test
    public void netherrackPlusRedstoneGivesGlowstoneAndDrainsSix() {
        MaceratorSupplement.Outcome outcome = MaceratorSupplement
            .classify(REFS, TIN_SOURCE, stack(NETHERRACK, 8), NETHERRACK, REDSTONE);

        assertEquals(MaceratorSupplement.Kind.GLOWSTONE, outcome.kind);
        assertEquals(1, outcome.usesPerSupplement);
        assertEquals(6, outcome.inputDrain);
        assertTrue(outcome.outputs.get(0).isItemEqual(GLOWSTONE));
    }

    @Test
    public void netherrackNeedsMoreThanSevenToTrigger() {
        assertFalse(
            MaceratorSupplement.classify(REFS, TIN_SOURCE, stack(NETHERRACK, 7), NETHERRACK, REDSTONE)
                .hasEffect());
        assertTrue(
            MaceratorSupplement.classify(REFS, TIN_SOURCE, stack(NETHERRACK, 8), NETHERRACK, REDSTONE)
                .hasEffect());
    }

    @Test
    public void netherrackSupplementWorksWithoutABaseRecipe() {
        MaceratorSupplement.Outcome outcome = MaceratorSupplement
            .classify(REFS, TIN_SOURCE, stack(NETHERRACK, 8), null, REDSTONE);
        assertEquals(MaceratorSupplement.Kind.GLOWSTONE, outcome.kind);
    }

    @Test
    public void coalDustPlusAnyWaterGivesHydratedCoalDust() {
        for (ItemStack water : new ItemStack[] { WATER_BUCKET, WATER_CELL, ICE }) {
            MaceratorSupplement.Outcome outcome = MaceratorSupplement
                .classify(REFS, TIN_SOURCE, COAL_DUST, COAL_DUST, water);
            assertEquals(MaceratorSupplement.Kind.HYDRATED_COAL, outcome.kind);
            assertEquals(8, outcome.usesPerSupplement);
            assertTrue(outcome.outputs.get(0).isItemEqual(HYDRATED));
        }
    }

    @Test
    public void coalDustPlusNonWaterHasNoEffect() {
        assertFalse(MaceratorSupplement.classify(REFS, TIN_SOURCE, COAL_DUST, COAL_DUST, SAND).hasEffect());
    }

    /**
     * Pins the original else-if precedence: once the result is crushed copper the other rules are not
     * considered, even if their own conditions would hold.
     */
    @Test
    public void bronzeRuleTakesPrecedenceOverQuartzAndNetherrackRules() {
        assertFalse(
            MaceratorSupplement.classify(REFS, TIN_SOURCE, stack(NETHERRACK, 8), COPPER, REDSTONE)
                .hasEffect());
        assertFalse(MaceratorSupplement.classify(REFS, TIN_SOURCE, QUARTZ_ORE, COPPER, SAND).hasEffect());
    }

    @Test
    public void nullInputsAreSafe() {
        assertSame(MaceratorSupplement.Outcome.NONE, MaceratorSupplement.classify(REFS, TIN_SOURCE, null, null, SAND));
        assertSame(
            MaceratorSupplement.Outcome.NONE,
            MaceratorSupplement.classify(null, TIN_SOURCE, COPPER, COPPER, TIN_ORE));
        assertSame(MaceratorSupplement.Outcome.NONE, MaceratorSupplement.classify(REFS, null, COPPER, COPPER, TIN_ORE));
    }

    @Test
    public void outputsAreCopiesNotSharedReferences() {
        MaceratorSupplement.Outcome first = MaceratorSupplement.classify(REFS, TIN_SOURCE, QUARTZ_ORE, null, SAND);
        MaceratorSupplement.Outcome second = MaceratorSupplement.classify(REFS, TIN_SOURCE, QUARTZ_ORE, null, SAND);
        assertTrue(first.outputs.get(0) != QUARTZ_TWO);
        assertTrue(first.outputs.get(0) != second.outputs.get(0));
    }
}
