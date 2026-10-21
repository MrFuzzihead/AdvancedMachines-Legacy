package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The creative-tab listing should honour the per-machine "… Enabled" config flags.
 */
public class ItemAdvancedMachineTest {

    private ModAdvancedMachines previousInstance;

    @Before
    public void installModInstance() {
        previousInstance = ModAdvancedMachines.instance;
        ModAdvancedMachines.instance = new ModAdvancedMachines();
    }

    @After
    public void restoreModInstance() {
        ModAdvancedMachines.instance = previousInstance;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<Integer> metas(ItemAdvancedMachine item) {
        List raw = new ArrayList();
        item.getSubItems(item, null, raw);
        List<Integer> metas = new ArrayList<Integer>();
        for (Object entry : raw) {
            metas.add(((ItemStack) entry).getItemDamage());
        }
        return metas;
    }

    @Test
    public void unknownMetadataStillHasAName() {
        ItemAdvancedMachine item = new ItemAdvancedMachine(new BlockAdvancedMachines());
        assertNotNull(item.getUnlocalizedName(new ItemStack(item, 1, 99)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String tooltip(ItemAdvancedMachine item, int meta) {
        List raw = new ArrayList();
        item.addInformation(new ItemStack(item, 1, meta), null, raw, false);
        return raw.isEmpty() ? ""
            : raw.get(0)
                .toString();
    }

    /**
     * Mirrors IC2's base tooltip line, using the configured power multiplier (default 3x): macerator
     * 2 -> 6 EU/t, recycler 1 -> 3 EU/t, all LV/32; ore washer 16 -> 48 EU/t, which is MV/128.
     */
    @Test
    public void tooltipsShowPowerAndTier() {
        ItemAdvancedMachine item = new ItemAdvancedMachine(new BlockAdvancedMachines());

        assertTrue(tooltip(item, 0).contains("6 EU/t, 32 EU/t"));
        assertTrue(tooltip(item, 1).contains("6 EU/t, 32 EU/t"));
        assertTrue(tooltip(item, 2).contains("6 EU/t, 32 EU/t"));
        assertTrue(tooltip(item, 3).contains("3 EU/t, 32 EU/t"));
        assertTrue(tooltip(item, 4).contains("48 EU/t, 128 EU/t"));
    }

    /** Raising the power factor must raise the advertised tier with it, not just the draw. */
    @Test
    public void tooltipTierFollowsARaisedPowerFactor() {
        ModAdvancedMachines.instance.setSettings(new AdvancedMachineSettings(10, 10000, 20.0d));
        ItemAdvancedMachine item = new ItemAdvancedMachine(new BlockAdvancedMachines());

        assertTrue(tooltip(item, 0).contains("40 EU/t, 128 EU/t"));
        assertTrue(tooltip(item, 3).contains("20 EU/t, 32 EU/t"));
        assertTrue(tooltip(item, 4).contains("320 EU/t, 512 EU/t"));
    }

    @Test
    public void unknownMetadataHasNoTooltip() {
        ItemAdvancedMachine item = new ItemAdvancedMachine(new BlockAdvancedMachines());
        assertTrue(tooltip(item, 99).isEmpty());
    }

    @Test
    public void allMachinesAreListedWhenEnabled() {
        ItemAdvancedMachine item = new ItemAdvancedMachine(new BlockAdvancedMachines());
        assertEquals(BlockAdvancedMachines.MACHINE_COUNT, metas(item).size());
    }

    @Test
    public void disabledMachinesAreHidden() {
        ModAdvancedMachines.instance.setMachineEnabled(0, false);
        ModAdvancedMachines.instance.setMachineEnabled(3, false);

        ItemAdvancedMachine item = new ItemAdvancedMachine(new BlockAdvancedMachines());
        List<Integer> metas = metas(item);

        assertFalse(metas.contains(0));
        assertFalse(metas.contains(3));
        assertTrue(metas.contains(1));
        assertTrue(metas.contains(2));
        assertTrue(metas.contains(4));
        assertEquals(3, metas.size());
    }
}
