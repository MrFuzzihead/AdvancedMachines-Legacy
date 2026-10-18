package atomicstryker.ic2.advancedmachines.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import ic2.core.ContainerBase;
import ic2.core.upgrade.IUpgradableBlock;
import ic2.core.util.GuiTooltipHelper;

/**
 * Shared chrome for the advanced machine GUIs: the machine frame, a centred title, and IC2's
 * compatible-upgrade info button plus its tooltip. Machine-specific sprites and text go in the two
 * hooks so every screen behaves (and looks) like an IC2 machine screen.
 */
public abstract class GuiAdvancedMachine<T extends ContainerBase<?>> extends GuiContainer {

    private static final ResourceLocation INFO_BUTTON = new ResourceLocation("ic2", "textures/gui/infobutton.png");
    private static final int INFO_BUTTON_X = 3;
    private static final int INFO_BUTTON_Y = 3;
    private static final int INFO_BUTTON_SIZE = 10;
    private static final int HOVER_MIN_X = 0;
    private static final int HOVER_MIN_Y = 0;
    private static final int HOVER_MAX_X = 12;
    private static final int HOVER_MAX_Y = 12;

    protected final T container;
    private final ResourceLocation texture;

    protected GuiAdvancedMachine(T container, ResourceLocation texture) {
        super(container);
        this.container = container;
        this.texture = texture;
    }

    /** Localised machine name shown at the top of the GUI. */
    protected abstract String getTitle();

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(texture);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

        drawMachineBackground(x, y, partialTicks);

        // IC2's compatible-upgrade info button, then restore the machine texture.
        this.mc.getTextureManager()
            .bindTexture(INFO_BUTTON);
        drawTexturedModalRect(x + INFO_BUTTON_X, y + INFO_BUTTON_Y, 0, 0, INFO_BUTTON_SIZE, INFO_BUTTON_SIZE);
        this.mc.getTextureManager()
            .bindTexture(texture);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = getTitle();
        this.fontRendererObj
            .drawString(title, (this.xSize - this.fontRendererObj.getStringWidth(title)) / 2, 6, 4210752);
        this.fontRendererObj
            .drawString(StatCollector.translateToLocal("container.inventory"), 8, this.ySize - 96 + 2, 4210752);

        drawMachineForeground(mouseX, mouseY);

        if (container.base instanceof IUpgradableBlock) {
            GuiTooltipHelper.drawUpgradeslotTooltip(
                mouseX - this.guiLeft,
                mouseY - this.guiTop,
                HOVER_MIN_X,
                HOVER_MIN_Y,
                HOVER_MAX_X,
                HOVER_MAX_Y,
                (IUpgradableBlock) container.base,
                25,
                0);
        }
    }

    /** Machine-specific sprites (charge/progress bars, fluid gauge, ...). */
    protected void drawMachineBackground(int x, int y, float partialTicks) {}

    /** Machine-specific text and area tooltips. */
    protected void drawMachineForeground(int mouseX, int mouseY) {}
}
