package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Guards the block-metadata ↔ tile-entity wiring. The ore washer (meta 4) is skipped because its
 * superclass constructor needs the FML fluid/block registry, which is unavailable in a plain JVM.
 */
public class BlockRegistrationTest {

    @Test
    public void metadataMapsToTheRightTileEntity() {
        BlockAdvancedMachines block = new BlockAdvancedMachines();

        assertTrue(block.createTileEntity(null, 0) instanceof TileEntityAdvancedMacerator);
        assertTrue(block.createTileEntity(null, 1) instanceof TileEntityAdvancedCompressor);
        assertTrue(block.createTileEntity(null, 2) instanceof TileEntityAdvancedExtractor);
        assertTrue(block.createTileEntity(null, 3) instanceof TileEntityAdvancedRecycler);
    }

    @Test
    public void unknownMetadataHasNoTileEntity() {
        BlockAdvancedMachines block = new BlockAdvancedMachines();

        assertNull(block.createTileEntity(null, 5));
        assertNull(block.createTileEntity(null, 99));
        assertNull(block.createTileEntity(null, -1));
    }

    @Test
    public void classicCreateNewTileEntityIsUnused() {
        assertNull(new BlockAdvancedMachines().createNewTileEntity(null, 0));
    }
}
