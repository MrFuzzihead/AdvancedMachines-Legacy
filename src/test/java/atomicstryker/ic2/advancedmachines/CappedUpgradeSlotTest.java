package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.BeforeClass;
import org.junit.Test;

import ic2.api.recipe.Recipes;
import ic2.core.BasicMachineRecipeManager;
import ic2.core.slot.SlotInvSlot;

/**
 * The container's upgrade slots should be swapped for capped ones (overclockers refused at insert
 * time). The overclocker-acceptance arithmetic itself is covered by
 * {@link MachineMathTest#overclockersFitUntilTheCap()}; constructing a real overclocker is not
 * possible in a plain JVM because {@code ItemUpgradeModule} touches {@code IC2.tabIC2}.
 */
public class CappedUpgradeSlotTest {

    @BeforeClass
    public static void installRecipeManager() {
        Recipes.compressor = new BasicMachineRecipeManager();
    }

    private static final class DummyContainer extends Container {

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }

        void add(Slot slot) {
            addSlotToContainer(slot);
        }
    }

    @Test
    public void upgradeSlotsAreReplacedWithCappedOnes() {
        TileEntityAdvancedCompressor te = new TileEntityAdvancedCompressor();
        DummyContainer container = new DummyContainer();
        container.add(new SlotInvSlot(te.upgradeSlot, 0, 8, 8));

        CappedUpgradeSlot.replaceUpgradeSlots(container, te.upgradeSlot);

        Object replaced = container.inventorySlots.get(0);
        assertTrue(replaced instanceof CappedUpgradeSlot);
        // Non-upgrade items are still rejected by IC2's own accepts().
        assertFalse(((CappedUpgradeSlot) replaced).isItemValid(new ItemStack(new Item())));
    }
}
