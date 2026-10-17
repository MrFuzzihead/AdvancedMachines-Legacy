package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import ic2.core.block.BlockBase;

/**
 * Sanity checks that the Minecraft/IC2 classes the mod depends on are loadable from a plain JUnit
 * JVM, and that the calculations do not rely on the vanilla item/block registries (which are not
 * bootstrapped here — use bare {@link Item} instances when a distinct, unregistered stack is needed).
 */
public class GameClassLoadingTest {

    @Test
    public void itemStacksCanBeCreatedWithoutRegistries() {
        Item item = new Item();
        ItemStack stack = new ItemStack(item, 3, 1);
        assertNotNull(stack);
        assertEquals(3, stack.stackSize);
        assertTrue(stack.isItemEqual(new ItemStack(item, 1, 1)));
        assertFalse(stack.isItemEqual(new ItemStack(new Item())));
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
