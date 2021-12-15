package com.dantaeusb.zettergallery.client.gui;

import com.dantaeusb.zettergallery.ZetterGallery;
import com.dantaeusb.zettergallery.client.gui.merchant.InfoWidget;
import com.dantaeusb.zettergallery.client.gui.merchant.PreviewWidget;
import com.dantaeusb.zettergallery.client.gui.merchant.AuthWidget;
import com.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import com.dantaeusb.zettergallery.core.Helper;
import com.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import com.dantaeusb.zettergallery.network.packet.CGallerySelectOfferPacket;
import com.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

@OnlyIn(Dist.CLIENT)
public class PaintingMerchantScreen extends AbstractContainerScreen<PaintingMerchantMenu> {
    // This is the resource location for the background image
    private static final ResourceLocation LOADING_RESOURCE = new ResourceLocation("zetter", "textures/gui/painting_trade_loading.png");
    private static final ResourceLocation READY_RESOURCE = new ResourceLocation("zetter", "textures/gui/painting_trade.png");

    private static final Component TRADES_TEXT = new TranslatableComponent("merchant.trades");
    private static final Component DASH_TEXT = new TextComponent(" - ");
    private static final Component field_243353_D = new TranslatableComponent("merchant.deprecated");

    private AuthWidget statusWidget;
    private PreviewWidget previewWidget;
    private InfoWidget infoWidget;

    private boolean waitingForBrowser = false;
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

        this.statusWidget = new AuthWidget(this, this.getGuiLeft() + AUTH_POSITION_X, this.getGuiTop() + AUTH_POSITION_Y);
        this.previewWidget = new PreviewWidget(this, this.getGuiLeft() + PREVIEW_POSITION_X, this.getGuiTop() + PREVIEW_POSITION_Y);
        this.infoWidget = new InfoWidget(this, this.getGuiLeft() + INFO_POSITION_X, this.getGuiTop() + INFO_POSITION_Y);

        this.addWidget(this.statusWidget);
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

    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);

        if (this.menu.getState() == PaintingMerchantMenu.State.READY) {
            super.render(matrixStack, mouseX, mouseY, partialTicks);
            this.previewWidget.render(matrixStack, mouseX, mouseY, partialTicks);
            this.infoWidget.render(matrixStack, mouseX, mouseY, partialTicks);
        } else {
            this.cleanRender(matrixStack, mouseX, mouseY, partialTicks);
            this.statusWidget.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    /**
     * Copied from parent class and adjusted to not draw slots
     * @param poseStack
     * @param mouseX
     * @param mouseY
     * @param partialTicks
     */
    public void cleanRender(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        int i = this.leftPos;
        int j = this.topPos;
        this.renderBg(poseStack, partialTicks, mouseX, mouseY);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ContainerScreenEvent.DrawBackground(this, poseStack, mouseX, mouseY));
        RenderSystem.disableDepthTest();
        PoseStack poseStack1 = RenderSystem.getModelViewStack();
        poseStack1.pushPose();
        poseStack1.translate(i, j, 0.0D);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.hoveredSlot = null;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        this.drawGuiContainerForegroundLayer(poseStack1, mouseX, mouseY);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ContainerScreenEvent.DrawForeground(this, poseStack1, mouseX, mouseY));

        poseStack1.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        if (this.waitingForBrowser) {
            if (!Minecraft.getInstance().isWindowActive()) {
                this.waitingForAuth = true;
                this.waitingForBrowser = false;
            }
        }

        if (this.waitingForAuth) {
            if (Minecraft.getInstance().isWindowActive()) {
                this.menu.updateAuthentication();
                this.waitingForAuth = false;
            }
        }
    }

    private void drawMerchantLevel(PoseStack p_238839_1_, int p_238839_2_, int p_238839_3_, MerchantOffer p_238839_4_) {
        /*this.minecraft.getTextureManager().bindTexture(PAINTING_RESOURCE);
        int i = this.menu.getMerchantLevel();
        int j = this.menu.getXp();

        if (i < 5) {
            blit(p_238839_1_, p_238839_2_ + 136, p_238839_3_ + 16, this.getBlitOffset(), 0.0F, 186.0F, 102, 5, 256, 512);
            int k = VillagerData.getExperiencePrevious(i);
            if (j >= k && VillagerData.canLevelUp(i)) {
                int l = 100;
                float f = 100.0F / (float)(VillagerData.getExperienceNext(i) - k);
                int i1 = Math.min(MathHelper.floor(f * (float)(j - k)), 100);
                blit(p_238839_1_, p_238839_2_ + 136, p_238839_3_ + 16, this.getBlitOffset(), 0.0F, 191.0F, i1 + 1, 5, 256, 512);
                int j1 = this.menu.getPendingExp();
                if (j1 > 0) {
                    int k1 = Math.min(MathHelper.floor((float)j1 * f), 100 - i1);
                    blit(p_238839_1_, p_238839_2_ + 136 + i1 + 1, p_238839_3_ + 16 + 1, this.getBlitOffset(), 2.0F, 182.0F, k1, 3, 256, 512);
                }

            }
        }*/
    }

    protected void drawGuiContainerForegroundLayer(PoseStack matrixStack, int x, int y) {
        /*int merchantLevel = this.menu.getMerchantLevel();

        // Draw level
        if (merchantLevel > 0 && merchantLevel <= 5 && this.menu.func_217042_i()) {
            ITextComponent levelText = this.title.deepCopy().append(DASH_TEXT).append(new TranslationTextComponent("merchant.level." + merchantLevel));
            int j = this.font.getStringPropertyWidth(levelText);
            int k = 49 + this.xSize / 2 - j / 2;
            this.font.func_243248_b(matrixStack, levelText, (float)k, 6.0F, 4210752);
        } else {
            this.font.func_243248_b(matrixStack, this.title, (float)(49 + this.xSize / 2 - this.font.getStringPropertyWidth(this.title) / 2), 6.0F, 4210752);
        }

        this.font.func_243248_b(matrixStack, this.playerInventory.getDisplayName(), (float)this.playerInventoryTitleX, (float)this.playerInventoryTitleY, 4210752);
        int l = this.font.getStringPropertyWidth(TRADES_TEXT);
        this.font.func_243248_b(matrixStack, TRADES_TEXT, (float)(5 - l / 2 + 48), 6.0F, 4210752);*/
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.menu.getState() == PaintingMerchantMenu.State.READY) {
            this.minecraft.getTextureManager().bindForSetup(READY_RESOURCE);
        } else {
            this.minecraft.getTextureManager().bindForSetup(LOADING_RESOURCE);
        }

        blit(matrixStack, this.leftPos, this.topPos, this.getBlitOffset(), 0.0F, 0.0F, this.width, this.height, 256, 256);
    }

    public void prevOffer() {
        if (this.menu.getOffers() == null || this.getOffersCount() == 0) {
            return;
        }

        int newOffersIndex = this.getCurrentOfferIndex() - 1;

        if (newOffersIndex < 0) {
            newOffersIndex = this.getOffersCount() - 1;
        }

        this.updateCurrentOfferIndex(newOffersIndex);
    }

    public void nextOffer() {
        if (this.menu.getOffers() == null || this.getOffersCount() == 0) {
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

        CGallerySelectOfferPacket selectOfferPacket = new CGallerySelectOfferPacket(newOffersIndex);
        ZetterGalleryNetwork.simpleChannel.sendToServer(selectOfferPacket);
    }

    public int getOffersCount() {
        return this.menu.getOffersCount();
    }

    public int getCurrentOfferIndex() {
        if (this.menu.getOffers() != null) {
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
