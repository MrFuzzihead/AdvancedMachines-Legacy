package atomicstryker.ic2.advancedmachines.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import atomicstryker.ic2.advancedmachines.ContainerAdvancedMachine;
import atomicstryker.ic2.advancedmachines.ContainerAdvancedOreWasher;
import atomicstryker.ic2.advancedmachines.IProxy;
import atomicstryker.ic2.advancedmachines.TileEntityAdvancedCompressor;
import atomicstryker.ic2.advancedmachines.TileEntityAdvancedExtractor;
import atomicstryker.ic2.advancedmachines.TileEntityAdvancedMacerator;
import atomicstryker.ic2.advancedmachines.TileEntityAdvancedOreWasher;
import atomicstryker.ic2.advancedmachines.TileEntityAdvancedRecycler;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;

public class AdvancedMachinesClient implements IProxy {

    @Override
    public void load() {
        // NOOP: face/back texture selection is handled by IC2's BlockBase#getTextureSubIndex.
    }

    @Override
    public Object getGuiElementForClient(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null) {
            if (te instanceof TileEntityAdvancedMacerator) {
                return new GuiRotaryMacerator(
                    new ContainerAdvancedMachine<TileEntityStandardMachine>(player, (TileEntityAdvancedMacerator) te));
            } else if (te instanceof TileEntityAdvancedExtractor) {
                return new GuiCentrifugeExtractor(
                    new ContainerAdvancedMachine<TileEntityStandardMachine>(player, (TileEntityAdvancedExtractor) te));
            } else if (te instanceof TileEntityAdvancedCompressor) {
                return new GuiSingularityCompressor(
                    new ContainerAdvancedMachine<TileEntityStandardMachine>(player, (TileEntityAdvancedCompressor) te));
            } else if (te instanceof TileEntityAdvancedRecycler) {
                return new GuiCombinedRecycler(
                    new ContainerAdvancedMachine<TileEntityStandardMachine>(player, (TileEntityAdvancedRecycler) te));
            } else if (te instanceof TileEntityAdvancedOreWasher) {
                return new GuiRotaryOreWasher(new ContainerAdvancedOreWasher(player, (TileEntityAdvancedOreWasher) te));
            }
        }

        return null;
    }

}
