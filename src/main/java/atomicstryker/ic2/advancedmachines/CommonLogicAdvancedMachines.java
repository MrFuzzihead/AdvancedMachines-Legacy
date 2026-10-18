package atomicstryker.ic2.advancedmachines;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import ic2.api.recipe.RecipeOutput;
import ic2.core.block.TileEntityLiquidTankStandardMaschine;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
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
        /** The machine is redstone-powered and idles at zero cost. */
        STALLED,
        /** A real recipe result is available. */
        READY
    }

    private final AdvancedMachineSettings settings;
    private final ArrayList<InvSlotOutput> outSlotList;

    private int speed;
    private final String dataFormat;
    private final int dataScaling;

    private OutputStatus lastOutputStatus = OutputStatus.NO_INPUT;

    public CommonLogicAdvancedMachines(String dF, int dS) {
        this(dF, dS, currentSettings());
    }

    CommonLogicAdvancedMachines(String dF, int dS, AdvancedMachineSettings settings) {
        this.settings = settings;
        outSlotList = new ArrayList<InvSlotOutput>();
        speed = 0;
        dataFormat = dF;
        dataScaling = dS;
    }

    private static AdvancedMachineSettings currentSettings() {
        ModAdvancedMachines instance = ModAdvancedMachines.instance;
        return instance != null ? instance.getSettings() : AdvancedMachineSettings.DEFAULTS;
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
        speed = nbtt.getInteger("speed");
    }

    public void writeToNBT(NBTTagCompound nbtt) {
        nbtt.setInteger("speed", speed);
    }

    public RecipeOutput getOutput(TileEntityStandardMachine te) {
        return getOutput(te, isRedstonePowered(te));
    }

    /**
     * @param powered whether the machine currently receives a redstone signal. While powered the
     *                machine idles: no progress, no energy use, and the speed ramp decays.
     */
    RecipeOutput getOutput(TileEntityStandardMachine te, boolean powered) {
        if (powered) {
            lastOutputStatus = OutputStatus.STALLED;
            return null;
        }

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
        if (te instanceof IProgressAccess) {
            updateEntity((IProgressAccess) te, te.getActive());
        }
    }

    /**
     * Grants the extra progress ticks for this tick.
     *
     * @param ranThisTick whether the base IC2 machine actually ran and paid EU this tick. Extra
     *                    progress is only granted then: the old implementation added it whenever the
     *                    machine was not in the redstone "running empty" state, which let an
     *                    under-powered machine advance (and even bank) progress for free.
     */
    void updateEntity(IProgressAccess progress, boolean ranThisTick) {
        speed = MachineMath.rampSpeed(speed, settings.speedUpTicks, ranThisTick);
        if (!ranThisTick) {
            return;
        }

        short extraProgress = MachineMath.extraProgressTicks(speed, settings.speedUpTicks, settings.speedUpFactor);
        progress.setRawProgress((short) (progress.getRawProgress() + extraProgress));
    }

    public void setOverclockRates(TileEntityStandardMachine te) {
        te.energyConsume = MachineMath.scaledEnergyConsume(te.energyConsume, settings.powerDrawFactor);

        // super.setOverclockRates() sized the internal buffer for the un-multiplied draw; grow it to
        // cover at least one boosted operation so the machine can actually use its extra speed.
        if (te.operationLength > 0) {
            long requiredStorage = (long) te.operationLength * te.energyConsume;
            if (requiredStorage > te.maxEnergy) {
                te.maxEnergy = (int) Math.min(Integer.MAX_VALUE, requiredStorage);
            }
        }
    }

    /**
     * Fallback guard: ejects overclockers beyond {@link MachineMath#MAX_OVERCLOCKERS}. Normal play is
     * now capped at insert time by {@link CappedUpgradeSlot}, so this only catches items forced in by
     * other mods, legacy saves or NBT edits. Replaces the old behaviour of exploding the machine.
     */
    public void enforceOverclockerCap(TileEntityStandardMachine te) {
        int overclockers = countOverclockers(te.upgradeSlot);
        int excess = MachineMath.excessOverclockers(overclockers, MachineMath.MAX_OVERCLOCKERS);
        if (excess <= 0) {
            return;
        }

        World world = te.getWorldObj();
        for (int i = te.upgradeSlot.size() - 1; i >= 0 && excess > 0; i--) {
            ItemStack stack = te.upgradeSlot.get(i);
            if (!isOverclocker(stack)) {
                continue;
            }
            int removed = Math.min(excess, stack.stackSize);
            ItemStack dropped = stack.copy();
            dropped.stackSize = removed;
            stack.stackSize -= removed;
            if (stack.stackSize <= 0) {
                te.upgradeSlot.put(i, null);
            }
            excess -= removed;
            if (world != null && !world.isRemote) {
                dropAtMachine(world, te, dropped);
            }
        }
    }

    public static int countOverclockers(InvSlotUpgrade upgradeSlot) {
        int overclockers = 0;
        for (int i = 0; i < upgradeSlot.size(); i++) {
            ItemStack stack = upgradeSlot.get(i);
            if (isOverclocker(stack)) {
                overclockers += stack.stackSize;
            }
        }
        return overclockers;
    }

    public static boolean isOverclocker(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemUpgradeModule && stack.getItemDamage() == 0;
    }

    /**
     * @return true when {@code stack} may be placed into the upgrade slot without exceeding
     *         {@link MachineMath#MAX_OVERCLOCKERS}. Non-overclockers always pass (IC2's own
     *         {@code InvSlotUpgrade.accepts} decides whether they are valid upgrades).
     */
    public static boolean canAcceptOverclockers(InvSlotUpgrade upgradeSlot, ItemStack stack) {
        if (!isOverclocker(stack)) {
            return true;
        }
        return MachineMath
            .canAddOverclockers(countOverclockers(upgradeSlot), stack.stackSize, MachineMath.MAX_OVERCLOCKERS);
    }

    private static void dropAtMachine(World world, TileEntityStandardMachine te, ItemStack stack) {
        double x = te.xCoord + 0.5D + (world.rand.nextDouble() - 0.5D) * 0.5D;
        double y = te.yCoord + 0.5D + (world.rand.nextDouble() - 0.5D) * 0.5D;
        double z = te.zCoord + 0.5D + (world.rand.nextDouble() - 0.5D) * 0.5D;
        EntityItem entity = new EntityItem(world, x, y, z, stack);
        entity.delayBeforeCanPickup = 10;
        world.spawnEntityInWorld(entity);
    }

    @Override
    public ArrayList<InvSlotOutput> getOutputSlots() {
        return outSlotList;
    }

    private static boolean isRedstonePowered(TileEntityStandardMachine te) {
        return te.hasWorldObj() && te.getWorldObj()
            .isBlockIndirectlyGettingPowered(te.xCoord, te.yCoord, te.zCoord);
    }

}
