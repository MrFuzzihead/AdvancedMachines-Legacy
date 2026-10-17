package atomicstryker.ic2.advancedmachines;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import ic2.api.recipe.RecipeOutput;
import ic2.api.recipe.Recipes;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.machine.tileentity.TileEntityMacerator;

public class TileEntityAdvancedMacerator extends TileEntityMacerator implements IAdvancedMachine, IProgressAccess {

    public static final int SUPPLEMENT_SLOT_INDEX = 8;
    public static final int EXTRA_OUT_SLOT_INDEX = 4;

    private static final String NBT_SUPPLEMENT_USES = "supplementUses";
    private static final String FLAG_SUPPLEMENT = "advSupplement";
    private static final String FLAG_DRAIN = "advDrain";
    private static final String FLAG_NO_BASE_RECIPE = "advNoBaseRecipe";

    private final CommonLogicAdvancedMachines advLogic;
    private final MaceratorSupplement.References supplementRefs;
    private final InvSlot supplementSlot;

    /**
     * Remaining macerations covered by the currently charged supplement item. Persisted so a chunk
     * reload does not hand out free supplement uses.
     */
    public int supplementedItemsLeft = 0;

    /** Prototype of the supplement item the current charge belongs to (transient, never persisted). */
    private ItemStack chargedSupplement;

    public TileEntityAdvancedMacerator() {
        this(MaceratorSupplement.References.fromIc2());
    }

    /** Test/injection constructor. */
    TileEntityAdvancedMacerator(MaceratorSupplement.References refs) {
        super();
        supplementRefs = refs;
        advLogic = new CommonLogicAdvancedMachines("%5d RPM", 1);
        advLogic.getOutputSlots()
            .add(outputSlot);
        advLogic.getOutputSlots()
            .add(new InvSlotOutput(this, "outputextra1", EXTRA_OUT_SLOT_INDEX, 1));
        supplementSlot = new InvSlot(this, "supplement", SUPPLEMENT_SLOT_INDEX, InvSlot.Access.NONE, 1);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtt) {
        super.readFromNBT(nbtt);
        advLogic.readFromNBT(nbtt);
        supplementedItemsLeft = Math.max(0, nbtt.getInteger(NBT_SUPPLEMENT_USES));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtt) {
        super.writeToNBT(nbtt);
        advLogic.writeToNBT(nbtt);
        nbtt.setInteger(NBT_SUPPLEMENT_USES, supplementedItemsLeft);
    }

    @Override
    public void updateEntityServer() {
        super.updateEntityServer();
        advLogic.updateEntity(this);

        // is second output is not empty but primary is, swap output slot contents
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
        RecipeOutput baseOutput = advLogic.getOutput(this);
        CommonLogicAdvancedMachines.OutputStatus status = advLogic.getLastOutputStatus();

        if (status == CommonLogicAdvancedMachines.OutputStatus.READY && baseOutput != null) {
            ArrayList<ItemStack> outItems = new ArrayList<ItemStack>(baseOutput.items);
            SupplementApplication application = runSupplementLogic(outItems);

            if (outItems.isEmpty()) {
                // A recipe with no items, or (with the corrected rules) impossible "everything was
                // replaced by nothing". Fall back to the untouched base output.
                return application.applied ? null : baseOutput;
            }

            NBTTagCompound metadata = copyMetadata(baseOutput.metadata);
            if (application.applied) {
                metadata.setBoolean(FLAG_SUPPLEMENT, true);
                metadata.setInteger(FLAG_DRAIN, application.inputDrain);
            }
            return new RecipeOutput(metadata, outItems);
        }

        if (status == CommonLogicAdvancedMachines.OutputStatus.NO_RECIPE) {
            // No IC2 macerator recipe for this input, but a supplement may still apply (quartz ore +
            // sand, netherrack + redstone). Handled here instead of polluting Recipes.macerator with
            // identity recipes that would also affect plain IC2 macerators.
            ArrayList<ItemStack> outItems = new ArrayList<ItemStack>();
            SupplementApplication application = runSupplementLogic(outItems);
            if (application.applied && !outItems.isEmpty() && canStash(outItems)) {
                NBTTagCompound metadata = new NBTTagCompound();
                metadata.setBoolean(FLAG_SUPPLEMENT, true);
                metadata.setInteger(FLAG_DRAIN, application.inputDrain);
                metadata.setBoolean(FLAG_NO_BASE_RECIPE, true);
                return new RecipeOutput(metadata, outItems);
            }
            return null;
        }

        // null, or the redstone stall output which must pass through untouched.
        return baseOutput;
    }

