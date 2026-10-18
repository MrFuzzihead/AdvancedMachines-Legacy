package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import ic2.core.block.invslot.InvSlotOutput;

/**
 * The advanced machines' GUI speed readout is synced over the vanilla progress-bar channel. The ore
 * washer previously showed a constant 0 because only the standard-machine container did this.
 */
public class AdvancedMachineSpeedSyncTest {

    private static final class FakeMachine implements IAdvancedMachine {

        int speed;
        int clientSpeed = -1;

        @Override
        public String printFormattedData() {
            return Integer.toString(speed);
        }

        @Override
        public int getSpeed() {
            return speed;
        }

        @Override
        public void setClientSpeed(int value) {
            clientSpeed = value;
        }

        @Override
        public ArrayList<InvSlotOutput> getOutputSlots() {
            return new ArrayList<InvSlotOutput>();
        }
    }

    private static final class FakeCrafter implements ICrafting {

        int updates;
        int lastIndex = -1;
        int lastValue = -1;

        @Override
        public void sendContainerAndContentsToPlayer(Container container, List<ItemStack> list) {}

        @Override
        public void sendSlotContents(Container container, int index, ItemStack stack) {}

        @Override
        public void sendProgressBarUpdate(Container container, int index, int value) {
            updates++;
            lastIndex = index;
            lastValue = value;
        }
    }

    @Test
    public void sendsOnlyWhenTheSpeedChanges() {
        FakeMachine machine = new FakeMachine();
        AdvancedMachineSpeedSync sync = new AdvancedMachineSpeedSync(machine);
        FakeCrafter crafter = new FakeCrafter();
        List<ICrafting> crafters = Arrays.asList((ICrafting) crafter);

        sync.detectAndSendChanges(null, crafters);
        assertEquals("unchanged speed must not be sent", 0, crafter.updates);

        machine.speed = 5;
        sync.detectAndSendChanges(null, crafters);
        assertEquals(1, crafter.updates);
        assertEquals(AdvancedMachineSpeedSync.PROGRESS_BAR_ID, crafter.lastIndex);
        assertEquals(5, crafter.lastValue);

        sync.detectAndSendChanges(null, crafters);
        assertEquals("same speed again must not be re-sent", 1, crafter.updates);
    }

    @Test
    public void updatesTheClientSpeedOnlyForItsOwnChannel() {
        FakeMachine machine = new FakeMachine();
        AdvancedMachineSpeedSync sync = new AdvancedMachineSpeedSync(machine);

        assertTrue(sync.updateProgressBar(AdvancedMachineSpeedSync.PROGRESS_BAR_ID, 17));
        assertEquals(17, machine.clientSpeed);

        assertFalse(sync.updateProgressBar(AdvancedMachineSpeedSync.PROGRESS_BAR_ID + 1, 99));
        assertEquals("foreign channels must be left to the super container", 17, machine.clientSpeed);
    }
}
