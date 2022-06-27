package me.dantaeusb.zettergallery.client.gui.merchant;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.client.gui.PaintingMerchantScreen;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Optional;

public class InfoWidget extends AbstractWidget implements Widget {
    private static final ResourceLocation BUTTON_RESOURCE = new ResourceLocation(ZetterGallery.MOD_ID, "textures/gui/painting_trade_button.png");

    protected final PaintingMerchantScreen parentScreen;

    @Nullable
    protected Minecraft minecraft;
    protected ItemRenderer itemRenderer;
    protected Font font;

    private int tick = 0;

    static final int WIDTH = 160;
    static final int HEIGHT = 32;

    public InfoWidget(PaintingMerchantScreen parentScreen, int x, int y) {
        super(x, y, WIDTH, HEIGHT, new TranslatableComponent("container.zetter.painting.info"));

        this.parentScreen = parentScreen;

        this.minecraft = parentScreen.getMinecraft();
        this.itemRenderer = minecraft.getItemRenderer();
        this.font = minecraft.font;
    }

    private static final int OFFER_BUTTON_WIDTH = 160;
    private static final int OFFER_BUTTON_HEIGHT = 32;

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderTexture(0, BUTTON_RESOURCE);

        PaintingMerchantOffer offer = this.parentScreen.getCurrentOffer();
        if (offer == null) {
            return;
        }

        if (offer.getPaintingData().isEmpty()) {
            return;
        }

        PaintingData offerPaintingData = offer.getPaintingData().get();

        final boolean hovered = isPointInRegion(0, 0, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT, mouseX, mouseY);

        if (this.canProceed()) {
            if (hovered) {
                blit(
                        matrixStack,
                        this.x,
                        this.y,
                        0,
                        OFFER_BUTTON_HEIGHT * 2,
                        OFFER_BUTTON_WIDTH,
                        OFFER_BUTTON_HEIGHT,
                        256,
                        256
                );
            } else {
                blit(
                        matrixStack,
                        this.x,
                        this.y,
                        0,
                        OFFER_BUTTON_HEIGHT,
                        OFFER_BUTTON_WIDTH,
                        OFFER_BUTTON_HEIGHT,
                        256,
                        256
                );
            }
        } else {
            blit(
                    matrixStack,
                    this.x,
                    this.y,
                    0,
                    0,
                    OFFER_BUTTON_WIDTH,
                    OFFER_BUTTON_HEIGHT,
                    256,
                    256
            );
        }

        // @todo: cache this and size
        TranslatableComponent actionString = new TranslatableComponent(this.parentScreen.getCurrentOffer().isSaleOffer() ? "container.zettergallery.merchant.sell" : "container.zettergallery.merchant.buy");
        final int actionWidth = this.font.width(actionString);

        String priceString = String.valueOf(offer.getPrice());
        final int priceWidth = this.font.width(priceString);

        ItemStack emeraldStack = new ItemStack(Items.EMERALD);

        this.itemRenderer.renderGuiItem(emeraldStack, this.x + this.width - 21, this.y + 8); // 8 padding + 16 texture - 3 emerald item padding

        // Duplicate from PaintingItem#setPaintingData
        int widthBlocks = offerPaintingData.getWidth() / offerPaintingData.getResolution().getNumeric();
        int heightBlocks = offerPaintingData.getHeight() / offerPaintingData.getResolution().getNumeric();
        // Account for RTL?
        TranslatableComponent blockSize = (new TranslatableComponent("item.zetter.painting.size", Integer.toString(widthBlocks), Integer.toString(heightBlocks)));

        if (this.isLoading()) {
            this.drawLoading(matrixStack);
            return;
        }

        if (offer.isError()) {
            final String errorMessage = offer.getMessage().orElse("Something went wrong");

            this.font.drawShadow(matrixStack, errorMessage, this.x + this.width / 2.0F - (this.font.width(errorMessage) / 2.0F), this.y + 12, Color.white.getRGB());
            return;
        }

        if (this.canProceed()) {
            this.font.drawShadow(matrixStack, offerPaintingData.getPaintingName(), this.x + 8, this.y + 7, Color.white.getRGB());
            this.font.drawShadow(matrixStack, offerPaintingData.getAuthorName() + ", " + blockSize.getString(), this.x + 8, this.y + 7 + 11, Color.white.getRGB());
            this.font.drawShadow(matrixStack, priceString, this.x + this.width - 22 - priceWidth, this.y + 12, Color.white.getRGB()); // -21 - 4 padding to text + 3 emerald item padding

            if (hovered) {
                final int xOverlayPos = OFFER_BUTTON_WIDTH / 2 - priceWidth / 2 - 3;
                final int yOverlayPos = 9;

                blit(
                        matrixStack,
                        this.x + xOverlayPos,
                        this.y + yOverlayPos,
                        xOverlayPos,
                        OFFER_BUTTON_HEIGHT * 2 + yOverlayPos,
                        actionWidth + 6,
                        9 + 6,
                        256,
                        256
                );

                this.font.drawShadow(matrixStack, actionString, this.x + this.width / 2.0F - (actionWidth / 2.0F), this.y + 12, Color.white.getRGB());
            }
        } else {
            this.font.draw(matrixStack, offerPaintingData.getPaintingName(), this.x + 8, this.y + 7, Color.darkGray.getRGB());
            this.font.draw(matrixStack, offerPaintingData.getAuthorName() + ", " + blockSize.getString(), this.x + 8, this.y + 7 + 11, Color.darkGray.getRGB());
            this.font.draw(matrixStack, priceString, this.x + this.width - 22 - priceWidth, this.y + 12, Color.darkGray.getRGB());
        }
    }

    private static final int LOADING_WIDTH = 16;
    private static final int LOADING_HEIGHT = 10;
    private static final int LOADING_UPOS = 160;
    private static final int LOADING_VPOS = 0;

    private void drawLoading(PoseStack matrixStack) {
        RenderSystem.setShaderTexture(0, BUTTON_RESOURCE);

        final int animation = this.tick % 40;
        int frame = animation / 10; // 0-3

        frame = frame > 2 ? 1 : frame; // 3rd frame is the same as 1st frame

        blit(matrixStack, this.x + this.width / 2 - LOADING_WIDTH / 2, this.y + 12, LOADING_UPOS, LOADING_VPOS + LOADING_HEIGHT * frame, LOADING_WIDTH, LOADING_HEIGHT, 256, 256);
    }

    public boolean isLoading() {
        if (this.parentScreen.getCurrentOffer() == null) {
            return false;
        }

        return this.parentScreen.getCurrentOffer().isLoading();
    }

    public boolean canProceed() {
        if (this.parentScreen.getCurrentOffer() == null) {
            return false;
        }

        return this.parentScreen.getCurrentOffer().isReady();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.canProceed()) {
            if (isPointInRegion(0, 0, OFFER_BUTTON_WIDTH, OFFER_BUTTON_HEIGHT, mouseX, mouseY)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.parentScreen.proceed();
            }
        }

        return false;
    }

    public void tick() {
        this.tick++;
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
