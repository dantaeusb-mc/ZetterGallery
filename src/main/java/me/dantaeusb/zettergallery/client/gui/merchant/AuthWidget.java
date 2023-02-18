package me.dantaeusb.zettergallery.client.gui.merchant;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import me.dantaeusb.zettergallery.core.ClientHelper;
import me.dantaeusb.zettergallery.menu.paintingmerchant.MerchantAuthorizationController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.List;

public class AuthWidget extends AbstractPaintingMerchantWidget {
    @Nullable
    protected Minecraft minecraft;
    protected ItemRenderer itemRenderer;
    protected FontRenderer font;

    static final int WIDTH = 98;
    static final int HEIGHT = 26;

    private static final int AUTH_UPOS = 208;
    private static final int AUTH_VPOS = 50;

    private int tick = 0;

    private static final ITextComponent AUTHENTICATING_TEXT = new TranslationTextComponent("container.zettergallery.merchant.authenticating");
    private static final ITextComponent LOGIN_TEXT = new TranslationTextComponent("container.zettergallery.merchant.login_request");
    private static final ITextComponent UNKNOWN_ERROR_TEXT = new TranslationTextComponent("container.zettergallery.merchant.unknown_error");
    private static final ITextComponent TRY_AGAIN_TEXT = new TranslationTextComponent("container.zettergallery.merchant.try_again");
    private static final ITextComponent DISABLED_TEXT = new TranslationTextComponent("container.zettergallery.merchant.login_disabled");

    private static final ITextComponent LINKS_DISABLED_TOOLTIP_TEXT = new TranslationTextComponent("container.zettergallery.merchant.links_disabled_tooltip");
    private static final ITextComponent LOGIN_TOOLTIP_TEXT = new TranslationTextComponent("container.zettergallery.merchant.login_request_tooltip");
    private static final ITextComponent WAITING_TOOLTIP_TEXT = new TranslationTextComponent("container.zettergallery.merchant.login_waiting");

    public AuthWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, new TranslationTextComponent("container.zetter.painting.status"));

        this.minecraft = parentScreen.getMinecraft();
        this.itemRenderer = this.minecraft.getItemRenderer();
        this.font = minecraft.font;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.parentScreen.getMinecraft().getTextureManager().bind(PaintingMerchantScreen.GUI_TEXTURE_RESOURCE);

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

                this.renderLoading(matrixStack);
                this.font.draw(matrixStack, AUTHENTICATING_TEXT, this.x + 32.0F, this.y + 8.0F, Color.white.getRGB());

                break;
            case CLIENT_AUTHORIZATION:
                if (!ClientHelper.openUriAllowed()) {
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

                    drawCenteredString(matrixStack, this.font, DISABLED_TEXT, this.x + this.width / 2, this.y + 10, Color.white.getRGB());

                    break;
                }

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

                final ITextComponent errorMessage = this.parentScreen.getMenu().getAuthController().hasError() ?
                        ITextComponent.nullToEmpty(this.parentScreen.getMenu().getAuthController().getError().getMessage()) :
                        UNKNOWN_ERROR_TEXT;

                List<IReorderingProcessor> lines = this.font.split(errorMessage, 92);
                int i = 0;
                int yPos = 4;

                if (lines.size() == 1) {
                    yPos += 6;
                }

                for (IReorderingProcessor line : lines) {
                    // stack, font, x, y, color
                    this.font.drawShadow(
                        matrixStack,
                        line,
                        (float) ((this.x + this.width / 2) - this.font.width(line) / 2),
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
        if (!ClientHelper.openUriAllowed()) {
            return false;
        }

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

    private void renderLoading(MatrixStack matrixStack)
    {
        final int animation = this.tick % 40;
        int frame = animation / 10; // 0-3

        frame = frame > 2 ? 1 : frame; // 3rd frame is the same as 1st frame

        blit(matrixStack, this.x + LOADING_XPOS, this.y + LOADING_YPOS, LOADING_UPOS, LOADING_VPOS + LOADING_HEIGHT * frame, LOADING_WIDTH, LOADING_HEIGHT, 512, 256);
    }

    @Override
    public @Nullable ITextComponent getTooltip(int mouseX, int mouseY) {

        switch (this.parentScreen.getPlayerAuthorizationState()) {
            case SERVER_AUTHENTICATION:
                return WAITING_TOOLTIP_TEXT;
            case CLIENT_AUTHORIZATION:
                if (!ClientHelper.openUriAllowed()) {
                    return LINKS_DISABLED_TOOLTIP_TEXT;
                }

                return LOGIN_TOOLTIP_TEXT;
            case LOGGED_IN:
                return new TranslationTextComponent("container.zettergallery.merchant.logged_in_player_tooltip", this.parentScreen.getAuthorizedPlayerNickname());
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
}
