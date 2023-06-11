package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zetter.core.ZetterOverlays;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.client.gui.overlay.GalleryPaintingInfoOverlay;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ZetterGalleryOverlays {
    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        GalleryPaintingInfoOverlay overlay = new GalleryPaintingInfoOverlay();
        ZetterOverlays.OVERLAYS.put(GalleryPaintingData.OVERLAY_KEY, overlay);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), overlay.getId(), overlay);
    }
}
