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
