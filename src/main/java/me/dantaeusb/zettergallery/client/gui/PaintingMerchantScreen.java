package me.dantaeusb.zettergallery.client.gui;

import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.client.gui.merchant.InfoWidget;
import me.dantaeusb.zettergallery.client.gui.merchant.PreviewWidget;
import me.dantaeusb.zettergallery.client.gui.merchant.AuthWidget;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.core.Helper;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

@OnlyIn(Dist.CLIENT)
public class PaintingMerchantScreen extends AbstractContainerScreen<PaintingMerchantMenu> {
    // This is the resource location for the background image
    private static final ResourceLocation LOADING_RESOURCE = new ResourceLocation(ZetterGallery.MOD_ID, "textures/gui/painting_trade_loading.png");
    private static final ResourceLocation READY_RESOURCE = new ResourceLocation(ZetterGallery.MOD_ID, "textures/gui/painting_trade.png");

    private static final Component LEVEL_SEPARATOR = Component.literal(" - ");

    private AuthWidget authWidget;
    private PreviewWidget previewWidget;
    private InfoWidget infoWidget;

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
        this.imageWidth = 176;
    }

    static final int AUTH_POSITION_X = 7;
    static final int AUTH_POSITION_Y = 7;

    static final int PREVIEW_POSITION_X = 42;
    static final int PREVIEW_POSITION_Y = 25;

    static final int INFO_POSITION_X = 8;
    static final int INFO_POSITION_Y = 107;

    @Override
    protected void init() {
        super.init();

        this.authWidget = new AuthWidget(this, this.getGuiLeft() + AUTH_POSITION_X, this.getGuiTop() + AUTH_POSITION_Y);
        this.previewWidget = new PreviewWidget(this, this.getGuiLeft() + PREVIEW_POSITION_X, this.getGuiTop() + PREVIEW_POSITION_Y);
        this.infoWidget = new InfoWidget(this, this.getGuiLeft() + INFO_POSITION_X, this.getGuiTop() + INFO_POSITION_Y);

        this.addWidget(this.authWidget);
        this.addWidget(this.previewWidget);
        this.addWidget(this.infoWidget);

        this.inventoryLabelX = 107;
    }

    public void onClose() {
        super.onClose();
    }

    public void openAuthenticationLink() {
        try {
            String token = this.menu.getCrossAuthorizationCode();

            if (token == null) {
                throw new IllegalStateException("Unable to start client authentication without token");
            }

            URIBuilder urlBuilder = new URIBuilder(Helper.GALLERY_HOST);
            urlBuilder.setScheme(Helper.GALLERY_SCHEME);
            urlBuilder.setPort(Helper.GALLERY_PORT);
            urlBuilder.addPath(Helper.GALLERY_AUTH_SERVER_ENDPOINT);
            urlBuilder.addParameter("code", this.menu.getCrossAuthorizationCode());

            Util.getPlatform().openUri(urlBuilder.getURI());

            this.waitingForBrowser = true;
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }

    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);

        super.render(poseStack, mouseX, mouseY, partialTicks);

        this.renderProgressBar(poseStack);

        this.renderTooltip(poseStack, mouseX, mouseY);
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
                this.menu.requestUpdateAuthenticationStatus();
                this.waitingForAuth = false;
            }
        }

        this.authWidget.tick();
        this.infoWidget.tick();
    }

    private void renderProgressBar(PoseStack poseStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, READY_RESOURCE);

        final float BAR_U = 0.0F;
        final float BAR_V = 246.0F;
        final int BAR_WIDTH = 102;
        final int BAR_HEIGHT = 5;

        final int xPos = this.leftPos + (this.imageWidth / 2) - 50;
        final int yPos = this.topPos + 16;

        int merchantLevel = this.menu.getMerchantLevel();
        int merchantXp = this.menu.getMerchant().getVillagerXp();

        if (merchantLevel < 5) {
            blit(
                    poseStack,
                    xPos,
                    yPos,
                    this.getBlitOffset(), // hmmmm
                    BAR_U,
                    BAR_V,
                    BAR_WIDTH,
                    BAR_HEIGHT,
                    256,
                    256
            );

            int k = VillagerData.getMinXpPerLevel(merchantLevel);

            if (merchantXp >= k && VillagerData.canLevelUp(merchantLevel)) {
                int l = 100;
                float f = 100.0F / (float)(VillagerData.getMaxXpPerLevel(merchantLevel) - k);
                int i1 = Math.min(Mth.floor(f * (float)(merchantXp - k)), l);

                blit(
                        poseStack,
                        xPos,
                        yPos,
                        this.getBlitOffset(),
                        BAR_U,
                        BAR_V + BAR_HEIGHT,
                        i1 + 1, // WIDTH
                        BAR_HEIGHT,
                        256,
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
    protected void renderLabels(PoseStack poseStack, int x, int y) {
        int merchantLevel = this.menu.getMerchantLevel();

        // Draw level
        if (merchantLevel > 0 && merchantLevel <= 5) {
            Component levelText = this.title.copy().append(LEVEL_SEPARATOR).append(Component.translatable("merchant.level." + merchantLevel));
            int textWidth = this.font.width(levelText);
            int textPos = this.imageWidth / 2 - textWidth / 2;
            this.font.draw(poseStack, levelText, (float)textPos, 6.0F, Color.darkGray.getRGB());
        } else {
            this.font.draw(poseStack, this.title, (float)(this.imageWidth / 2 - this.font.width(this.title) / 2), 6.0F, Color.darkGray.getRGB());
        }
    }

    protected void renderBg(PoseStack poseStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.menu.getState() == PaintingMerchantMenu.State.READY) {
            RenderSystem.setShaderTexture(0, READY_RESOURCE);
        } else {
            RenderSystem.setShaderTexture(0, LOADING_RESOURCE);
        }

        blit(poseStack, this.leftPos, this.topPos, this.getBlitOffset(), 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        if (this.menu.getState() == PaintingMerchantMenu.State.READY) {
            // Suggest paintings or emeralds for sale
            final int SELL_SLOT_X = 15;
            final int SELL_SLOT_Y = 83;
            final int SELL_SLOT_U = 212;
            final int SELL_SLOT_V = 0;
            final int SELL_SLOT_WIDTH = 16;
            final int SELL_SLOT_HEIGHT = 16;
            final int sellSlotVOffset = this.tick % 40 > 19 ? SELL_SLOT_HEIGHT : 0;

            blit(poseStack, this.leftPos + SELL_SLOT_X, this.topPos + SELL_SLOT_Y, SELL_SLOT_U, SELL_SLOT_V + sellSlotVOffset, SELL_SLOT_WIDTH, SELL_SLOT_HEIGHT);

            // Widgets
            this.previewWidget.render(poseStack, mouseX, mouseY, partialTicks);
            this.infoWidget.render(poseStack, mouseX, mouseY, partialTicks);
        } else {
            this.authWidget.render(poseStack, mouseX, mouseY, partialTicks);
        }
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

    public void proceed() {
        this.menu.startCheckout();
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

    public PaintingMerchantMenu.State getState() {
        return this.menu.getState();
    }

    class URIBuilder {
        private StringBuilder path, params;

        private String scheme, host;

        @Nullable
        private Integer port;

        void setScheme(String conn) {
            this.scheme = conn;
        }

        void setPort(int port) {
            this.port = port;
        }

        URIBuilder() {
            path = new StringBuilder();
            params = new StringBuilder();
        }

        URIBuilder(String host) {
            this();
            this.host = host;
        }

        void addPath(String folder) {
            path.append("/");
            path.append(folder);
        }

        void addParameter(String parameter, String value) {
            if (params.toString().length() > 0) {
                params.append("&");
            }

            params.append(parameter);
            params.append("=");
            params.append(value);
        }

        URI getURI() throws URISyntaxException, MalformedURLException {
            String authority = this.host;

            boolean needPort = !(
                    this.scheme.equals("http") && (this.port == null || this.port == 80) ||
                    this.scheme.equals("https") && (this.port == null || this.port == 443)
            );

            if (needPort) {
                authority = authority + ":" + this.port;
            }

            return new URI(this.scheme, authority, this.path.toString(),
                    this.params.toString(), null);
        }
    }
}
