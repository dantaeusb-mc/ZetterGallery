package me.dantaeusb.zettergallery.client.gui.merchant;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;

public class RefreshWidget extends AbstractPaintingMerchantWidget {
    private static final int WIDTH = 46;
    private static final int HEIGHT = 18;

    private static final int REFRESH_BUTTON_WIDTH = 18;
    private static final int REFRESH_BUTTON_HEIGHT = 18;

    private static final int REFRESH_BUTTON_XPOS = WIDTH - REFRESH_BUTTON_WIDTH;
    private static final int REFRESH_BUTTON_YPOS = 0;

    private static final int REFRESH_BUTTON_UPOS = 304;
    private static final int REFRESH_BUTTON_VPOS = 16;

    protected Minecraft minecraft;
    protected Font font;

    public RefreshWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, new TranslatableComponent("container.zettergallery.merchant.refresh"));

        this.minecraft = parentScreen.getMinecraft();
        this.font = minecraft.font;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (this.isLoading()) {
            return;
        }

        RenderSystem.setShaderTexture(0, PaintingMerchantScreen.GUI_TEXTURE_RESOURCE);

        if (this.canUpdate()) {
            // Left (down) arrow
            if (isPointInRegion(REFRESH_BUTTON_XPOS, REFRESH_BUTTON_YPOS, REFRESH_BUTTON_WIDTH, REFRESH_BUTTON_HEIGHT, mouseX, mouseY)) {
                blit(
                    matrixStack,
                    this.x + REFRESH_BUTTON_XPOS,
                    this.y + REFRESH_BUTTON_YPOS,
                    REFRESH_BUTTON_UPOS + REFRESH_BUTTON_WIDTH * 2,
                    REFRESH_BUTTON_VPOS,
                    REFRESH_BUTTON_WIDTH,
                    REFRESH_BUTTON_HEIGHT,
                    512,
                    256
                );
            } else {
                blit(
                    matrixStack,
                    this.x + REFRESH_BUTTON_XPOS,
                    this.y + REFRESH_BUTTON_YPOS,
                    REFRESH_BUTTON_UPOS + REFRESH_BUTTON_WIDTH,
                    REFRESH_BUTTON_VPOS,
                    REFRESH_BUTTON_WIDTH,
                    REFRESH_BUTTON_HEIGHT,
                    512,
                    256
                );
            }
        } else {
            blit(
                matrixStack,
                this.x + REFRESH_BUTTON_XPOS,
                this.y + REFRESH_BUTTON_YPOS,
                REFRESH_BUTTON_UPOS,
                REFRESH_BUTTON_VPOS,
                REFRESH_BUTTON_WIDTH,
                REFRESH_BUTTON_HEIGHT,
                512,
                256
            );
        }

        final String timeToUpdate = this.getUpdateTimeout();
        final int timerWidth = this.font.width(timeToUpdate);
        this.font.draw(matrixStack, timeToUpdate, this.x + WIDTH - REFRESH_BUTTON_WIDTH - timerWidth - 2, this.y + 5, Color.gray.getRGB());
    }

    public String getUpdateTimeout() {
        final int totalSecondsToUpdate = this.canUpdate() ?
            this.parentScreen.getMenu().getContainer().getSecondsToForceUpdateCycle()
            : this.parentScreen.getMenu().getContainer().getSecondsToNextCycle();

        if (totalSecondsToUpdate < 0) {
            return "-:--";
        }

        int minutesToUpdate = totalSecondsToUpdate / 60;
        int secondsToUpdate = totalSecondsToUpdate % 60;

        return String.format("%d:%02d", minutesToUpdate, secondsToUpdate);
    }

    public boolean isLoading() {
        if (this.parentScreen.getCurrentOffer() == null) {
            return false;
        }

        return this.parentScreen.getCurrentOffer().isLoading();
    }

    /**
     * The offers list is circled, so if we're at the last painting,
     * next will be 0, so we use only one parameter for both buttons
     *
     * @return
     */
    private boolean canUpdate() {
        return this.parentScreen.getMenu().getContainer().canUpdate();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.canUpdate()) {
            if (isPointInRegion(REFRESH_BUTTON_XPOS, REFRESH_BUTTON_YPOS, REFRESH_BUTTON_WIDTH, REFRESH_BUTTON_HEIGHT, mouseX, mouseY)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.parentScreen.requestNewOffers();
            }
        }

        return false;
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        if (this.isLoading()) {
            return null;
        }

        if (this.parentScreen.getMenu().getContainer().hasError()) {
            return null;
        }

        if (this.canUpdate()) {
            return new TranslatableComponent("container.zettergallery.merchant.refresh.available", this.getUpdateTimeout());
        } else {
            return new TranslatableComponent("container.zettergallery.merchant.refresh.before", this.getUpdateTimeout());
        }
    }

    /**
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