    /**
     * Applies the supplement rules to the (mutable) result list. Returns whether a supplement effect
     * was applied; the caller must only treat that as a match.
     */
    private SupplementApplication runSupplementLogic(List<ItemStack> items) {
        ItemStack supplement = supplementSlot.get();
        if (supplement == null) {
            supplementedItemsLeft = 0;
            chargedSupplement = null;
            return SupplementApplication.NONE;
        }
        resetChargeIfSupplementChanged(supplement);

        List<ItemStack> additions = new ArrayList<ItemStack>();
        boolean applied = false;
        int uses = 0;
        int inputDrain = 0;

        if (items.isEmpty()) {
            MaceratorSupplement.Outcome outcome = MaceratorSupplement
                .classify(supplementRefs, this::isTinSource, inputSlot.get(), null, supplement);
            if (outcome.hasEffect()) {
                additions.addAll(outcome.outputs);
                applied = true;
                uses = outcome.usesPerSupplement;
                inputDrain = outcome.inputDrain;
            }
        } else {
            Iterator<ItemStack> iterator = items.iterator();
            while (iterator.hasNext()) {
                ItemStack result = iterator.next();
                MaceratorSupplement.Outcome outcome = MaceratorSupplement
                    .classify(supplementRefs, this::isTinSource, inputSlot.get(), result, supplement);
                if (outcome.hasEffect()) {
                    iterator.remove();
                    additions.addAll(outcome.outputs);
                    applied = true;
                    if (uses == 0) {
                        uses = outcome.usesPerSupplement;
                    }
                    inputDrain = Math.max(inputDrain, outcome.inputDrain);
                }
            }
        }

        items.addAll(additions);

        if (applied && supplementedItemsLeft == 0) {
            supplementedItemsLeft = Math.max(1, uses);
        }

        return new SupplementApplication(applied, inputDrain);
    }

    private void resetChargeIfSupplementChanged(ItemStack supplement) {
        if (chargedSupplement != null && !sameStack(chargedSupplement, supplement)) {
            supplementedItemsLeft = 0;
        }
        chargedSupplement = supplement.copy();
        chargedSupplement.stackSize = 1;
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    private boolean isTinSource(ItemStack supplement) {
        if (supplementRefs.tinCrushed == null || Recipes.macerator == null) {
            return false;
        }
        RecipeOutput suppOut = Recipes.macerator.getOutputFor(supplement, false);
        if (suppOut == null) {
            return false;
        }
        for (ItemStack item : suppOut.items) {
            if (item != null && item.isItemEqual(supplementRefs.tinCrushed)) {
                return true;
            }
        }
        return false;
    }

    private boolean canStash(List<ItemStack> items) {
        for (InvSlotOutput slot : advLogic.getOutputSlots()) {
            if (slot.canAdd(items)) {
                return true;
            }
        }
        return false;
    }

    private static NBTTagCompound copyMetadata(NBTTagCompound metadata) {
        return metadata == null ? new NBTTagCompound() : (NBTTagCompound) metadata.copy();
    }

    @Override
    public void operateOnce(RecipeOutput output, List<ItemStack> processResult) {
        NBTTagCompound metadata = output != null ? output.metadata : null;
        boolean supplementOnly = metadata != null && metadata.getBoolean(FLAG_NO_BASE_RECIPE);
        boolean supplementApplied = metadata != null && metadata.getBoolean(FLAG_SUPPLEMENT);

        if (supplementOnly) {
            // No IC2 recipe exists for this input, so the generic helper (which calls
            // InvSlotProcessableGeneric.consume()) would throw; consume manually instead.
            consumeInput(1);
            advLogic.addToOutputs(processResult);
        } else {
            advLogic.operateOnce(this, output, processResult);
        }

        if (supplementApplied) {
            consumeSupplementCharge();
            int drain = metadata != null ? metadata.getInteger(FLAG_DRAIN) : 0;
            consumeInput(drain);
        }
    }

    private void consumeSupplementCharge() {
        if (supplementedItemsLeft <= 0) {
            return;
        }
        ItemStack supplement = supplementSlot.get();
        if (supplement == null) {
            supplementedItemsLeft = 0;
            return;
        }
        supplementedItemsLeft--;
        if (supplementedItemsLeft <= 0) {
            if (--supplement.stackSize <= 0) {
                supplementSlot.clear();
            }
            supplementedItemsLeft = 0;
        }
    }

    /** Decrements the input by {@code amount}, clearing the slot when it empties. Never throws. */
    private void consumeInput(int amount) {
        if (amount <= 0) {
            return;
        }
        ItemStack input = inputSlot.get();
        if (input == null) {
            return;
        }
        input.stackSize = Math.max(0, input.stackSize - amount);
        if (input.stackSize <= 0) {
            inputSlot.clear();
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
        return "Rotary Macerator";
    }

    /** Whether a supplement effect was applied and how much extra input it consumed. */
    private static final class SupplementApplication {

        static final SupplementApplication NONE = new SupplementApplication(false, 0);

        final boolean applied;
        final int inputDrain;

        SupplementApplication(boolean applied, int inputDrain) {
            this.applied = applied;
            this.inputDrain = inputDrain;
        }
    }

}
