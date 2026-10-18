package atomicstryker.ic2.advancedmachines;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

public class ItemAdvancedMachine extends ItemBlock {

    /** Base IC2 machine consumption in EU/t (before the mod's power-draw multiplier), by metadata. */
    private static final int[] BASE_ENERGY_CONSUME = { 2, 2, 2, 1, 16 };

    /** Every advanced machine is a tier-1 sink, i.e. 32 EU/t max, unless upgraded in-world. */
    private static final int MACHINE_TIER_VOLTAGE = 32;

    private static final String KEY_POWER = "ic2.item.tooltip.power";
    private static final String KEY_MAX = "ic2.item.tooltip.max";

    public ItemAdvancedMachine(Block b) {
        super(b);
        setMaxDamage(0);
        setHasSubtypes(true);
        setCreativeTab(CreativeTabs.tabRedstone);
    }

    @Override
    public int getMetadata(int var1) {
        return var1;
    }

    @Override
    public String getItemStackDisplayName(ItemStack itemStack) {
        return EnumChatFormatting.GOLD + super.getItemStackDisplayName(itemStack);
    }

    @Override
    public String getUnlocalizedName(ItemStack var1) {
        int var2 = var1.getItemDamage();
        switch (var2) {
            case 0:
                return "item.advancedmachines:rotaryMacerator";
            case 1:
                return "item.advancedmachines:singularityCompressor";
            case 2:
                return "item.advancedmachines:centrifugeExtractor";
            case 3:
                return "item.advancedmachines:combinedRecycler";
            case 4:
                return "item.advancedmachines:rotaryOreWasher";
            default:
                // Unknown metadata (corrupt NBT, third-party item) must still have a display name.
                return super.getUnlocalizedName(var1);
        }
    }

    /**
     * Shows IC2's "Power X EU/t, Y EU/t max" line, matching the base machine tooltips. The draw is
     * computed from the configured power multiplier so it stays accurate if the config is changed.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List info, boolean advanced) {
        int meta = stack.getItemDamage();
        if (meta < 0 || meta >= BASE_ENERGY_CONSUME.length) {
            return;
        }

        ModAdvancedMachines mod = ModAdvancedMachines.instance;
        double factor = mod != null ? mod.getSettings().powerDrawFactor
            : AdvancedMachineSettings.DEFAULT_POWER_DRAW_FACTOR;
        int energyPerTick = MachineMath.scaledEnergyConsume(BASE_ENERGY_CONSUME[meta], factor);

        info.add(
            StatCollector.translateToLocal(KEY_POWER) + " "
                + energyPerTick
                + " EU/t, "
                + MACHINE_TIER_VOLTAGE
                + " EU/t "
                + StatCollector.translateToLocal(KEY_MAX));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void getSubItems(Item par1, CreativeTabs par2CreativeTabs, List par3List) {
        ModAdvancedMachines mod = ModAdvancedMachines.instance;
        for (int meta = 0; meta < BlockAdvancedMachines.MACHINE_COUNT; meta++) {
            if (mod == null || mod.isMachineEnabled(meta)) {
                par3List.add(new ItemStack(par1, 1, meta));
            }
        }
    }
}
