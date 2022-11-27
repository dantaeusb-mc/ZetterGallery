package me.dantaeusb.zettergallery.client.gui.merchant;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zettergallery.menu.paintingmerchant.MerchantAuthorizationController;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;

public class AuthWidget extends AbstractPaintingMerchantWidget {
    @Nullable
    protected Minecraft minecraft;
    protected ItemRenderer itemRenderer;
    protected Font font;

    static final int WIDTH = 98;
    static final int HEIGHT = 26;

    private static final int AUTH_UPOS = 208;
    private static final int AUTH_VPOS = 50;

    private int tick = 0;

    private static final Component AUTHENTICATING_TEXT = Component.translatable("container.zettergallery.merchant.authenticating");
    private static final Component LOGIN_TEXT = Component.translatable("container.zettergallery.merchant.login_request");
    private static final Component UNKNOWN_ERROR_TEXT = Component.translatable("container.zettergallery.merchant.unknown_error");
    private static final Component TRY_AGAIN_TEXT = Component.translatable("container.zettergallery.merchant.try_again");

    private static final Component LOGIN_TOOLTIP_TEXT = Component.translatable("container.zettergallery.merchant.login_request_tooltip");
    private static final Component WAITING_TOOLTIP_TEXT = Component.translatable("container.zettergallery.merchant.login_waiting");

    public AuthWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, Component.translatable("container.zetter.painting.status"));

        this.minecraft = parentScreen.getMinecraft();
        this.itemRenderer = minecraft.getItemRenderer();
        this.font = minecraft.font;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderTexture(0, PaintingMerchantScreen.GUI_TEXTURE_RESOURCE);

        switch (this.parentScreen.getPlayerAuthorizationState()) {
            case SERVER_AUTHENTICATION:
                blit(
                        matrixStack,
                        this.x,
                        this.y,
                        AUTH_UPOS,
                        AUTH_VPOS + HEIGHT * 2,
                        WIDTH,
                        HEIGHT,
                        512,
                        256
                );

                this.drawLoading(matrixStack);
                this.font.draw(matrixStack, AUTHENTICATING_TEXT, this.x + 32.0F, this.y + 8.0F, Color.white.getRGB());

                break;
            case CLIENT_AUTHORIZATION:
                if (!isPointInRegion(0, 0, WIDTH, HEIGHT, mouseX, mouseY)) {
                    blit(
                            matrixStack,
                            this.x,
                            this.y,
                            AUTH_UPOS,
                            AUTH_VPOS,
                            WIDTH,
                            HEIGHT,
                            512,
                            256
                    );
                } else {
                    blit(
                            matrixStack,
                            this.x,
                            this.y,
                            AUTH_UPOS,
                            AUTH_VPOS + HEIGHT,
                            WIDTH,
                            HEIGHT,
                            512,
                            256
                    );
                }

                // stack, font, x, y, color
                drawCenteredString(matrixStack, this.font, LOGIN_TEXT, this.x + this.width / 2, this.y + 10, Color.white.getRGB());
                break;
            case LOGGED_IN:
                blit(
                        matrixStack,
                        this.x,
                        this.y,
                        AUTH_UPOS,
                        AUTH_VPOS + HEIGHT * 2,
                        WIDTH,
                        HEIGHT,
                        512,
                        256
                );

                // @todo: draw player-client info
                this.font.draw(matrixStack, this.parentScreen.getAuthorizedPlayerNickname(), this.x + 26.0F, this.y + 9.0F, Color.white.getRGB());
                break;
            case ERROR:
                blit(
                        matrixStack,
                        this.x,
                        this.y,
                        AUTH_UPOS,
                        AUTH_VPOS + HEIGHT * 2,
                        WIDTH,
                        HEIGHT,
                        512,
                        256
                );

                final Component errorMessage = this.parentScreen.getMenu().getAuthController().hasError() ?
                        Component.nullToEmpty(this.parentScreen.getMenu().getAuthController().getError().getMessage()) :
                        UNKNOWN_ERROR_TEXT;

                List<FormattedCharSequence> lines = this.font.split(errorMessage, 92);
                int i = 0;
                int yPos = 4;

                if (lines.size() == 1) {
                    yPos += 6;
                }

                for (FormattedCharSequence line : lines) {
                    // stack, font, x, y, color
                    drawCenteredString(
                            matrixStack,
                            this.font,
                            line,
                            this.x + this.width / 2,
                            this.y + yPos + (i++ * 10),
                            Color.white.getRGB()
                    );

                    if (i == 2) {
                        break;
                    }
                }

                break;
        }
    }

    public void tick() {
        this.tick++;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.parentScreen.getPlayerAuthorizationState() == MerchantAuthorizationController.PlayerAuthorizationState.CLIENT_AUTHORIZATION &&
            isPointInRegion(0, 0, WIDTH, HEIGHT, mouseX, mouseY)
        ) {
            this.playDownSound(this.minecraft.getSoundManager());
            this.parentScreen.openAuthenticationLink();
        }

        return false;
    }

    private static final int LOADING_WIDTH = 16;
    private static final int LOADING_HEIGHT = 10;
    private static final int LOADING_XPOS = 8;
    private static final int LOADING_YPOS = 8;
    private static final int LOADING_UPOS = 208;
    private static final int LOADING_VPOS = 20;

    private void drawLoading(PoseStack matrixStack)
    {
        final int animation = this.tick % 40;
        int frame = animation / 10; // 0-3

        frame = frame > 2 ? 1 : frame; // 3rd frame is the same as 1st frame

        blit(matrixStack, this.x + LOADING_XPOS, this.y + LOADING_YPOS, LOADING_UPOS, LOADING_VPOS + LOADING_HEIGHT * frame, LOADING_WIDTH, LOADING_HEIGHT, 512, 256);
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        switch (this.parentScreen.getPlayerAuthorizationState()) {
            case SERVER_AUTHENTICATION:
                return WAITING_TOOLTIP_TEXT;
            case CLIENT_AUTHORIZATION:
                return LOGIN_TOOLTIP_TEXT;
            case LOGGED_IN:
                return Component.translatable("container.zettergallery.merchant.logged_in_player_tooltip", this.parentScreen.getAuthorizedPlayerNickname());
            case ERROR:
                return null;
        }

        return super.getTooltip(mouseX, mouseY);
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
