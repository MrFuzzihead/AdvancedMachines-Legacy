package atomicstryker.ic2.advancedmachines;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import ic2.api.recipe.RecipeOutput;
import ic2.core.block.TileEntityLiquidTankStandardMaschine;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import ic2.core.item.ItemUpgradeModule;

public class CommonLogicAdvancedMachines implements IAdvancedMachine {

    /**
     * Why {@link #getOutput(TileEntityStandardMachine)} returned what it did. Callers that need to
     * distinguish "no recipe" from "the result does not fit" (the Rotary Macerator's supplement
     * logic) can inspect this instead of having to re-probe the recipe manager.
     */
    public enum OutputStatus {
        /** The input slot is empty. */
        NO_INPUT,
        /** There is an input but the machine has no recipe for it. */
        NO_RECIPE,
        /** A recipe exists but none of the output slots (or the fluid tank) can hold the result. */
        NOT_STORABLE,
        /** The machine is redstone-powered and idles; the dummy output is returned. */
        STALLED,
        /** A real recipe result is available. */
        READY
    }

    private ModAdvancedMachines mod;
    private final ArrayList<InvSlotOutput> outSlotList;

    private boolean runningEmpty;
    private int speed;
    private final String dataFormat;
    private final int dataScaling;
    private final RecipeOutput dummyOut = new RecipeOutput(new NBTTagCompound(), new ItemStack(Blocks.dirt));
    private OutputStatus lastOutputStatus = OutputStatus.NO_INPUT;

    public CommonLogicAdvancedMachines(String dF, int dS) {
        mod = ModAdvancedMachines.instance;
        outSlotList = new ArrayList<InvSlotOutput>();
        speed = 0;
        dataFormat = dF;
        dataScaling = dS;
    }

    @Override
    public String printFormattedData() {
        return String.format(this.dataFormat, new Object[] { Integer.valueOf(speed * dataScaling) });
    }

    @Override
    public int getSpeed() {
        return speed;
    }

    @Override
    public void setClientSpeed(int value) {
        speed = value;
    }

    public void readFromNBT(NBTTagCompound nbtt) {
        try {
            speed = nbtt.getInteger("speed");
        } catch (Exception e) {}
    }

    public void writeToNBT(NBTTagCompound nbtt) {
        nbtt.setInteger("speed", speed);
    }

    public RecipeOutput getOutput(TileEntityStandardMachine te) {
        RecipeOutput ic2Output = null;
        boolean canStash = false;
        boolean hasRecipe = false;
        if (!te.inputSlot.isEmpty()) {
            ic2Output = te.inputSlot.process();
            if (ic2Output != null) {
                hasRecipe = true;
                for (int index = 0; index < outSlotList.size(); index++) {
                    if (outSlotList.get(index)
                        .canAdd(ic2Output.items)) {
                        canStash = true;
                        break;
                    }
                }
            }
        }

        if (!canStash) {
            ic2Output = null;
        }

        if (ic2Output != null && te instanceof TileEntityLiquidTankStandardMaschine) {
            if (ic2Output.metadata != null
                && ic2Output.metadata.getInteger("amount") > ((TileEntityLiquidTankStandardMaschine) te).getFluidTank()
                    .getFluidAmount()) {
                ic2Output = null;
            }
        }

        if (ic2Output != null) {
            runningEmpty = false;
            lastOutputStatus = OutputStatus.READY;
            return ic2Output;
        }

        if (te.inputSlot.isEmpty()) {
            lastOutputStatus = OutputStatus.NO_INPUT;
        } else if (!hasRecipe) {
            lastOutputStatus = OutputStatus.NO_RECIPE;
        } else {
            lastOutputStatus = OutputStatus.NOT_STORABLE;
        }

        if (te.hasWorldObj() && te.getWorldObj()
            .isBlockIndirectlyGettingPowered(te.xCoord, te.yCoord, te.zCoord)) {
            runningEmpty = true;
            lastOutputStatus = OutputStatus.STALLED;
            return dummyOut;
        }

        return null;
    }

    public OutputStatus getLastOutputStatus() {
        return lastOutputStatus;
    }

    /**
     * Inserts a result into the first output slot that can hold it. Returns false when nothing was
     * inserted (the caller should then not consume the input, or accept the loss).
     */
    public boolean addToOutputs(List<ItemStack> processResult) {
        for (InvSlotOutput slot : outSlotList) {
            if (slot.canAdd(processResult)) {
                slot.add(processResult);
                return true;
            }
        }
        return false;
    }

    public void operateOnce(TileEntityStandardMachine te, RecipeOutput output, List<ItemStack> processResult) {
        if (!te.inputSlot.isEmpty()) {
            te.inputSlot.consume();
            addToOutputs(processResult);
        }
    }

    public void updateEntity(TileEntityStandardMachine te) {
        if (!(te instanceof IProgressAccess)) {
            return;
        }
        IProgressAccess progress = (IProgressAccess) te;

        speed = MachineMath.rampSpeed(speed, mod.maxMachineSpeedUpTicks, te.getActive());

        if (runningEmpty) {
            progress.setRawProgress((short) 0);
            return;
        }

        // progress typically goes from 0(start) to 400,500(operation end) and gets incremented by 1
        // every tick. Additionally, setOverClockRates may mess with it and further increment it. The
        // goal here is to increase the base processing speed to up to N*100% by calculating the speed.
        short extraProgress = MachineMath
            .extraProgressTicks(speed, mod.maxMachineSpeedUpTicks, mod.maxMachineSpeedUpFactor);
        progress.setRawProgress((short) (progress.getRawProgress() + extraProgress));
    }

    public void setOverclockRates(TileEntityStandardMachine te) {
        te.energyConsume = MachineMath.scaledEnergyConsume(te.energyConsume, mod.machinePowerDrawFactor);

        int overclockers = 0;
        for (int i = 0; i < te.upgradeSlot.size(); i++) {
            ItemStack stack = te.upgradeSlot.get(i);
            if (stack != null) {
                if (stack.getItem() instanceof ItemUpgradeModule && stack.getItemDamage() == 0) {
                    overclockers += stack.stackSize;
                }
            }
        }
        if (overclockers > 6) {
            te.getWorldObj()
                .createExplosion(null, te.xCoord + 0.5f, te.yCoord, te.zCoord + 0.5f, 3f, true);
        }
    }

    @Override
    public ArrayList<InvSlotOutput> getOutputSlots() {
        return outSlotList;
    }

}
