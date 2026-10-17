package atomicstryker.ic2.advancedmachines;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;

import ic2.core.block.machine.tileentity.TileEntityStandardMachine;

public class ContainerAdvancedMacerator extends ContainerAdvancedMachine<TileEntityStandardMachine> {

    public ContainerAdvancedMacerator(EntityPlayer entityPlayer, TileEntityAdvancedMacerator tileEntity) {
        super(entityPlayer, tileEntity);
        // Maceration supplement slot. It is a plain (unsided) slot so the player can manage it; the
        // tile entity itself owns the charge state, so no click interception is needed here.
        addSlotToContainer(new Slot(tileEntity, TileEntityAdvancedMacerator.SUPPLEMENT_SLOT_INDEX, 75, 17));
    }

}
