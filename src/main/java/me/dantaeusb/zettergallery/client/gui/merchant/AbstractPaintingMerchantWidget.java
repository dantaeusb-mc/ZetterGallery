package me.dantaeusb.zettergallery.client.gui.merchant;

import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

abstract public class AbstractPaintingMerchantWidget extends AbstractWidget implements GuiEventListener {
    protected final PaintingMerchantScreen parentScreen;

    public AbstractPaintingMerchantWidget(PaintingMerchantScreen parentScreen, int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
    }

    public void renderTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        Component tooltip = this.getTooltip(x, y);

        if (tooltip != null) {
            List<FormattedCharSequence> tooltipLines = this.parentScreen.getFont().split(tooltip, 120);
            guiGraphics.renderTooltip(this.parentScreen.getFont(), tooltipLines, x, y);
        }
    }

    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        return this.getMessage();
    }
}
