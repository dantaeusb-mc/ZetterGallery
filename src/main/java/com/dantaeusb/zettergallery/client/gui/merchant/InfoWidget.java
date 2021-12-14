package com.dantaeusb.zettergallery.client.gui.merchant;

import com.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import com.dantaeusb.zettergallery.storage.OfferPaintingData;
import com.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.awt.*;

public class InfoWidget extends AbstractWidget implements Widget {
    protected final PaintingMerchantScreen parentScreen;

    @Nullable
    protected Minecraft minecraft;
    protected ItemRenderer itemRenderer;
    protected Font font;

    static final int WIDTH = 160;
    static final int HEIGHT = 32;

    public InfoWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(x, y, WIDTH, HEIGHT, new TranslatableComponent("container.zetter.painting.info"));

        this.parentScreen = parentScreen;

        this.minecraft = parentScreen.getMinecraft();
        this.itemRenderer = minecraft.getItemRenderer();
        this.font = minecraft.font;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        PaintingMerchantOffer offer = this.parentScreen.getCurrentOffer();
        if (offer == null) {
            return;
        }

        OfferPaintingData offerPaintingData = offer.getPaintingData();
        if (offerPaintingData == null) {
            return;
        }

        this.font.drawShadow(matrixStack, offerPaintingData.getPaintingName(), this.x + 8, this.y + 7, Color.white.getRGB());
        this.font.drawShadow(matrixStack, offerPaintingData.getAuthorName(), this.x + 8, this.y + 7 + 11, Color.white.getRGB());

        String priceString = String.valueOf(offer.getPrice());
        final int priceWidth = this.font.width(priceString);

        ItemStack emeraldStack = new ItemStack(Items.EMERALD);

        this.itemRenderer.renderGuiItem(emeraldStack, this.x + this.width - 21, this.y + 8); // 8 padding + 16 texture - 3 emerald item padding
        this.font.draw(matrixStack, priceString, this.x + this.width - 22 - priceWidth, this.y + 12, Color.white.getRGB()); // -21 - 4 padding to text + 3 emerald item padding
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
