package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.BeforeClass;
import org.junit.Test;

import ic2.api.recipe.RecipeInputItemStack;
import ic2.api.recipe.Recipes;
import ic2.core.BasicMachineRecipeManager;

/**
 * Tests for the shared machine logic: the speed ramp is only fed while the base machine actually ran
 * (ANALYSIS.md §1.4), and a redstone signal now idles the machine instead of feeding it a fake dirt
 * output (ANALYSIS.md §1.6).
 */
public class CommonLogicAdvancedMachinesTest {

    private static final Item I_ORE = new Item();
    private static final Item I_INGOT = new Item();

    private static final AdvancedMachineSettings SETTINGS = new AdvancedMachineSettings(10, 10000, 3.0d);

    @BeforeClass
    public static void installRecipeManager() {
        Recipes.compressor = new BasicMachineRecipeManager();
        Recipes.compressor.addRecipe(new RecipeInputItemStack(new ItemStack(I_ORE)), null, new ItemStack(I_INGOT));
    }

    private static final class FakeProgress implements IProgressAccess {

        short value;

        @Override
        public short getRawProgress() {
            return value;
        }

        @Override
        public void setRawProgress(short v) {
            value = v;
        }
    }

    private static CommonLogicAdvancedMachines newLogic() {
        return new CommonLogicAdvancedMachines("%5d", 1, SETTINGS);
    }

    private static TileEntityAdvancedCompressor newCompressor() {
        TileEntityAdvancedCompressor te = new TileEntityAdvancedCompressor();
        te.inputSlot.put(new ItemStack(I_ORE));
        return te;
    }

    @Test
    public void extraProgressIsGrantedWhileRunning() {
        CommonLogicAdvancedMachines logic = newLogic();
        FakeProgress progress = new FakeProgress();

        for (int tick = 0; tick < SETTINGS.speedUpTicks; tick++) {
            logic.updateEntity(progress, true);
        }

        assertEquals(SETTINGS.speedUpTicks, logic.getSpeed());

        progress.value = 0;
        logic.updateEntity(progress, true);
        assertEquals("max speed should add factor-1 progress ticks", SETTINGS.speedUpFactor - 1, progress.value);
    }

    /**
     * §1.4 regression: the old code added extra progress whenever the machine was not in the redstone
     * "running empty" state, so an under-powered machine advanced (and banked) progress for free.
     */
    @Test
    public void noProgressIsGrantedWhenTheMachineDidNotRun() {
        CommonLogicAdvancedMachines logic = newLogic();
        FakeProgress progress = new FakeProgress();

        for (int tick = 0; tick < SETTINGS.speedUpTicks; tick++) {
            logic.updateEntity(progress, true);
        }
        progress.value = 42;
        int speedBefore = logic.getSpeed();

        for (int tick = 0; tick < 500; tick++) {
            logic.updateEntity(progress, false);
        }

        assertEquals("idle machine must not advance progress", 42, progress.value);
        assertEquals(speedBefore - 500, logic.getSpeed());
    }

    @Test
    public void speedRampsUpAndDecaysSmoothly() {
        CommonLogicAdvancedMachines logic = newLogic();
        FakeProgress progress = new FakeProgress();

        logic.updateEntity(progress, true);
        logic.updateEntity(progress, true);
        assertEquals(2, logic.getSpeed());

        logic.updateEntity(progress, false);
        assertEquals(1, logic.getSpeed());
        logic.updateEntity(progress, false);
        assertEquals(0, logic.getSpeed());
        logic.updateEntity(progress, false);
        assertEquals(0, logic.getSpeed());
    }

    @Test
    public void redstoneSignalStallsWithoutProducingARecipeResult() {
        TileEntityAdvancedCompressor te = newCompressor();
        CommonLogicAdvancedMachines logic = newLogic();
        logic.getOutputSlots()
            .add(te.outputSlot);

        assertNotNull(logic.getOutput(te, false));
        assertEquals(CommonLogicAdvancedMachines.OutputStatus.READY, logic.getLastOutputStatus());

        assertNull(logic.getOutput(te, true));
        assertEquals(CommonLogicAdvancedMachines.OutputStatus.STALLED, logic.getLastOutputStatus());
    }

    @Test
    public void powerDrawFactorIsAppliedToEnergyConsume() {
        TileEntityAdvancedCompressor te = newCompressor();
        int base = te.energyConsume;

        newLogic().setOverclockRates(te);

        assertEquals((int) Math.round(base * SETTINGS.powerDrawFactor), te.energyConsume);
        assertTrue(
            "buffer must cover a whole boosted operation",
            te.maxEnergy >= (long) te.operationLength * te.energyConsume);
    }
}
