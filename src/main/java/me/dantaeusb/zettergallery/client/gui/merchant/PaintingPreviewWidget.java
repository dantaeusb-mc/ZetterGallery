package me.dantaeusb.zettergallery.client.gui.merchant;

import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class PaintingPreviewWidget extends AbstractWidget implements Widget, GuiEventListener {
    protected final PaintingMerchantScreen parentScreen;

    @Nullable
    protected Minecraft minecraft;
    protected ItemRenderer itemRenderer;
    protected Font font;

    static final int WIDTH = 92;
    static final int HEIGHT = 78;

    public PaintingPreviewWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(x, y, WIDTH, HEIGHT, Component.translatable("container.zettergallery.merchant.preview"));

        this.parentScreen = parentScreen;

        this.minecraft = parentScreen.getMinecraft();
        this.itemRenderer = minecraft.getItemRenderer();
        this.font = minecraft.font;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        //this.renderOffersCount(matrixStack, mouseX, mouseY, partialTicks);
        this.renderOfferPainting(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void renderOfferPainting(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        PaintingMerchantOffer offer = this.parentScreen.getCurrentOffer();

        if (offer == null) {
            // @todo: show loading if ready
            return;
        }

        String canvasCode = offer.getCanvasCode();

        if (offer.getPaintingData().isPresent()) {
            PaintingData offerPaintingData = (PaintingData) offer.getPaintingData().get();

            float maxSize = Math.max(offerPaintingData.getHeight(), offerPaintingData.getWidth()) / 16.0F;
            float scale = 4.0F / maxSize;

            final float scaledWidth = offerPaintingData.getWidth() * scale;
            final float scaledHeight = offerPaintingData.getHeight() * scale;

            float aspectRatio = scaledWidth / scaledHeight;
            int offsetX = 0;
            int offsetY = 0;

            if (aspectRatio > 1.0F) {
                offsetY += Math.round((64.0F - scaledHeight) / 2.0F);
            } else if (aspectRatio < 1.0F) {
                offsetX += Math.round((64.0F - scaledWidth) / 2.0F);
            }

            matrixStack.pushPose();
            matrixStack.translate(this.x + offsetX, this.y + offsetY, 1.0F);
            matrixStack.scale(scale, scale, 1.0F);

            MultiBufferSource.BufferSource renderBuffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            // @todo: [LOW] make code with uuid (wtf does this mean)
            CanvasRenderer.getInstance().renderCanvas(matrixStack, renderBuffers, canvasCode, offerPaintingData, 0xF000F0);
            renderBuffers.endBatch();

            matrixStack.popPose();
        } else {
            CanvasRenderer.getInstance().queueCanvasTextureUpdate(canvasCode);
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
