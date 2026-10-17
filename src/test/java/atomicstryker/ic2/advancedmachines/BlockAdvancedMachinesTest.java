package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for the block-level robustness fixes from ANALYSIS.md §1.8 and §1.13: drop damage, metadata
 * bounds, and the creature-spawn guard.
 */
public class BlockAdvancedMachinesTest {

    @Test
    public void onlyKnownMetadataIsSupported() {
        for (int meta = 0; meta < BlockAdvancedMachines.MACHINE_COUNT; meta++) {
            assertTrue("meta " + meta, BlockAdvancedMachines.supportsMeta(meta));
        }
        assertFalse(BlockAdvancedMachines.supportsMeta(-1));
        assertFalse(BlockAdvancedMachines.supportsMeta(BlockAdvancedMachines.MACHINE_COUNT));
        assertFalse(BlockAdvancedMachines.supportsMeta(99));
    }

    /**
     * §1.8: mining must yield the IC2 advanced machine component (damage 12), not the basic machine
     * casing (damage 0) that the missing damageDropped override produced.
     */
    @Test
    public void miningDropsTheAdvancedMachineCasing() {
        assertEquals(12, BlockAdvancedMachines.droppedDamage(3, false));
        assertEquals(12, BlockAdvancedMachines.droppedDamage(0, false));
    }

    /** When IC2's ignoreWrenchRequirement balance option is on, the machine itself drops. */
    @Test
    public void wrenchRequirementConfigPreservesTheMachineMeta() {
        assertEquals(3, BlockAdvancedMachines.droppedDamage(3, true));
        assertEquals(0, BlockAdvancedMachines.droppedDamage(0, true));
    }

    @Test
    public void blockCanBeConstructedAndBlocksSpawning() {
        BlockAdvancedMachines block = new BlockAdvancedMachines();
        assertNotNull(block);
        assertFalse(block.canCreatureSpawn(null, null, 0, 0, 0));
    }

    @Test
    public void unregisteredIconsReturnNullInsteadOfThrowing() {
        BlockAdvancedMachines block = new BlockAdvancedMachines();
        // iconBuffer is only populated client-side by registerBlockIcons; a corrupt meta must still
        // not throw.
        assertEquals(null, block.getIcon(0, 0));
        assertEquals(null, block.getIcon(0, 99));
    }
}
