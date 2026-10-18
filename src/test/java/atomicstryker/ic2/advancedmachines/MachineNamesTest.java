package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import ic2.api.recipe.Recipes;
import ic2.core.BasicMachineRecipeManager;

/**
 * The advanced machines should identify themselves by their own names (WAILA/NEI read
 * {@code getInventoryName()}), not the names of the IC2 machines they extend.
 * <p>
 * The Rotary Ore Washer is not covered here: {@code TileEntityOreWashing}'s constructor needs
 * {@code FluidRegistry.WATER}, which needs the vanilla/FML block registry, which cannot be
 * initialised outside the Forge launchwrapper. Its name is checked in the in-game QA pass instead.
 */
public class MachineNamesTest {

    @BeforeClass
    public static void installRecipeManagers() {
        Recipes.macerator = new BasicMachineRecipeManager();
        Recipes.compressor = new BasicMachineRecipeManager();
        Recipes.extractor = new BasicMachineRecipeManager();
        Recipes.recycler = new BasicMachineRecipeManager();
    }

    @Test
    public void machinesReportTheirOwnNames() {
        assertEquals("Rotary Macerator", new TileEntityAdvancedMacerator().getInventoryName());
        assertEquals("Singularity Compressor", new TileEntityAdvancedCompressor().getInventoryName());
        assertEquals("Centrifuge Extractor", new TileEntityAdvancedExtractor().getInventoryName());
        assertEquals("Combined Recycler", new TileEntityAdvancedRecycler().getInventoryName());
    }
}
