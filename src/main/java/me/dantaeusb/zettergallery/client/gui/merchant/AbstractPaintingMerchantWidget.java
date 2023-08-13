package me.dantaeusb.zettergallery.client.gui.merchant;

import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

abstract public class AbstractPaintingMerchantWidget extends AbstractWidget implements GuiEventListener {
    protected final PaintingMerchantScreen parentScreen;

    public AbstractPaintingMerchantWidget(PaintingMerchantScreen parentScreen, int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
    }

    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        return this.getMessage();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }
}
