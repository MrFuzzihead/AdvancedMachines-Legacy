package atomicstryker.ic2.advancedmachines.client;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import atomicstryker.ic2.advancedmachines.ContainerAdvancedOreWasher;
import atomicstryker.ic2.advancedmachines.IAdvancedMachine;
import ic2.core.block.machine.tileentity.TileEntityOreWashing;
import ic2.core.util.DrawUtil;
import ic2.core.util.GuiTooltipHelper;

public class GuiRotaryOreWasher extends GuiAdvancedMachine<ContainerAdvancedOreWasher> {

    private static final ResourceLocation TEX = new ResourceLocation(
        "advancedmachines",
        "textures/gui/GUIOreWashingPlant.png");

    public GuiRotaryOreWasher(ContainerAdvancedOreWasher container) {
        super(container, TEX);
    }

    @Override
    protected String getTitle() {
        return StatCollector.translateToLocal("item.advancedmachines:rotaryOreWasher.name");
    }

    @Override
    protected void drawMachineForeground(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(StatCollector.translateToLocal("advancedmachines.gui.speed"), 4, 36, 4210752);
        this.fontRendererObj.drawString(((IAdvancedMachine) container.base).printFormattedData(), 10, 44, 4210752);

        FluidStack fluidstack = ((TileEntityOreWashing) container.base).getFluidStackfromTank();
        if (fluidstack != null && fluidstack.getFluid() != null) {
            String tooltip = localizeFluid(fluidstack) + ": "
                + fluidstack.amount
                + StatCollector.translateToLocal("ic2.generic.text.mb");
            GuiTooltipHelper.drawAreaTooltip(mouseX - this.guiLeft, mouseY - this.guiTop, tooltip, 63, 23, 76, 71);
        }
    }

    @Override
    protected void drawMachineBackground(int x, int y, float partialTicks) {
        int chargeLevel = (int) (14.0F * ((TileEntityOreWashing) container.base).getChargeLevel());
        int progress = (int) (20.0F * ((TileEntityOreWashing) container.base).getProgress());

        if (chargeLevel > 0) {
            drawTexturedModalRect(x + 9, y + 57 - chargeLevel, 176, 14 - chargeLevel, 14, chargeLevel);
        }
        if (progress > 0) {
            drawTexturedModalRect(x + 102, y + 38, 176, 117, progress + 1, 20);
        }

        TileEntityOreWashing te = (TileEntityOreWashing) container.base;
        if (te.getTankAmount() > 0 && te.getFluidTank()
            .getFluid() != null
            && te.getFluidTank()
                .getFluid()
                .getFluid() != null) {
            IIcon fluidIcon = te.getFluidTank()
                .getFluid()
                .getFluid()
                .getIcon();

            if (fluidIcon != null) {
                drawTexturedModalRect(x + 60, y + 20, 176, 15, 20, 55);

                this.mc.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
                int liquidHeight = te.gaugeLiquidScaled(47);
                DrawUtil.drawRepeated(fluidIcon, x + 64, y + 24 + 47 - liquidHeight, 12.0D, liquidHeight, this.zLevel);

                this.mc.renderEngine.bindTexture(TEX);
                drawTexturedModalRect(x + 64, y + 24, 176, 70, 12, 47);
            }
        }
    }

    /**
     * 1.7.10 has no vanilla/IC2 translation for fluid names (neither {@code fluid.water} nor
     * {@code ic2.water} exist), so use a translation when a resource pack provides one and otherwise
     * fall back to the capitalised raw name instead of printing the untranslated key.
     */
    private static String localizeFluid(FluidStack stack) {
        String key = stack.getUnlocalizedName();
        if (key != null && StatCollector.canTranslate(key)) {
            return StatCollector.translateToLocal(key);
        }
        String raw = stack.getFluid()
            .getName();
        if (raw == null || raw.isEmpty()) {
            return key == null ? "" : key;
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
