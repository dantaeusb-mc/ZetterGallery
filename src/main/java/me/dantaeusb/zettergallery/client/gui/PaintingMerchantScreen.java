package me.dantaeusb.zettergallery.client.gui;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.client.gui.merchant.*;
import me.dantaeusb.zettergallery.container.PaintingMerchantContainer;
import me.dantaeusb.zettergallery.core.ClientHelper;
import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.gallery.AuthorizationCode;
import me.dantaeusb.zettergallery.gallery.PlayerToken;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.core.Helper;
import me.dantaeusb.zettergallery.menu.paintingmerchant.MerchantAuthorizationController;
import me.dantaeusb.zettergallery.network.packet.CFeedRefreshRequest;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zettergallery.trading.PaintingMerchantSaleOffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PaintingMerchantScreen extends AbstractContainerScreen<PaintingMerchantMenu> {
    public static final ResourceLocation PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE = new ResourceLocation(ZetterGallery.MOD_ID, "textures/gui/painting_trade.png");

    private static final Component LEVEL_SEPARATOR = Component.literal(" - ");

    private AuthWidget authWidget;
    private PaintingPreviewWidget previewWidget;
    private PaginatorWidget paginatorWidget;
    private OfferInfoWidget infoWidget;
    private RefreshWidget refreshWidget;

    private final List<AbstractPaintingMerchantWidget> paintingMerchantWidgets = Lists.newArrayList();

    private int tick = 0;

    /**
     * Flag is activated after player pressed log in link in authorization window
     * but window is not yet unfocused, so browser is not opened yet
     */
    private boolean waitingForBrowser = false;

    /**
     * Flag is activated after window lose focus after pressing log in button,
     * when focused back we would need to check if player authorized server
     */
    private boolean waitingForAuth = false;

    public PaintingMerchantScreen(PaintingMerchantMenu merchantContainer, Inventory playerInventory, Component title) {
        super(merchantContainer, playerInventory, title);

        this.imageHeight = 236;
        this.imageWidth = 208;
    }

    static final int AUTH_POSITION_X = 6;
    static final int AUTH_POSITION_Y = 114;

    static final int PREVIEW_POSITION_X = 11;
    static final int PREVIEW_POSITION_Y = 30;

    static final int PAGINATOR_POSITION_X = 83;
    static final int PAGINATOR_POSITION_Y = 100;

    static final int INFO_POSITION_X = 6;
    static final int INFO_POSITION_Y = 26;

    static final int REFRESH_POSITION_X = 156;
    static final int REFRESH_POSITION_Y = 5;

    @Override
    protected void init() {
        super.init();

        this.authWidget = new AuthWidget(this, this.getGuiLeft() + AUTH_POSITION_X, this.getGuiTop() + AUTH_POSITION_Y);
        this.previewWidget = new PaintingPreviewWidget(this, this.getGuiLeft() + PREVIEW_POSITION_X, this.getGuiTop() + PREVIEW_POSITION_Y);
        this.paginatorWidget = new PaginatorWidget(this, this.getGuiLeft() + PAGINATOR_POSITION_X, this.getGuiTop() + PAGINATOR_POSITION_Y);
        this.infoWidget = new OfferInfoWidget(this, this.getGuiLeft() + INFO_POSITION_X, this.getGuiTop() + INFO_POSITION_Y);
        this.refreshWidget = new RefreshWidget(this, this.getGuiLeft() + REFRESH_POSITION_X, this.getGuiTop() + REFRESH_POSITION_Y);

        this.addPaintingMerchantWidget(this.authWidget);
        this.addPaintingMerchantWidget(this.previewWidget);
        this.addPaintingMerchantWidget(this.paginatorWidget);
        this.addPaintingMerchantWidget(this.infoWidget);
        this.addPaintingMerchantWidget(this.refreshWidget);

        this.inventoryLabelX = 107;
    }

    @Override
    protected void clearWidgets() {
        super.clearWidgets();
        this.paintingMerchantWidgets.clear();
    }

    public void addPaintingMerchantWidget(AbstractPaintingMerchantWidget widget) {
        this.paintingMerchantWidgets.add(widget);
        this.addWidget(widget);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        this.tick++;

        if (this.waitingForBrowser) {
            if (!Minecraft.getInstance().isWindowActive()) {
                this.waitingForAuth = true;
                this.waitingForBrowser = false;
            }
        }

        if (this.waitingForAuth) {
            if (Minecraft.getInstance().isWindowActive()) {
                this.menu.getAuthController().handleAuthorizationRetry();
                this.waitingForAuth = false;
            }
        }

        this.authWidget.tick();
        this.previewWidget.tick();
        this.infoWidget.tick();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        this.renderProgressBar(guiGraphics);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);

        for (AbstractPaintingMerchantWidget widget : this.paintingMerchantWidgets) {
            if (widget.isMouseOver(x, y)) {
                widget.renderTooltip(guiGraphics, x, y);
            }
        }
    }

    private void renderProgressBar(GuiGraphics guiGraphics) {
        final float BAR_U = 0.0F;
        final float BAR_V = 246.0F;
        final int BAR_WIDTH = 102;
        final int BAR_HEIGHT = 5;

        final int xPos = this.leftPos + 6;
        final int yPos = this.topPos + 16;

        int merchantLevel = this.menu.getMerchantLevel();
        int merchantXp = this.menu.getMerchant().getVillagerXp();

        if (merchantLevel < 5) {
            guiGraphics.blit(
                PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE,
                xPos,
                yPos,
                BAR_U,
                BAR_V,
                BAR_WIDTH,
                BAR_HEIGHT,
                512,
                256
            );

            int k = VillagerData.getMinXpPerLevel(merchantLevel);

            if (merchantXp >= k && VillagerData.canLevelUp(merchantLevel)) {
                int l = 100;
                float f = 100.0F / (float) (VillagerData.getMaxXpPerLevel(merchantLevel) - k);
                int i1 = Math.min(Mth.floor(f * (float) (merchantXp - k)), l);

                guiGraphics.blit(
                    PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE,
                    xPos,
                    yPos,
                    BAR_U,
                    BAR_V + BAR_HEIGHT,
                    i1 + 1, // WIDTH
                    BAR_HEIGHT,
                    512,
                    256
                );

                /*int futureTraderXp = this.menu.getFutureTraderXp();

                if (futureTraderXp > 0) {
                    int k1 = Math.min(Mth.floor((float)futureTraderXp * f), 100 - i1);
                    blit(poseStack, width + 136 + i1 + 1, height + 16 + 1, this.getBlitOffset(), 2.0F, 182.0F, k1, 3, 512, 256);
                }*/
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int x, int y) {
        int merchantLevel = this.menu.getMerchantLevel();

        // Draw level
        if (merchantLevel > 0 && merchantLevel <= 5) {
            Component levelText = this.title.copy().append(LEVEL_SEPARATOR).append(Component.translatable("merchant.level." + merchantLevel));

            guiGraphics.drawString(this.getFont(), levelText, 6, 6, Color.darkGray.getRGB(), false);
        } else {
            guiGraphics.drawString(this.getFont(), this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, Color.darkGray.getRGB(), false);
        }

        this.renderOffersCount(guiGraphics, x, y);
    }

    private final static int COUNT_X = 44;
    private final static int COUNT_Y = 97;

    private void renderOffersCount(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.hasOffers()) {
            return;
        }

        PaintingMerchantOffer offer = this.getCurrentOffer();

        if (offer instanceof PaintingMerchantSaleOffer) {
            guiGraphics.drawCenteredString(this.getFont(), Component.translatable("container.zettergallery.merchant.sell"), COUNT_X, COUNT_Y, Color.white.getRGB());
        } else {
            int currentOffer = this.getCurrentOfferIndex() + 1;
            int offersCount = this.getOffersCount();

            // @todo: [MED] Remove shadow
            guiGraphics.drawCenteredString(this.getFont(), currentOffer + "/" + offersCount, COUNT_X, COUNT_Y, Color.white.getRGB());
        }
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE);

        guiGraphics.blit(
            PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE,
            this.leftPos,
            this.topPos,
            0.0F, 0.0F,
            this.imageWidth, this.imageHeight,
            512, 256
        );

        // Suggest paintings or emeralds for sale
        final int SELL_SLOT_X = 119;
        final int SELL_SLOT_Y = 119;
        final int SELL_SLOT_U = 208;
        final int SELL_SLOT_V = 160;
        final int SELL_SLOT_WIDTH = 16;
        final int SELL_SLOT_HEIGHT = 16;

        int sellSlotVOffset = 0;

        if (this.saleAllowed() && this.tick % 40 > 19) {
            sellSlotVOffset = SELL_SLOT_HEIGHT;
        }

        guiGraphics.blit(PAINTING_MERCHANT_GUI_TEXTURE_RESOURCE, this.leftPos + SELL_SLOT_X, this.topPos + SELL_SLOT_Y, SELL_SLOT_U, SELL_SLOT_V + sellSlotVOffset, SELL_SLOT_WIDTH, SELL_SLOT_HEIGHT);

        // Widgets
        this.authWidget.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.previewWidget.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.paginatorWidget.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.infoWidget.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.refreshWidget.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    public void onClose() {
        super.onClose();
    }

    public void openAuthenticationLink() {
        try {
            AuthorizationCode authorizationCode = this.menu.getAuthController().getAuthorizationCode();

            if (authorizationCode == null) {
                throw new IllegalStateException("Unable to start client authentication without cross-auth authorizationCode");
            }

            URI uri;
            if (ZetterGallery.DEBUG_LOCALHOST) {
                uri = new URI(Helper.LOCALHOST_FRONTEND + Helper.GALLERY_AUTH_SERVER_ENDPOINT);
            } else {
                uri = new URI(Helper.GALLERY_FRONTEND + Helper.GALLERY_AUTH_SERVER_ENDPOINT);
            }

            uri = addUriParam(uri, "code", this.menu.getAuthController().getAuthorizationCode().code);

            ClientHelper.openUriPrompt(this, uri.toString());

            this.waitingForBrowser = true;
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }

    public static URI addUriParam(URI uri, String name, String value) throws URISyntaxException {
        final String appendQuery = name + "=" + value;
        URI oldUri = new URI(uri.toString());
        return new URI(oldUri.getScheme(), oldUri.getAuthority(), oldUri.getPath(),
            oldUri.getQuery() == null ? appendQuery : oldUri.getQuery() + "&" + appendQuery, oldUri.getFragment());
    }

    public boolean saleAllowed() {
        return false;
    }

    /**
     * Expose some methods for widgets
     */

    public Font getFont() {
        return this.font;
    }

    /**
     * Pipe methods to container
     */

    public void prevOffer() {
        if (!this.menu.hasOffers()) {
            return;
        }

        int newOffersIndex = this.getCurrentOfferIndex() - 1;

        if (newOffersIndex < 0) {
            newOffersIndex = this.getOffersCount() - 1;
        }

        this.updateCurrentOfferIndex(newOffersIndex);
    }

    public void nextOffer() {
        if (!this.menu.hasOffers()) {
            return;
        }

        int newOffersIndex = this.getCurrentOfferIndex() + 1;

        if (newOffersIndex > this.menu.getOffersCount() - 1) {
            newOffersIndex = 0;
        }

        this.updateCurrentOfferIndex(newOffersIndex);
    }

    private void updateCurrentOfferIndex(int newOffersIndex) {
        this.menu.updateCurrentOfferIndex(newOffersIndex);
    }

    public void requestNewOffers() {
        CFeedRefreshRequest feedRefreshRequest = new CFeedRefreshRequest();
        ZetterGalleryNetwork.simpleChannel.sendToServer(feedRefreshRequest);
    }

    public int getOffersCount() {
        return this.menu.getOffersCount();
    }

    public int getCurrentOfferIndex() {
        if (this.menu.hasOffers()) {
            return this.menu.getCurrentOfferIndex();
        }

        return 0;
    }

    @Nullable
    public PaintingMerchantOffer getCurrentOffer() {
        return this.menu.getCurrentOffer();
    }

    public PaintingMerchantContainer.OffersState getOffersState() {
        return this.menu.getContainer().getState();
    }

    public MerchantAuthorizationController.PlayerAuthorizationState getPlayerAuthorizationState() {
        return this.menu.getAuthController().getState();
    }

    public String getAuthorizedPlayerNickname() {
        PlayerToken.PlayerInfo playerInfo = this.menu.getAuthController().getPlayerInfo();

        if (playerInfo != null) {
            return playerInfo.nickname();
        }

        return "Anonymous";
    }
}
