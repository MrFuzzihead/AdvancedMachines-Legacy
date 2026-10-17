package atomicstryker.ic2.advancedmachines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import ic2.api.item.IC2Items;

/**
 * The macerator "supplement" rules, extracted from {@link TileEntityAdvancedMacerator} so that the
 * decision table can be unit tested without a running machine.
 * <p>
 * A supplement is an item placed in the Rotary Macerator's supplement slot. When the current input /
 * result matches one of the rules below the supplement replaces (or adds to) the normal maceration
 * output and is consumed once every {@link Outcome#usesPerSupplement} operations.
 * <p>
 * The important contract, and the fix for the original bug, is that a non-matching supplement must
 * return {@link Outcome#NONE} — never an empty (but non-null) result that the caller mistakes for a
 * match and uses to delete the real maceration output.
 */
public final class MaceratorSupplement {

    private MaceratorSupplement() {}

    /** Asks whether a supplement item macerates into crushed tin ore (IC2 recipe driven). */
    public interface TinSource {

        boolean isTinSource(ItemStack supplement);
    }

    public enum Kind {
        NONE,
        BRONZE,
        QUARTZ,
        GLOWSTONE,
        HYDRATED_COAL
    }

    /** Immutable per-machine reference stacks the rules compare against / copy from. */
    public static final class References {

        public final ItemStack copperCrushed;
        public final ItemStack tinCrushed;
        public final ItemStack bronzeDust;
        public final ItemStack hydratedCoalDust;
        public final ItemStack twoQuartz;
        public final ItemStack quartzOre;
        public final ItemStack sand;
        public final ItemStack netherrack;
        public final ItemStack redstone;
        public final ItemStack coalDust;
        public final ItemStack waterBucket;
        public final ItemStack waterCell;
        public final ItemStack ice;
        public final ItemStack glowstoneDust;

        public References(ItemStack copperCrushed, ItemStack tinCrushed, ItemStack bronzeDust,
            ItemStack hydratedCoalDust, ItemStack twoQuartz, ItemStack quartzOre, ItemStack sand,
            ItemStack netherrack, ItemStack redstone, ItemStack coalDust, ItemStack waterBucket,
            ItemStack waterCell, ItemStack ice, ItemStack glowstoneDust) {
            this.copperCrushed = copperCrushed;
            this.tinCrushed = tinCrushed;
            this.bronzeDust = bronzeDust;
            this.hydratedCoalDust = hydratedCoalDust;
            this.twoQuartz = twoQuartz;
            this.quartzOre = quartzOre;
            this.sand = sand;
            this.netherrack = netherrack;
            this.redstone = redstone;
            this.coalDust = coalDust;
            this.waterBucket = waterBucket;
            this.waterCell = waterCell;
            this.ice = ice;
            this.glowstoneDust = glowstoneDust;
        }

        /** Builds the references from the live IC2/vanilla items (only valid once IC2 has loaded). */
        public static References fromIc2() {
            return new References(
                IC2Items.getItem("crushedCopperOre"),
                IC2Items.getItem("crushedTinOre"),
                IC2Items.getItem("bronzeDust"),
                IC2Items.getItem("hydratedCoalDust"),
                new ItemStack(Items.quartz, 2),
                new ItemStack(Blocks.quartz_ore),
                new ItemStack(Blocks.sand),
                new ItemStack(Blocks.netherrack),
                new ItemStack(Items.redstone),
                IC2Items.getItem("coalDust"),
                new ItemStack(Items.water_bucket),
                IC2Items.getItem("waterCell"),
                new ItemStack(Blocks.ice),
                new ItemStack(Items.glowstone_dust));
        }
    }

    /** The effect a supplement has on a single maceration. */
    public static final class Outcome {

        public static final Outcome NONE = new Outcome(Kind.NONE, Collections.<ItemStack>emptyList(), 0, 0);

        public final Kind kind;
        public final List<ItemStack> outputs;
        public final int usesPerSupplement;
        public final int inputDrain;

        private Outcome(Kind kind, List<ItemStack> outputs, int usesPerSupplement, int inputDrain) {
            this.kind = kind;
            this.outputs = Collections.unmodifiableList(outputs);
            this.usesPerSupplement = usesPerSupplement;
            this.inputDrain = inputDrain;
        }

        public boolean hasEffect() {
            return kind != Kind.NONE;
        }
    }

    /**
     * @param refs       reference stacks (never null in production; null-safe here)
     * @param tinSource  strategy used for the bronze rule; may be null
     * @param original   the macerator input stack (may be null)
     * @param result     a maceration output stack, or null when there is no base recipe
     * @param supplement the supplement stack (must be non-null for any effect)
     * @return the effect, or {@link Outcome#NONE}; never null
     */
    public static Outcome classify(References refs, TinSource tinSource, ItemStack original, ItemStack result,
        ItemStack supplement) {
        if (refs == null || supplement == null || original == null && result == null) {
            return Outcome.NONE;
        }

        // Copper crushed ore + anything that macerates to tin crushed ore -> bronze dust.
        if (result != null && safeIsItemEqual(result, refs.copperCrushed)) {
            if (tinSource != null && tinSource.isTinSource(supplement) && refs.bronzeDust != null) {
                return new Outcome(
                    Kind.BRONZE,
                    list(copyWithSize(refs.bronzeDust, Math.max(1, result.stackSize))),
                    4,
                    0);
            }
            return Outcome.NONE;
        }

        // Quartz ore + sand -> two nether quartz.
        if (original != null && safeIsItemEqual(original, refs.quartzOre)
            && safeIsItemEqual(supplement, refs.sand)
            && refs.twoQuartz != null) {
            return new Outcome(Kind.QUARTZ, list(copyWithSize(refs.twoQuartz, 2)), 1, 0);
        }

        // Netherrack + redstone -> glowstone dust; seven netherrack are consumed per glowstone.
        if (original != null && safeIsItemEqual(original, refs.netherrack)
            && safeIsItemEqual(supplement, refs.redstone)
            && original.stackSize > 7
            && refs.glowstoneDust != null) {
            return new Outcome(Kind.GLOWSTONE, list(copyWithSize(refs.glowstoneDust, 1)), 1, 6);
        }

        // Coal dust + water (bucket / water cell / ice) -> hydrated coal dust.
        if (result != null && safeIsItemEqual(result, refs.coalDust)
            && isWater(refs, supplement)
            && refs.hydratedCoalDust != null) {
            return new Outcome(Kind.HYDRATED_COAL, list(copyWithSize(refs.hydratedCoalDust, 1)), 8, 0);
        }

        return Outcome.NONE;
    }

    public static boolean isWater(References refs, ItemStack item) {
        return item != null && (safeIsItemEqual(item, refs.waterBucket) || safeIsItemEqual(item, refs.waterCell)
            || safeIsItemEqual(item, refs.ice));
    }

    private static boolean safeIsItemEqual(ItemStack stack, ItemStack reference) {
        return reference != null && stack.isItemEqual(reference);
    }

    private static ItemStack copyWithSize(ItemStack stack, int size) {
        ItemStack copy = stack.copy();
        copy.stackSize = size;
        return copy;
    }

    private static List<ItemStack> list(ItemStack stack) {
        List<ItemStack> result = new ArrayList<ItemStack>(1);
        result.add(stack);
        return result;
    }
}
