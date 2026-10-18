package atomicstryker.ic2.advancedmachines;

import java.util.List;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;

/**
 * Syncs an advanced machine's speed counter to the client through the vanilla progress-bar channel
 * (id {@link #PROGRESS_BAR_ID}). Shared by {@link ContainerAdvancedMachine} and
 * {@link ContainerAdvancedOreWasher} so every advanced machine's GUI speed readout works — previously
 * the ore washer showed a permanent 0 because its base container had no sync.
 */
final class AdvancedMachineSpeedSync {

    static final int PROGRESS_BAR_ID = 3;

    private final IAdvancedMachine machine;
    private int networked;

    AdvancedMachineSpeedSync(IAdvancedMachine machine) {
        this.machine = machine;
    }

    /** Sends the speed to every crafter, but only when it changed since the last call. */
    void detectAndSendChanges(Container container, List<?> crafters) {
        int speed = machine.getSpeed();
        if (networked == speed) {
            return;
        }
        for (Object crafter : crafters) {
            if (crafter instanceof ICrafting) {
                ((ICrafting) crafter).sendProgressBarUpdate(container, PROGRESS_BAR_ID, speed);
            }
        }
        networked = speed;
    }

    /**
     * Handles a progress-bar update.
     *
     * @return true when the update belonged to the speed channel (and was consumed).
     */
    boolean updateProgressBar(int index, int value) {
        if (index != PROGRESS_BAR_ID) {
            return false;
        }
        machine.setClientSpeed(value);
        return true;
    }
}
