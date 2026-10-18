package atomicstryker.ic2.advancedmachines;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import atomicstryker.ic2.advancedmachines.client.GuiCombinedRecycler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.recipe.RecipeOutput;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.machine.tileentity.TileEntityRecycler;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;

public class TileEntityAdvancedRecycler extends TileEntityRecycler implements IAdvancedMachine, IProgressAccess {

    private final CommonLogicAdvancedMachines advLogic;

    public TileEntityAdvancedRecycler() {
        super();
        advLogic = new CommonLogicAdvancedMachines("%5d cm3/s", 1);
        advLogic.getOutputSlots()
            .add(outputSlot);
        advLogic.getOutputSlots()
            .add(new InvSlotOutput(this, "outputextra1", 4, 1));
        advLogic.getOutputSlots()
            .add(new InvSlotOutput(this, "outputextra2", 5, 1));
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
    }

    @Override
    public void setOverclockRates() {
        advLogic.enforceOverclockerCap(this);
        super.setOverclockRates();
        advLogic.setOverclockRates(this);
    }

    @Override
    public RecipeOutput getOutput() {
        return advLogic.getOutput(this);
    }

    @Override
    public void operateOnce(RecipeOutput output, List<ItemStack> processResult) {
        if (inputSlot.isEmpty()) {
            return;
        }
        // Reproduce the base recycler's scrap chance; the old delegation to advLogic.operateOnce
        // always inserted the result, i.e. 8x the intended scrap yield.
        inputSlot.consume();
        if (MachineMath.rollScrap(IC2.random, TileEntityRecycler.recycleChance())) {
            advLogic.addToOutputs(processResult);
        }
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
    public short getRawProgress() {
        return progress;
    }

    @Override
    public void setRawProgress(short value) {
        progress = value;
    }

    @Override
    public String getInventoryName() {
        return "Combined Recycler";
    }

    @Override
    public boolean wrenchCanSetFacing(EntityPlayer player, int side) {
        return this.getFacing() != side;
    }

    @Override
    public ContainerBase<? extends TileEntityStandardMachine> getGuiContainer(EntityPlayer player) {
        return new ContainerAdvancedMachine<TileEntityStandardMachine>(player, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiScreen getGui(EntityPlayer player, boolean isAdmin) {
        return new GuiCombinedRecycler(new ContainerAdvancedMachine<TileEntityStandardMachine>(player, this));
    }

}
