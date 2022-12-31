package me.dantaeusb.zettergallery.client.gui.merchant;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.storage.DummyCanvasData;
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

    private int tick = 0;

    public PaintingPreviewWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, Component.translatable("container.zettergallery.merchant.preview"));
    }

    @Override
    public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        PaintingMerchantOffer offer = this.parentScreen.getCurrentOffer();

        if (offer == null || offer.isError()) {
            return;
        }

        if (this.isLoading()) {
            this.renderLoading(matrixStack);
            return;
        }

        String canvasCode = offer.getDummyCanvasCode();
        DummyCanvasData offerPaintingData = offer.getDummyPaintingData();

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
    }

    private static final int LOADING_WIDTH = 16;
    private static final int LOADING_HEIGHT = 10;
    private static final int LOADING_UPOS = 208;
    private static final int LOADING_VPOS = 20;

    private void renderLoading(PoseStack matrixStack)
    {
        RenderSystem.setShaderTexture(0, PaintingMerchantScreen.GUI_TEXTURE_RESOURCE);

        final int animation = this.tick % 40;
        int frame = animation / 10; // 0-3

        frame = frame > 2 ? 1 : frame; // 3rd frame is the same as 1st frame

        blit(matrixStack, this.x + (this.width - LOADING_WIDTH) / 2, this.y + (this.height - LOADING_HEIGHT) / 2, LOADING_UPOS, LOADING_VPOS + LOADING_HEIGHT * frame, LOADING_WIDTH, LOADING_HEIGHT, 512, 256);
    }

    public boolean isLoading() {
        if (this.parentScreen.getCurrentOffer() == null) {
            return false;
        }

        return this.parentScreen.getCurrentOffer().isLoading();
    }

    public void tick() {
        this.tick++;
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        PaintingMerchantOffer offer = this.parentScreen.getCurrentOffer();

        if (offer == null) {
            return null;
        }

        if (offer.isLoading()) {
            return LOADING_TEXT;
        }

        return null;
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
