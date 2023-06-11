package me.dantaeusb.zettergallery.client.gui.merchant;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import me.dantaeusb.zettergallery.container.PaintingMerchantContainer;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import me.dantaeusb.zettergallery.trading.PaintingMerchantPurchaseOffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class OfferInfoWidget extends AbstractPaintingMerchantWidget {
    private static final Component FETCHING_OFFERS = Component.translatable("container.zettergallery.merchant.fetching_offers");
    private static final Component LOADING_PAINTING = Component.translatable("container.zettergallery.merchant.loading_painting");
    private static final Component UNKNOWN_OFFER_ERROR = Component.translatable("container.zettergallery.merchant.offer.unknown_error");

    @Nullable
    protected Minecraft minecraft;
    protected ItemRenderer itemRenderer;
    protected Font font;

    private int tick = 0;

    private static final int WIDTH = 196;
    private static final int HEIGHT = 74;

    public OfferInfoWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, Component.translatable("container.zetter.painting.info"));

        this.minecraft = parentScreen.getMinecraft();
        this.itemRenderer = minecraft.getItemRenderer();
        this.font = minecraft.font;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (this.parentScreen.getOffersState().equals(PaintingMerchantContainer.OffersState.LOADING)) {
            this.renderLoadingLogo(guiGraphics);
        } else if (this.parentScreen.getOffersState().equals(PaintingMerchantContainer.OffersState.ERROR)) {
            this.renderErrorMessage(guiGraphics, this.parentScreen.getMenu().getAuthController().getError().getMessage());
        } else {
            this.renderPaintingInfo(guiGraphics);
        }
    }

    private static final int LOGO_WIDTH = 32;
    private static final int LOGO_HEIGHT = 32;
    private static final int LOGO_UPOS = 208;
    private static final int LOGO_VPOS = 128;
    private static final int LOGO_XPOS = (WIDTH - LOGO_WIDTH) / 2;
    private static final int LOGO_YPOS = (HEIGHT - (LOGO_HEIGHT + 16)) / 2;

    private static final int LOGO_LOADER_WIDTH = 16;
    private static final int LOGO_LOADER_HEIGHT = 20;
    private static final int LOGO_LOADER_UPOS = 240;
    private static final int LOGO_LOADER_VPOS = 128;
    private static final int LOGO_LOADER_XPOS = (WIDTH - LOGO_LOADER_WIDTH) / 2;
    private static final int LOGO_LOADER_YPOS = (HEIGHT - (LOGO_LOADER_HEIGHT + 16)) / 2;

    private void renderLoadingLogo(GuiGraphics guiGraphics) {
        final int animation = this.tick % (LOGO_LOADER_HEIGHT * 10);
        int frame = animation / 10; // 0-19

        // draw loader
        guiGraphics.blit(
            PaintingMerchantScreen.PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE,
            this.getX() + LOGO_LOADER_XPOS,
            this.getY() + LOGO_LOADER_YPOS,
            LOGO_LOADER_UPOS,
            LOGO_LOADER_VPOS + frame,
            LOGO_LOADER_WIDTH,
            LOGO_LOADER_HEIGHT,
            512,
            256
        );

        // draw logo
        guiGraphics.blit(
            PaintingMerchantScreen.PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE,
            this.getX() + LOGO_XPOS,
            this.getY() + LOGO_YPOS,
            LOGO_UPOS,
            LOGO_VPOS,
            LOGO_WIDTH,
            LOGO_HEIGHT,
            512,
            256
        );

        guiGraphics.drawCenteredString(this.parentScreen.getFont(), FETCHING_OFFERS, this.getX() + LOGO_XPOS + (LOGO_WIDTH / 2), this.getY() + LOGO_YPOS + LOGO_HEIGHT + 7, Color.white.getRGB());
    }

    private void renderPaintingInfo(GuiGraphics guiGraphics) {
        PaintingMerchantOffer offer = this.parentScreen.getCurrentOffer();

        if (offer == null) {
            ZetterGallery.LOG.error("No offer to render info");
            return;
        }

        if (this.isLoading()) {
            // Preview widget will have loader
            guiGraphics.drawCenteredString(this.parentScreen.getFont(), LOADING_PAINTING, this.getX() + this.width / 2, this.getY() + this.height / 2 - 4, Color.white.getRGB());
            return;
        }

        if (offer.isError()) {
            String errorMessage = offer.getErrorMessage();
            if (errorMessage == null) {
                errorMessage = UNKNOWN_OFFER_ERROR.getString();
            }

            this.renderErrorMessage(guiGraphics, errorMessage);
            return;
        }

        DummyCanvasData offerPaintingData = offer.getDummyPaintingData();

        String priceString = String.valueOf(offer.getPrice());
        final int priceWidth = this.font.width(priceString);

        ItemStack emeraldStack = new ItemStack(Items.EMERALD);
        guiGraphics.renderFakeItem(
            emeraldStack,
            this.getX() + this.width - 23,
            this.getY() + 63
        );
        guiGraphics.drawString(this.parentScreen.getFont(), priceString, this.getX() + this.width - 24 - priceWidth, this.getY() + 67, Color.white.getRGB(), false);

        // Duplicate from PaintingItem#setPaintingData
        int widthBlocks = offerPaintingData.getWidth() / offerPaintingData.getResolution().getNumeric();
        int heightBlocks = offerPaintingData.getHeight() / offerPaintingData.getResolution().getNumeric();

        // Account for RTL?
        Component blockSize = (Component.translatable("item.zetter.painting.size", Integer.toString(widthBlocks), Integer.toString(heightBlocks)));

        List<FormattedCharSequence> multilinePaintingTitle = this.font.split(FormattedText.of(offer.getPaintingName()), 80);
        List<FormattedCharSequence> multilineNickname = this.font.split(FormattedText.of(offer.getAuthorName()), 80);

        // To avoid texture swapping, first draw icons, then text
        RenderSystem.setShaderTexture(0, PaintingMerchantScreen.PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE);

        // Feed icon
        if (offer instanceof PaintingMerchantPurchaseOffer purchaseOffer) {
            if (purchaseOffer.getFeedName() != null) {
                int feedIconU = 16;

                switch (purchaseOffer.getFeedName()) {
                    case "new":
                        feedIconU = 16;
                        break;
                    case "top":
                        feedIconU = 32;
                        break;
                    case "favorite":
                        feedIconU = 48;
                        break;
                    case "hot":
                    default:
                        break;
                }

                guiGraphics.blit(
                    PaintingMerchantScreen.PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE,
                    this.getX() + WIDTH - 16 - 5,
                    this.getY() + 5,
                    304 + feedIconU,
                    0,
                    16,
                    16,
                    512,
                    256
                );
            }
        }

        final int ICON_OFFSET = 5 + 64 + 5;
        final int TEXT_OFFSET = ICON_OFFSET + 8 + 5;

        // Name
        guiGraphics.blit(
            PaintingMerchantScreen.PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE,
            this.getX() + ICON_OFFSET,
            this.getY() + 4,
            265,
            0,
            8,
            8,
            512,
            256
        );

        // Author
        // @todo: real icon from base64
        guiGraphics.blit(
            PaintingMerchantScreen.PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE,
            this.getX() + ICON_OFFSET,
            this.getY() + 4 + 11 * multilinePaintingTitle.size(),
            265,
            8,
            8,
            8,
            512,
            256
        );

        // Size
        guiGraphics.blit(
            PaintingMerchantScreen.PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE,
            this.getX() + ICON_OFFSET,
            this.getY() + 4 + 11 * multilinePaintingTitle.size() + 11 * multilineNickname.size(),
            273 + (widthBlocks - 1) * 8,
            (heightBlocks - 1) * 8,
            8,
            8,
            512,
            256
        );

        int titleNameLines = 0;
        for (FormattedCharSequence paintingTitleNameLine : multilinePaintingTitle) {
            guiGraphics.drawString(this.parentScreen.getFont(), paintingTitleNameLine, this.getX() + TEXT_OFFSET, this.getY() + 5 + 11 * titleNameLines++, Color.white.getRGB(), false);
        }

        int nicknameLines = 0;
        for (FormattedCharSequence nicknameLine : multilineNickname) {
            guiGraphics.drawString(this.parentScreen.getFont(), nicknameLine, this.getX() + TEXT_OFFSET, this.getY() + 5 + 11 * titleNameLines + 11 * nicknameLines++, Color.white.getRGB(), false);
        }

        guiGraphics.drawString(this.parentScreen.getFont(), blockSize.getString(), this.getX() + TEXT_OFFSET, this.getY() + 5 + 11 * titleNameLines + 11 * nicknameLines, Color.white.getRGB(), false);
    }

    private void renderErrorMessage(GuiGraphics guiGraphics, String errorMessage) {
        List<FormattedCharSequence> multilineErrorMessage = this.font.split(FormattedText.of(errorMessage), 186);
        int errorLines = 0;
        for (FormattedCharSequence errorLine : multilineErrorMessage) {
            guiGraphics.drawCenteredString(this.parentScreen.getFont(), errorLine, this.getX() + this.width / 2, this.getY() + 20 + 11 * errorLines++, Color.white.getRGB());
        }
    }

    public boolean isLoading() {
        if (this.parentScreen.getCurrentOffer() == null) {
            return false;
        }

        return this.parentScreen.getCurrentOffer().isLoading();
    }

    /**
     * Show some kind of indicator, but generally item in output slot will
     * be enough of an indicator
     *
     * @return
     */
    public boolean canProceed() {
        if (this.parentScreen.getCurrentOffer() == null) {
            return false;
        }

        return this.parentScreen.getCurrentOffer().isReady();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public void tick() {
        this.tick++;
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        return null;
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
        int i = this.getX();
        int j = this.getY();
        mouseX = mouseX - (double) i;
        mouseY = mouseY - (double) j;
        return mouseX >= (double) (x - 1) && mouseX < (double) (x + width + 1) && mouseY >= (double) (y - 1) && mouseY < (double) (y + height + 1);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }
}
