package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterGalleryGameEvents {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        ConnectionManager.getInstance().update();
    }

    /**
     *
     * @param event
     */
    @SubscribeEvent
    public static void onPlayerContainerOpened(PlayerContainerEvent.Open event) {
        if (event.getEntity().level.isClientSide()) {
            return;
        }

        if (event.getContainer() instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) event.getContainer()).updateAuthorizationState();
        }
    }
}
