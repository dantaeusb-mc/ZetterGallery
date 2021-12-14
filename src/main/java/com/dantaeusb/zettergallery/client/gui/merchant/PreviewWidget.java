package com.dantaeusb.zettergallery.client.gui.merchant;

import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import com.dantaeusb.zettergallery.storage.OfferPaintingData;
import com.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
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
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.*;

public class PreviewWidget extends AbstractWidget implements Widget, GuiEventListener {
    private static final ResourceLocation READY_RESOURCE = new ResourceLocation("zetter", "textures/gui/painting_trade.png");

    protected final PaintingMerchantScreen parentScreen;

    @Nullable
    protected Minecraft minecraft;
    protected ItemRenderer itemRenderer;
    protected Font font;

    static final int WIDTH = 92;
    static final int HEIGHT = 78;

    public PreviewWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(x, y, WIDTH, HEIGHT, new TranslatableComponent("container.zetter.painting.preview"));

        this.parentScreen = parentScreen;

        this.minecraft = parentScreen.getMinecraft();
        this.itemRenderer = minecraft.getItemRenderer();
        this.font = minecraft.font;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderOffersCount(matrixStack, mouseX, mouseY, partialTicks);
        this.renderOfferPainting(matrixStack, mouseX, mouseY, partialTicks);
        this.renderOfferButtons(matrixStack, mouseX, mouseY, partialTicks);
    }

    private static final int OFFER_BUTTON_WIDTH = 10;
    private static final int OFFER_BUTTON_HEIGHT = 20;
    private static final int PREV_OFFER_BUTTON_XPOS = 2;
    private static final int PREV_OFFER_BUTTON_YPOS = 34;
    private static final int PREV_OFFER_BUTTON_UPOS = 192;
    private static final int PREV_OFFER_BUTTON_VPOS = 0;
    private static final int NEXT_OFFER_BUTTON_XPOS = 80;
    private static final int NEXT_OFFER_BUTTON_YPOS = 34;
    private static final int NEXT_OFFER_BUTTON_UPOS = 202;
    private static final int NEXT_OFFER_BUTTON_VPOS = 0;

    private void renderOfferButtons(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.minecraft.getTextureManager().bindForSetup(READY_RESOURCE);

        if (isPointInRegion(PREV_OFFER_BUTTON_XPOS, PREV_OFFER_BUTTON_YPOS, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT, mouseX, mouseY)) {
            blit(
                    matrixStack,
                    this.x + PREV_OFFER_BUTTON_XPOS,
                    this.y + PREV_OFFER_BUTTON_YPOS,
                    PREV_OFFER_BUTTON_UPOS,
                    PREV_OFFER_BUTTON_VPOS + OFFER_BUTTON_HEIGHT,
                    OFFER_BUTTON_WIDTH,
                    OFFER_BUTTON_HEIGHT,
                    256,
                    256
            );
        } else {
            blit(
                    matrixStack,
                    this.x + PREV_OFFER_BUTTON_XPOS,
                    this.y + PREV_OFFER_BUTTON_YPOS,
                    PREV_OFFER_BUTTON_UPOS,
                    PREV_OFFER_BUTTON_VPOS,
                    OFFER_BUTTON_WIDTH,
                    OFFER_BUTTON_HEIGHT,
                    256,
                    256
            );
        }

        if (isPointInRegion(NEXT_OFFER_BUTTON_XPOS, NEXT_OFFER_BUTTON_YPOS, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT, mouseX, mouseY)) {
            blit(
                    matrixStack,
                    this.x + NEXT_OFFER_BUTTON_XPOS,
                    this.y + NEXT_OFFER_BUTTON_YPOS,
                    NEXT_OFFER_BUTTON_UPOS,
                    NEXT_OFFER_BUTTON_VPOS + OFFER_BUTTON_HEIGHT,
                    OFFER_BUTTON_WIDTH,
                    OFFER_BUTTON_HEIGHT,
                    256,
                    256
            );
        } else {
            blit(
                    matrixStack,
                    this.x + NEXT_OFFER_BUTTON_XPOS,
                    this.y + NEXT_OFFER_BUTTON_YPOS,
                    NEXT_OFFER_BUTTON_UPOS,
                    NEXT_OFFER_BUTTON_VPOS,
                    OFFER_BUTTON_WIDTH,
                    OFFER_BUTTON_HEIGHT,
                    256,
                    256
            );
        }
    }

    private static final int COUNT_TEXT_YPOS = 2;

    private void renderOffersCount(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int currentOffer = this.parentScreen.getCurrentOfferIndex() + 1;
        int offersCount = this.parentScreen.getOffersCount();

        drawCenteredString(matrixStack, this.font, currentOffer + "/" + offersCount, this.x + this.width / 2, this.y + COUNT_TEXT_YPOS, Color.white.getRGB());
    }

    private void renderOfferPainting(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        PaintingMerchantOffer offer = this.parentScreen.getCurrentOffer();
        if (offer == null) {
            return;
        }

        OfferPaintingData offerPaintingData = offer.getPaintingData();
        if (offerPaintingData == null) {
            return;
        }

        float scale = 4.0F;

        matrixStack.pushPose();
        matrixStack.translate(this.x + 14, this.y + 12, 1.0F);
        matrixStack.scale(scale, scale, 1.0F);

        MultiBufferSource.BufferSource renderBuffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        // @todo: make code with uuid
        CanvasRenderer.getInstance().renderCanvas(matrixStack, renderBuffers, offerPaintingData.getUniqueId().toString(), offerPaintingData, 0xF000F0);
        renderBuffers.endBatch();

        matrixStack.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isPointInRegion(PREV_OFFER_BUTTON_XPOS, PREV_OFFER_BUTTON_YPOS, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT, mouseX, mouseY)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.parentScreen.prevOffer();
        }

        if (isPointInRegion(NEXT_OFFER_BUTTON_XPOS, NEXT_OFFER_BUTTON_YPOS, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT, mouseX, mouseY)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.parentScreen.nextOffer();
        }

        return false;
    }

    /**
     * @see net.minecraft.client.gui.screen.inventory.ContainerScreen#isPointInRegion(int, int, int, int, double, double)
     * @param x
     * @param y
     * @param width
     * @param height
     * @param mouseX
     * @param mouseY
     * @return
     */
    protected boolean isPointInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {
        int i = this.x;
        int j = this.y;
        mouseX = mouseX - (double)i;
        mouseY = mouseY - (double)j;
        return mouseX >= (double)(x - 1) && mouseX < (double)(x + width + 1) && mouseY >= (double)(y - 1) && mouseY < (double)(y + height + 1);
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
