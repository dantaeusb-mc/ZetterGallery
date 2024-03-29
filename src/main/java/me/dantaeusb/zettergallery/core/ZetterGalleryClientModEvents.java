package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zetter.client.gui.overlay.CanvasOverlay;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterOverlays;
import me.dantaeusb.zetter.event.CanvasOverlayViewEvent;
import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.client.gui.overlay.GalleryPaintingInfoOverlay;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ZetterGalleryClientModEvents {
    /**
     * Handle event when new canvas registered,
     * used to handle the moment when canvas that was not
     * previously loaded used in GUI (i.e. for selling)
     *
     * In order to update offer with actual canvas data,
     * it lookups the offer and updates data
     * @param event
     */
    @SubscribeEvent
    public static void onCanvasSynced(CanvasRegisterEvent.Post event) {
        if (!event.level.isClientSide()) {
            return;
        }

        if (Minecraft.getInstance().player.containerMenu instanceof PaintingMerchantMenu paintingMerchantMenu) {
            // We use both gallery painting for purchase offers and zetter paintings for sale
            if (event.canvasData.getType().equals(ZetterCanvasTypes.PAINTING.get())) {
                paintingMerchantMenu.getContainer().updateSaleOfferPaintingData(event.canvasCode, (PaintingData) event.canvasData);
            }
        }
    }

    @SubscribeEvent
    public static void overlayViewEvent(CanvasOverlayViewEvent<?> event) {
        if (event.canvasData.getType().equals(ZetterGalleryCanvasTypes.GALLERY_PAINTING.get())) {
            CanvasOverlay<?> overlay = ZetterOverlays.OVERLAYS.get(GalleryPaintingData.OVERLAY_KEY);

            if (overlay instanceof GalleryPaintingInfoOverlay) {
                ((GalleryPaintingInfoOverlay) overlay).setCanvasData((GalleryPaintingData) event.canvasData);
            }
        }
    }
}
