package me.dantaeusb.zettergallery.client.gui.merchant;

import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
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
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class PreviewWidget extends AbstractWidget implements Widget, GuiEventListener {
    private static final ResourceLocation READY_RESOURCE = new ResourceLocation(ZetterGallery.MOD_ID, "textures/gui/painting_trade.png");

    protected final PaintingMerchantScreen parentScreen;

    @Nullable
    protected Minecraft minecraft;
    protected ItemRenderer itemRenderer;
    protected Font font;

    static final int WIDTH = 92;
    static final int HEIGHT = 78;

    public PreviewWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(x, y, WIDTH, HEIGHT, Component.translatable("container.zettergallery.merchant.preview"));

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
        RenderSystem.setShaderTexture(0, READY_RESOURCE);

        if (this.canSelect()) {
            // Left arrow
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

            // Right arrow
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
        } else {
            blit(
                    matrixStack,
                    this.x + PREV_OFFER_BUTTON_XPOS,
                    this.y + PREV_OFFER_BUTTON_YPOS,
                    PREV_OFFER_BUTTON_UPOS,
                    PREV_OFFER_BUTTON_VPOS + OFFER_BUTTON_HEIGHT * 2,
                    OFFER_BUTTON_WIDTH,
                    OFFER_BUTTON_HEIGHT,
                    256,
                    256
            );

            blit(
                    matrixStack,
                    this.x + NEXT_OFFER_BUTTON_XPOS,
                    this.y + NEXT_OFFER_BUTTON_YPOS,
                    NEXT_OFFER_BUTTON_UPOS,
                    NEXT_OFFER_BUTTON_VPOS + OFFER_BUTTON_HEIGHT * 2,
                    OFFER_BUTTON_WIDTH,
                    OFFER_BUTTON_HEIGHT,
                    256,
                    256
            );
        }
    }

    private static final int COUNT_TEXT_YPOS = 2;

    private void renderOffersCount(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        PaintingMerchantOffer offer = this.parentScreen.getCurrentOffer();

        if (offer.isSaleOffer()) {
            drawCenteredString(matrixStack, this.font, Component.translatable("container.zettergallery.merchant.sell"), this.x + this.width / 2, this.y + COUNT_TEXT_YPOS, Color.white.getRGB());
        } else {
            int currentOffer = this.parentScreen.getCurrentOfferIndex() + 1;
            int offersCount = this.parentScreen.getOffersCount();

            drawCenteredString(matrixStack, this.font, currentOffer + "/" + offersCount, this.x + this.width / 2, this.y + COUNT_TEXT_YPOS, Color.white.getRGB());
        }
    }

    private void renderOfferPainting(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        final int PAINTING_PREVIEW_OFFSET_X = 14;
        final int PAINTING_PREVIEW_OFFSET_Y = 12;

        PaintingMerchantOffer offer = this.parentScreen.getCurrentOffer();
        if (offer == null) {
            return;
        }

        String canvasCode = offer.getCanvasCode();

        if (offer.getPaintingData().isPresent()) {
            PaintingData offerPaintingData = offer.getPaintingData().get();

            float maxSize = Math.max(offerPaintingData.getHeight(), offerPaintingData.getWidth()) / 16.0F;
            float scale = 4.0F / maxSize;

            final float scaledWidth = offerPaintingData.getWidth() * scale;
            final float scaledHeight = offerPaintingData.getHeight() * scale;

            float aspectRatio = scaledWidth / scaledHeight;
            int offsetX = PAINTING_PREVIEW_OFFSET_X;
            int offsetY = PAINTING_PREVIEW_OFFSET_Y;

            if (aspectRatio > 1.0F) {
                offsetY += Math.round((64.0F - scaledHeight) / 2.0F);
            } else if (aspectRatio < 1.0F) {
                offsetX += Math.round((64.0F - scaledWidth) / 2.0F);
            }

            matrixStack.pushPose();
            matrixStack.translate(this.x + offsetX, this.y + offsetY, 1.0F);
            matrixStack.scale(scale, scale, 1.0F);

            MultiBufferSource.BufferSource renderBuffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            // @todo: make code with uuid
            CanvasRenderer.getInstance().renderCanvas(matrixStack, renderBuffers, canvasCode, offerPaintingData, 0xF000F0);
            renderBuffers.endBatch();

            matrixStack.popPose();
        } else {
            // @todo: different type for zetter painting
            AbstractCanvasData.Type type = offer.isSaleOffer() ? AbstractCanvasData.Type.PAINTING : AbstractCanvasData.Type.PAINTING;
            CanvasRenderer.getInstance().queueCanvasTextureUpdate(type, canvasCode);
        }
    }

    private boolean canSelect() {
        return this.parentScreen.getOffersCount() > 1 && !this.parentScreen.getCurrentOffer().isSaleOffer();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.canSelect()) {
            if (isPointInRegion(PREV_OFFER_BUTTON_XPOS, PREV_OFFER_BUTTON_YPOS, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT, mouseX, mouseY)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.parentScreen.prevOffer();
            }

            if (isPointInRegion(NEXT_OFFER_BUTTON_XPOS, NEXT_OFFER_BUTTON_YPOS, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT, mouseX, mouseY)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.parentScreen.nextOffer();
            }
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
