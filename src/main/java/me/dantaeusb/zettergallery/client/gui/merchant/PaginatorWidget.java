package me.dantaeusb.zettergallery.client.gui.merchant;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class PaginatorWidget extends AbstractWidget implements Widget, GuiEventListener {
    protected final PaintingMerchantScreen parentScreen;

    private static final int WIDTH = 39;
    private static final int HEIGHT = 10;

    private static final int OFFER_BUTTON_WIDTH = 19;
    private static final int OFFER_BUTTON_HEIGHT = 10;
    private static final int PREV_OFFER_BUTTON_XPOS = 0;
    private static final int PREV_OFFER_BUTTON_YPOS = 0;
    private static final int PREV_OFFER_BUTTON_UPOS = 208;
    private static final int PREV_OFFER_BUTTON_VPOS = 0;
    private static final int NEXT_OFFER_BUTTON_XPOS = 20;
    private static final int NEXT_OFFER_BUTTON_YPOS = 0;
    private static final int NEXT_OFFER_BUTTON_UPOS = 208;
    private static final int NEXT_OFFER_BUTTON_VPOS = 10;

    public PaginatorWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(x, y, WIDTH, HEIGHT, Component.translatable("container.zettergallery.merchant.paginator"));

        this.parentScreen = parentScreen;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderTexture(0, PaintingMerchantScreen.GUI_TEXTURE_RESOURCE);

        if (this.canSelect()) {
            // Left (down) arrow
            if (isPointInRegion(PREV_OFFER_BUTTON_XPOS, PREV_OFFER_BUTTON_YPOS, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT, mouseX, mouseY)) {
                blit(
                        matrixStack,
                        this.x + PREV_OFFER_BUTTON_XPOS,
                        this.y + PREV_OFFER_BUTTON_YPOS,
                        PREV_OFFER_BUTTON_UPOS + OFFER_BUTTON_WIDTH * 2,
                        PREV_OFFER_BUTTON_VPOS,
                        OFFER_BUTTON_WIDTH,
                        OFFER_BUTTON_HEIGHT,
                        512,
                        256
                );
            } else {
                blit(
                        matrixStack,
                        this.x + PREV_OFFER_BUTTON_XPOS,
                        this.y + PREV_OFFER_BUTTON_YPOS,
                        PREV_OFFER_BUTTON_UPOS + OFFER_BUTTON_WIDTH,
                        PREV_OFFER_BUTTON_VPOS,
                        OFFER_BUTTON_WIDTH,
                        OFFER_BUTTON_HEIGHT,
                        512,
                        256
                );
            }

            // Right (up) arrow
            if (isPointInRegion(NEXT_OFFER_BUTTON_XPOS, NEXT_OFFER_BUTTON_YPOS, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT, mouseX, mouseY)) {
                blit(
                        matrixStack,
                        this.x + NEXT_OFFER_BUTTON_XPOS,
                        this.y + NEXT_OFFER_BUTTON_YPOS,
                        NEXT_OFFER_BUTTON_UPOS + OFFER_BUTTON_WIDTH * 2,
                        NEXT_OFFER_BUTTON_VPOS,
                        OFFER_BUTTON_WIDTH,
                        OFFER_BUTTON_HEIGHT,
                        512,
                        256
                );
            } else {
                blit(
                        matrixStack,
                        this.x + NEXT_OFFER_BUTTON_XPOS,
                        this.y + NEXT_OFFER_BUTTON_YPOS,
                        NEXT_OFFER_BUTTON_UPOS + OFFER_BUTTON_WIDTH,
                        NEXT_OFFER_BUTTON_VPOS,
                        OFFER_BUTTON_WIDTH,
                        OFFER_BUTTON_HEIGHT,
                        512,
                        256
                );
            }
        } else {
            blit(
                    matrixStack,
                    this.x + PREV_OFFER_BUTTON_XPOS,
                    this.y + PREV_OFFER_BUTTON_YPOS,
                    PREV_OFFER_BUTTON_UPOS,
                    PREV_OFFER_BUTTON_VPOS,
                    OFFER_BUTTON_WIDTH,
                    OFFER_BUTTON_HEIGHT,
                    512,
                    256
            );

            blit(
                    matrixStack,
                    this.x + NEXT_OFFER_BUTTON_XPOS,
                    this.y + NEXT_OFFER_BUTTON_YPOS,
                    NEXT_OFFER_BUTTON_UPOS,
                    NEXT_OFFER_BUTTON_VPOS,
                    OFFER_BUTTON_WIDTH,
                    OFFER_BUTTON_HEIGHT,
                    512,
                    256
            );
        }
    }

    /**
     * The offers list is circled, so if we're at the last painting,
     * next will be 0, so we use only one parameter for both buttons
     *
     * @return
     */
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
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }
}
