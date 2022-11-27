package me.dantaeusb.zettergallery.client.gui.merchant;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import me.dantaeusb.zettergallery.container.PaintingMerchantContainer;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

public class OfferInfoWidget extends AbstractPaintingMerchantWidget {
    private static final Component LOADING_TEXT = Component.translatable("container.zettergallery.merchant.fetching_sales");

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

    private static final int OFFER_BUTTON_WIDTH = 160;
    private static final int OFFER_BUTTON_HEIGHT = 32;

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderTexture(0, PaintingMerchantScreen.GUI_TEXTURE_RESOURCE);

        if (this.parentScreen.getOffersState().equals(PaintingMerchantContainer.OffersState.LOADING)) {
            this.drawLoadingLogo(matrixStack);
        } else if (this.parentScreen.getOffersState().equals(PaintingMerchantContainer.OffersState.ERROR)) {
            this.drawConnectionError(matrixStack);
            // @todo: draw error
        } else {
            this.drawPaintingInfo(matrixStack);
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
    private static final int LOGO_LOADER_YPOS = (HEIGHT - LOGO_LOADER_HEIGHT) / 2;

    private void drawLoadingLogo(PoseStack matrixStack) {
        final int animation = this.tick % (LOGO_LOADER_HEIGHT * 10);
        int frame = animation / 10; // 0-19

        // draw loader
        blit(
                matrixStack,
                this.x + LOGO_LOADER_XPOS,
                this.y + LOGO_LOADER_YPOS,
                LOGO_LOADER_UPOS,
                LOGO_LOADER_VPOS + frame,
                LOGO_LOADER_WIDTH,
                LOGO_LOADER_HEIGHT,
                512,
                256
        );

        // draw logo
        blit(
                matrixStack,
                this.x + LOGO_XPOS,
                this.y + LOGO_YPOS,
                LOGO_UPOS,
                LOGO_VPOS,
                LOGO_WIDTH,
                LOGO_HEIGHT,
                512,
                256
        );

        drawCenteredString(matrixStack, this.font, LOADING_TEXT, this.x + LOGO_XPOS + (LOGO_WIDTH / 2), this.y + LOGO_YPOS + LOGO_HEIGHT + 7, Color.white.getRGB());
    }

    private void drawPaintingInfo(PoseStack matrixStack) {
        PaintingMerchantOffer<?> offer = this.parentScreen.getCurrentOffer();

        if (offer == null) {
            ZetterGallery.LOG.error("No offer to render info");
            return;
        }

        if (this.isLoading() || offer.getPaintingData().isEmpty()) {
            // Preview widget will have loader
            // @todo: draw loading text
            return;
        }

        PaintingData offerPaintingData = offer.getPaintingData().get();

        String priceString = String.valueOf(offer.getPrice());
        final int priceWidth = this.font.width(priceString);

        ItemStack emeraldStack = new ItemStack(Items.EMERALD);
        this.itemRenderer.renderGuiItem(emeraldStack, this.x + this.width - 23, this.y + 63);
        this.font.draw(matrixStack, priceString, this.x + this.width - 24 - priceWidth, this.y + 67, Color.white.getRGB());

        // Duplicate from PaintingItem#setPaintingData
        int widthBlocks = offerPaintingData.getWidth() / offerPaintingData.getResolution().getNumeric();
        int heightBlocks = offerPaintingData.getHeight() / offerPaintingData.getResolution().getNumeric();

        // Account for RTL?
        Component blockSize = (Component.translatable("item.zetter.painting.size", Integer.toString(widthBlocks), Integer.toString(heightBlocks)));

        if (offer.isError()) {
            final String errorMessage = offer.getMessage().orElse("Something went wrong");

            this.font.draw(matrixStack, errorMessage, this.x + this.width / 2.0F - (this.font.width(errorMessage) / 2.0F), this.y + 12, Color.white.getRGB());
            return;
        }

        // To avoid texture swapping, first draw icons, then text
        RenderSystem.setShaderTexture(0, PaintingMerchantScreen.GUI_TEXTURE_RESOURCE);

        // Feed icon
        if (!offer.isSaleOffer() && offer.getFeedName() != null) {
            int feedIconU = 0;
            switch (offer.getFeedName()) {
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

            blit(
                matrixStack,
                this.x + WIDTH - 16 - 5,
                this.y + 5,
                304 + feedIconU,
                0,
                16,
                16,
                512,
                256
            );
        }

        final int ICON_OFFSET = 5 + 64 + 5;
        final int TEXT_OFFSET = ICON_OFFSET + 8 + 5;

        // Name
        blit(
            matrixStack,
            this.x + ICON_OFFSET,
            this.y + 4,
            265,
            0,
            8,
            8,
            512,
            256
        );

        // Author
        // @todo: real icon from base64
        blit(
            matrixStack,
            this.x + ICON_OFFSET,
            this.y + 4 + 11,
            265,
            8,
            8,
            8,
            512,
            256
        );

        // Size
        blit(
            matrixStack,
            this.x + ICON_OFFSET,
            this.y + 4 + 22,
            273 + (widthBlocks - 1) * 8,
            (heightBlocks - 1) * 8,
            8,
            8,
            512,
            256
        );

        this.font.draw(matrixStack, offerPaintingData.getPaintingTitle(), this.x + TEXT_OFFSET, this.y + 5, Color.white.getRGB());
        this.font.draw(matrixStack, offerPaintingData.getAuthorName(), this.x + TEXT_OFFSET, this.y + 5 + 11, Color.white.getRGB());
        this.font.draw(matrixStack, blockSize.getString(), this.x + TEXT_OFFSET, this.y + 5 + 22, Color.white.getRGB());
    }

    private void drawConnectionError(PoseStack matrixStack) {
        final String errorMessage = this.parentScreen.getMenu().getAuthController().getError().getMessage();

        drawCenteredString(matrixStack, this.font, errorMessage, this.x + this.width / 2, this.y + 20, Color.white.getRGB());
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
