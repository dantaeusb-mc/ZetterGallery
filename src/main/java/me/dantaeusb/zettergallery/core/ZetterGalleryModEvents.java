package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ZetterGalleryModEvents {
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
    public static void onCanvasSynced(CanvasRegisterEvent event) {
        event.getCanvasCode();

        if (Minecraft.getInstance().player.containerMenu == null) {
            return;
        }

        if (!(Minecraft.getInstance().player.containerMenu instanceof PaintingMerchantMenu)) {
            return;
        }

        PaintingMerchantMenu menu = (PaintingMerchantMenu) Minecraft.getInstance().player.containerMenu;
        menu.getContainer().updateCurrentOfferPaintingData(event.getCanvasCode(), (PaintingData) event.getCanvasData());
    }
}
