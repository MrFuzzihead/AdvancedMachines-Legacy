package atomicstryker.ic2.advancedmachines;

import net.minecraft.entity.player.EntityPlayer;

import ic2.core.block.machine.container.ContainerStandardMachine;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import ic2.core.slot.SlotInvSlot;

public class ContainerAdvancedMachine<T extends TileEntityStandardMachine> extends ContainerStandardMachine<T> {

    private final AdvancedMachineSpeedSync speedSync;

    public ContainerAdvancedMachine(EntityPlayer entityPlayer, T tileEntity) {
        super(entityPlayer, tileEntity);
        speedSync = new AdvancedMachineSpeedSync((IAdvancedMachine) tileEntity);

        IAdvancedMachine advM = (IAdvancedMachine) tileEntity;

        if (advM.getOutputSlots()
            .size() == 2) {
            addSlotToContainer(
                new SlotInvSlot(
                    advM.getOutputSlots()
                        .get(1),
                    0,
                    115,
                    53));
        } else if (advM.getOutputSlots()
            .size() == 3) {
                addSlotToContainer(
                    new SlotInvSlot(
                        advM.getOutputSlots()
                            .get(1),
                        0,
                        115,
                        17));
                addSlotToContainer(
                    new SlotInvSlot(
                        advM.getOutputSlots()
                            .get(2),
                        0,
                        115,
                        53));
            }

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
