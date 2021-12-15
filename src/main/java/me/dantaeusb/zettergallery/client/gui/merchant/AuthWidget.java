package me.dantaeusb.zettergallery.client.gui.merchant;

import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.*;

public class AuthWidget extends AbstractWidget implements Widget, GuiEventListener {

    private static final ResourceLocation LOADING_RESOURCE = new ResourceLocation("zetter", "textures/gui/painting_trade_loading.png");

    protected final PaintingMerchantScreen parentScreen;

    @Nullable
    protected Minecraft minecraft;
    protected ItemRenderer itemRenderer;
    protected Font font;

    static final int WIDTH = 162;
    static final int HEIGHT = 186;

    private int tick = 0;

    private static final Component AUTHENTICATING_TEXT = new TranslatableComponent("container.zetter.paintingMerchant.authenticating");
    private static final Component LOGIN_REQUEST_TEXT = new TranslatableComponent("container.zetter.paintingMerchant.loginRequest");
    private static final Component FETCHING_SALES_TEXT = new TranslatableComponent("container.zetter.paintingMerchant.fetchingSales");
    private static final Component UNKNOWN_ERROR_TEXT = new TranslatableComponent("container.zetter.paintingMerchant.unknownError");
    private static final Component TRY_AGAIN_TEXT = new TranslatableComponent("container.zetter.paintingMerchant.tryAgain");

    public AuthWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(x, y, WIDTH, HEIGHT, new TranslatableComponent("container.zetter.painting.status"));

        this.parentScreen = parentScreen;

        this.minecraft = parentScreen.getMinecraft();
        this.itemRenderer = minecraft.getItemRenderer();
        this.font = minecraft.font;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        switch (this.parentScreen.getState()) {
            case SERVER_AUTHENTICATION:
                // stack, font, x, y, color
                drawCenteredString(matrixStack, this.font, AUTHENTICATING_TEXT, this.x + this.width / 2, this.y + 96, Color.white.getRGB());

                this.drawLoading(matrixStack);
                break;
            case CLIENT_AUTHORIZATION:
                // stack, font, x, y, color
                drawCenteredString(matrixStack, this.font, LOGIN_REQUEST_TEXT, this.x + this.width / 2, this.y + 96, Color.white.getRGB());

                this.drawConnectButton(matrixStack, mouseX, mouseY);
                break;
            case FETCHING_SALES:
                drawCenteredString(matrixStack, this.font, FETCHING_SALES_TEXT, this.x + this.width / 2, this.y + 96, Color.white.getRGB());

                this.drawLoading(matrixStack);
                break;
            case ERROR:
                final Component errorMessage = this.parentScreen.getMenu().getError() != null ?
                        Component.nullToEmpty(this.parentScreen.getMenu().getError()) :
                        UNKNOWN_ERROR_TEXT;

                // stack, font, x, y, color
                drawCenteredString(matrixStack, this.font, errorMessage, this.x + this.width / 2, this.y + 64, Color.white.getRGB());
                drawCenteredString(matrixStack, this.font, TRY_AGAIN_TEXT, this.x + this.width / 2, this.y + 78, Color.white.getRGB());

                break;
        }
    }

    public void tick() {
        this.tick++;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.parentScreen.getState() == PaintingMerchantMenu.State.CLIENT_AUTHORIZATION &&
            isPointInRegion(BUTTON_XPOS, BUTTON_YPOS, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)
        ) {
            this.playDownSound(this.minecraft.getSoundManager());
            this.parentScreen.openAuthenticationLink();
        }

        return false;
    }

    private static final int LOADING_WIDTH = 16;
    private static final int LOADING_HEIGHT = 10;
    private static final int LOADING_XPOS = (WIDTH / 2) - (LOADING_WIDTH / 2);
    private static final int LOADING_YPOS = 78;
    private static final int LOADING_UPOS = 276;
    private static final int LOADING_VPOS = 0;

    private void drawLoading(PoseStack matrixStack) {

        this.minecraft.getTextureManager().bindForSetup(LOADING_RESOURCE);

        final int animation = this.tick % 40;
        int frame = animation / 10; // 0-3

        frame = frame > 2 ? 1 : frame; // 3rd frame is the same as 1st frame

        blit(matrixStack, this.x + LOADING_XPOS, this.y + LOADING_YPOS, LOADING_UPOS, LOADING_VPOS + LOADING_HEIGHT * frame, LOADING_WIDTH, LOADING_HEIGHT, 256, 256);
    }

    private static final int BUTTON_WIDTH = 64;
    private static final int BUTTON_HEIGHT = 14;

    private static final int BUTTON_XPOS = (WIDTH / 2) - (BUTTON_WIDTH / 2);
    private static final int BUTTON_YPOS = 115;

    /**
     * Renders connect button
     * @todo: use native button widget
     * @param matrixStack
     * @param mouseX
     * @param mouseY
     */
    private void drawConnectButton(PoseStack matrixStack, int mouseX, int mouseY) {
        final int BUTTON_UPOS = 176;
        final int BUTTON_VPOS = 0;

        this.minecraft.getTextureManager().bindForSetup(LOADING_RESOURCE);

        if (isPointInRegion(BUTTON_XPOS, BUTTON_YPOS, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            blit(matrixStack, this.x + BUTTON_XPOS, this.y + BUTTON_YPOS, BUTTON_UPOS, BUTTON_VPOS + BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, 256, 256);
        } else {
            blit(matrixStack, this.x + BUTTON_XPOS, this.y + BUTTON_YPOS, BUTTON_UPOS, BUTTON_VPOS, BUTTON_WIDTH, BUTTON_HEIGHT, 256, 256);
        }
    }

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
