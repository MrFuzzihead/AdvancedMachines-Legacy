package atomicstryker.ic2.advancedmachines.client;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import atomicstryker.ic2.advancedmachines.ContainerAdvancedMachine;
import atomicstryker.ic2.advancedmachines.IAdvancedMachine;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;

public class GuiCentrifugeExtractor extends GuiAdvancedMachine<ContainerAdvancedMachine<TileEntityStandardMachine>> {

    private static final ResourceLocation TEX = new ResourceLocation(
        "advancedmachines",
        "textures/gui/GUICentrifuge.png");

    public GuiCentrifugeExtractor(ContainerAdvancedMachine<TileEntityStandardMachine> container) {
        super(container, TEX);
    }

    @Override
    protected String getTitle() {
        return StatCollector.translateToLocal("item.advancedmachines:centrifugeExtractor.name");
    }

    @Override
    protected void drawMachineForeground(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(StatCollector.translateToLocal("advancedmachines.gui.speed"), 4, 36, 4210752);
        this.fontRendererObj.drawString(((IAdvancedMachine) container.base).printFormattedData(), 10, 44, 4210752);
    }

    @Override
    protected void drawMachineBackground(int x, int y, float partialTicks) {
        int chargeLevel = (int) (14.0F * ((TileEntityStandardMachine) container.base).getChargeLevel());
        int progress = (int) (24.0F * ((TileEntityStandardMachine) container.base).getProgress());

        if (chargeLevel > 0) {
            drawTexturedModalRect(x + 56, y + 36 + 14 - chargeLevel, 176, 14 - chargeLevel, 14, chargeLevel);
        }
        if (progress > 0) {
            drawTexturedModalRect(x + 79, y + 34, 176, 14, progress + 1, 16);
        }
    }
}
