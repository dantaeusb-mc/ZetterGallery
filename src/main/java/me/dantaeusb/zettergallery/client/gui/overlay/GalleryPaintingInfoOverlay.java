package me.dantaeusb.zettergallery.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.overlay.PaintingInfoOverlay;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class GalleryPaintingInfoOverlay extends PaintingInfoOverlay implements IGuiOverlay {
    private GalleryPaintingData paintingData = null;
    private int overlayMessageTime = 0;

    public void setPainting(GalleryPaintingData galleryPaintingData) {
        this.paintingData = galleryPaintingData;
        this.overlayMessageTime = 15 * 20;
    }

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        if (this.paintingData == null) {
            return;
        }

        if (this.overlayMessageTime <= 0) {
            this.paintingData = null;
            return;
        }

        Component title;

        String paintingName = this.paintingData.getPaintingTitle();
        String authorName = this.paintingData.getAuthorName();

        if (StringUtil.isNullOrEmpty(paintingName)) {
            paintingName = Component.translatable("item.zetter.painting.unnamed").getString();
        }

        if (StringUtil.isNullOrEmpty(authorName)) {
            authorName = Component.translatable("item.zetter.painting.unknown").getString();
        }

        title = Component.translatable("item.zetter.customPaintingByAuthor", paintingName, authorName);

        float ticksLeft = (float)this.overlayMessageTime - partialTick;
        int msLeft = (int)(ticksLeft * 255.0F / 20.0F);
        if (msLeft > 255) {
            msLeft = 255;
        }

        if (msLeft > 8) {
            poseStack.pushPose();
            poseStack.translate(screenWidth / 2, screenHeight - 68, 0.0D);
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
