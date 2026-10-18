package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import ic2.api.recipe.Recipes;
import ic2.core.BasicMachineRecipeManager;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;

/**
 * IC2 wrench convention: clicking the face a machine already faces removes it, clicking any other
 * face rotates it. (The ore washer is skipped here because constructing it needs the FML fluid
 * registry; see MachineNamesTest.)
 */
public class WrenchRotationTest {

    @BeforeClass
    public static void installRecipeManagers() {
        Recipes.macerator = new BasicMachineRecipeManager();
        Recipes.compressor = new BasicMachineRecipeManager();
        Recipes.extractor = new BasicMachineRecipeManager();
        Recipes.recycler = new BasicMachineRecipeManager();
    }

    private static void assertWrenchFacingBehaviour(TileEntityStandardMachine machine) {
        machine.prevFacing = 2; // avoid the IC2 network call in setFacing()
        machine.setFacing((short) 2);

        assertFalse("clicking the front face must fall through to removal", machine.wrenchCanSetFacing(null, 2));
        for (int side = 0; side < 6; side++) {
            if (side == 2) {
                continue;
            }
            assertTrue("clicking side " + side + " should rotate", machine.wrenchCanSetFacing(null, side));
        }
    }

    @Test
    public void maceratorRotatesExceptOnItsFrontFace() {
        assertWrenchFacingBehaviour(new TileEntityAdvancedMacerator());
    }

    @Test
    public void compressorRotatesExceptOnItsFrontFace() {
        assertWrenchFacingBehaviour(new TileEntityAdvancedCompressor());
    }

    @Test
    public void extractorRotatesExceptOnItsFrontFace() {
        assertWrenchFacingBehaviour(new TileEntityAdvancedExtractor());
    }

    @Test
    public void recyclerRotatesExceptOnItsFrontFace() {
        assertWrenchFacingBehaviour(new TileEntityAdvancedRecycler());
    }
}
