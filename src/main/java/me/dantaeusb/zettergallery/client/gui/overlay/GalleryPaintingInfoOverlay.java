package me.dantaeusb.zettergallery.client.gui.overlay;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.gui.overlay.PaintingInfoOverlay;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.security.InvalidParameterException;

public class GalleryPaintingInfoOverlay extends PaintingInfoOverlay {
    protected GalleryPaintingData paintingData;

    @Override
    public void setCanvasData(PaintingData galleryPaintingData) {
        if (!(galleryPaintingData instanceof GalleryPaintingData)) {
            throw new InvalidParameterException("This overlay should be used exclusively for Gallery Paintings");
        }

        this.paintingData = (GalleryPaintingData) galleryPaintingData;
        this.overlayMessageTime = 15 * 20;
    }

    public void hide() {
        this.paintingData = null;
    }

    @Override
    public void render(IngameGui gui, MatrixStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        if (this.paintingData == null) {
            return;
        }

        if (this.overlayMessageTime <= 0) {
            this.paintingData = null;
            return;
        }

        ITextComponent title;

        String paintingName = this.paintingData.getPaintingName();
        String authorName = this.paintingData.getAuthorName();

        if (StringUtils.isNullOrEmpty(paintingName)) {
            paintingName = new TranslationTextComponent("item.zetter.painting.unnamed").getString();
        }

        if (StringUtils.isNullOrEmpty(authorName)) {
            authorName = new TranslationTextComponent("item.zetter.painting.unknown").getString();
        }

        title = new TranslationTextComponent("item.zetter.customPaintingByAuthor", paintingName, authorName);

        float ticksLeft = (float)this.overlayMessageTime - partialTick;
        int msLeft = (int)(ticksLeft * 255.0F / 20.0F);
        if (msLeft > 255) {
            msLeft = 255;
        }

        if (msLeft > 8) {
            poseStack.pushPose();
            poseStack.translate(screenWidth / 2.0D, screenHeight - 68.0D, 0.0D);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int textColor = 0xFFFFFF;
            int transparencyMask = msLeft << 24 & 0xFF000000;

            int titleLength = gui.getFont().width(title);
            this.drawBackdrop(poseStack, gui.getFont(), -4, titleLength, 0xFFFFFF | transparencyMask);
            gui.getFont().drawShadow(poseStack, title, (float)(-titleLength / 2), -4.0F, textColor | transparencyMask);
            RenderSystem.disableBlend();

            poseStack.popPose();
        }
    }
}
