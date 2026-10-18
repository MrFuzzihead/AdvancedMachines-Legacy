package atomicstryker.ic2.advancedmachines;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import ic2.api.recipe.RecipeOutput;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.machine.tileentity.TileEntityMacerator;

public class TileEntityAdvancedMacerator extends TileEntityMacerator implements IAdvancedMachine, IProgressAccess {

    private static final int EXTRA_OUT_SLOT_INDEX = 4;

    private final CommonLogicAdvancedMachines advLogic;

    public TileEntityAdvancedMacerator() {
        super();
        advLogic = new CommonLogicAdvancedMachines("%5d RPM", 1);
        advLogic.getOutputSlots()
            .add(outputSlot);
        advLogic.getOutputSlots()
            .add(new InvSlotOutput(this, "outputextra1", EXTRA_OUT_SLOT_INDEX, 1));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtt) {
        super.readFromNBT(nbtt);
        advLogic.readFromNBT(nbtt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtt) {
        super.writeToNBT(nbtt);
        advLogic.writeToNBT(nbtt);
    }

    @Override
    public void updateEntityServer() {
        super.updateEntityServer();
        advLogic.updateEntity(this);

        // if the extra output holds items while the primary output is free, move them over
        if (!advLogic.getOutputSlots()
            .get(1)
            .isEmpty() && advLogic.getOutputSlots()
                .get(0)
                .isEmpty()) {
            advLogic.getOutputSlots()
                .get(0)
                .put(
                    advLogic.getOutputSlots()
                        .get(1)
                        .get());
            advLogic.getOutputSlots()
                .get(1)
                .put(null);
        }
    }

    @Override
    public void setOverclockRates() {
        super.setOverclockRates();
        advLogic.setOverclockRates(this);
    }

    @Override
    public RecipeOutput getOutput() {
        return advLogic.getOutput(this);
    }

    @Override
    public void operateOnce(RecipeOutput output, List<ItemStack> processResult) {
        advLogic.operateOnce(this, output, processResult);
    }

    @Override
    public String printFormattedData() {
        return advLogic.printFormattedData();
    }

    @Override
    public int getSpeed() {
        return advLogic.getSpeed();
    }

    @Override
    public void setClientSpeed(int value) {
        advLogic.setClientSpeed(value);
    }

    @Override
    public ArrayList<InvSlotOutput> getOutputSlots() {
        return advLogic.getOutputSlots();
    }

    @Override
    public String getInventoryName() {
        return "Rotary Macerator";
    }

    /**
     * IC2 wrench convention: clicking the face the machine already faces removes it, clicking any
     * other face rotates it.
     */
    @Override
    public boolean wrenchCanSetFacing(EntityPlayer player, int side) {
        return this.getFacing() != side;
    }

    @Override
    public short getRawProgress() {
        return progress;
    }

    @Override
    public void setRawProgress(short value) {
        progress = value;
    }

}
