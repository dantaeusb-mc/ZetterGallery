package me.dantaeusb.zettergallery.client.gui.merchant;

import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class PaintingPreviewWidget extends AbstractPaintingMerchantWidget {
    private static final Component LOADING_TEXT = Component.translatable("container.zettergallery.merchant.preview.loading");

    static final int WIDTH = 64;
    static final int HEIGHT = 64;

    public PaintingPreviewWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, Component.translatable("container.zettergallery.merchant.preview"));
    }

    @Override
    public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        PaintingMerchantOffer<?> offer = this.parentScreen.getCurrentOffer();

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

            CanvasRenderer.getInstance().renderCanvas(matrixStack, renderBuffers, canvasCode, offerPaintingData, 0xF000F0);
            renderBuffers.endBatch();

            matrixStack.popPose();
        } else {
            CanvasRenderer.getInstance().queueCanvasTextureUpdate(canvasCode);
        }
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        PaintingMerchantOffer<?> offer = this.parentScreen.getCurrentOffer();

        if (offer == null) {
            return null;
        }

        if (offer.getPaintingData().isEmpty()) {
            return LOADING_TEXT;
        }

        return null;
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
