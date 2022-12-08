package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.SalesManager;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterGalleryGameEvents {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        SalesManager.getInstance().tick();
    }

    /**
     * We need this because we need to start flow
     * only when container is opened and initialized on both sides
     * @param event
     */
    @SubscribeEvent
    public static void onPlayerContainerOpened(PlayerContainerEvent.Open event) {
        if (event.getEntity().level.isClientSide()) {
            return;
        }

        if (event.getContainer() instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) event.getContainer()).getAuthController().startFlow();
        }
    }
}
