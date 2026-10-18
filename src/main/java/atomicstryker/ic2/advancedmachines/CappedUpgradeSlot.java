package atomicstryker.ic2.advancedmachines;

import java.util.List;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.slot.SlotInvSlot;

/**
 * A {@link SlotInvSlot} for a machine's upgrade slots that refuses overclockers once the machine
 * already holds {@link MachineMath#MAX_OVERCLOCKERS} of them. This caps them at insert time, so the
 * player never ends up with a seventh overclocker to be ejected (let alone the old explosion).
 * <p>
 * Only the GUI needs this: IC2's {@code InvSlotUpgrade} uses {@code Access.NONE}, so hoppers/pipes
 * cannot insert into upgrade slots at all.
 */
final class CappedUpgradeSlot extends SlotInvSlot {

    private CappedUpgradeSlot(InvSlotUpgrade upgradeSlot, int index, int x, int y) {
        super(upgradeSlot, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return super.isItemValid(stack)
            && CommonLogicAdvancedMachines.canAcceptOverclockers((InvSlotUpgrade) invSlot, stack);
    }

    /** Replaces the upgrade slots the base container created with capped equivalents. */
    static void replaceUpgradeSlots(Container container, InvSlotUpgrade upgradeSlot) {
        List<Slot> slots = container.inventorySlots;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot instanceof SlotInvSlot && ((SlotInvSlot) slot).invSlot == upgradeSlot) {
                SlotInvSlot original = (SlotInvSlot) slot;
                slots.set(
                    i,
                    new CappedUpgradeSlot(
                        upgradeSlot,
                        original.index,
                        original.xDisplayPosition,
                        original.yDisplayPosition));
            }
        }
    }
}
