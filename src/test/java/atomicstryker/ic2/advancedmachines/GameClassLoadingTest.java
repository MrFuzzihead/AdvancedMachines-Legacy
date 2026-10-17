package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import ic2.core.block.BlockBase;

/**
 * Sanity checks that the Minecraft/IC2 classes the mod depends on are loadable from a plain JUnit
 * JVM. This is the harness check for the more game-coupled tests in later batches; if it ever breaks
 * it is an environment problem rather than a mod bug.
 */
public class GameClassLoadingTest {

    @Test
    public void itemStacksCanBeCreated() {
        ItemStack dirt = new ItemStack(Blocks.dirt);
        assertNotNull(dirt);
        assertTrue(dirt.isItemEqual(new ItemStack(Blocks.dirt)));
    }

    @Test
    public void ic2FaceMappingIsCallableAndInRange() {
        for (int facing = 0; facing < 6; facing++) {
            for (int side = 0; side < 6; side++) {
                int subIndex = BlockBase.getTextureSubIndex(facing, side);
                assertTrue("sub-index out of range for " + facing + "/" + side, subIndex >= 0 && subIndex < 6);
            }
        }
    }
}
