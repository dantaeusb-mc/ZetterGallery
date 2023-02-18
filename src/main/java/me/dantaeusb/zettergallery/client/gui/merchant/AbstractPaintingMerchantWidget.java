package me.dantaeusb.zettergallery.client.gui.merchant;

import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

abstract public class AbstractPaintingMerchantWidget extends Widget implements IGuiEventListener {
    protected final PaintingMerchantScreen parentScreen;

    public AbstractPaintingMerchantWidget(PaintingMerchantScreen parentScreen, int x, int y, int width, int height, ITextComponent title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
    }

    public @Nullable ITextComponent getTooltip(int mouseX, int mouseY) {
        return this.getMessage();
    }
}
