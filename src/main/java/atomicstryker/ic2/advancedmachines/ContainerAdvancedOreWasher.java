package atomicstryker.ic2.advancedmachines;

import net.minecraft.entity.player.EntityPlayer;

import ic2.core.block.machine.container.ContainerOreWashing;

/**
 * Ore-washer container with the advanced-machine speed sync bolted on. The washer's GUI shows an RPM
 * readout, so unlike the other four machines it cannot use {@link ContainerAdvancedMachine} (whose
 * slot layout is the standard machine one) and needs this small subclass instead.
 */
public class ContainerAdvancedOreWasher extends ContainerOreWashing {

    private final AdvancedMachineSpeedSync speedSync;

    public ContainerAdvancedOreWasher(EntityPlayer entityPlayer, TileEntityAdvancedOreWasher tileEntity) {
        super(entityPlayer, tileEntity);
        speedSync = new AdvancedMachineSpeedSync(tileEntity);
        CappedUpgradeSlot.replaceUpgradeSlots(this, tileEntity.upgradeSlot);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        speedSync.detectAndSendChanges(this, crafters);
    }

    @Override
    public void updateProgressBar(int index, int value) {
        if (!speedSync.updateProgressBar(index, value)) {
            super.updateProgressBar(index, value);
        }
    }
}
