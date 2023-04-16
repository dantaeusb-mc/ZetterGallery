package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zetter.client.gui.overlay.PaintingInfoOverlay;
import me.dantaeusb.zetter.core.ZetterOverlays;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.client.gui.overlay.GalleryPaintingInfoOverlay;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ZetterGalleryOverlays {
    public static void register() {
        GalleryPaintingInfoOverlay overlay = new GalleryPaintingInfoOverlay();
        ZetterOverlays.OVERLAYS.put(GalleryPaintingData.OVERLAY_KEY, overlay);
    }
}
