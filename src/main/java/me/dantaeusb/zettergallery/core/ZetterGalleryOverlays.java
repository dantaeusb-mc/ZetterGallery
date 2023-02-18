package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.client.gui.overlay.GalleryPaintingInfoOverlay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ZetterGalleryOverlays {
    public static GalleryPaintingInfoOverlay GALLERY_PAINTING_INFO = new GalleryPaintingInfoOverlay();

    @SubscribeEvent
    public static void onRenderOverlays(RenderGameOverlayEvent.Post event) {
        if(event.getType().equals(RenderGameOverlayEvent.ElementType.ALL)) {
            GALLERY_PAINTING_INFO.render(Minecraft.getInstance().gui, event.getMatrixStack(), event.getPartialTicks(), event.getWindow().getWidth(), event.getWindow().getHeight());
        }
    }
}
