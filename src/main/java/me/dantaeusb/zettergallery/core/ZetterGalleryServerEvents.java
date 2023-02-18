package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.gallery.SalesManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterGalleryServerEvents {
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void serverStarted(final FMLServerStartedEvent event)
    {
        ConnectionManager.initialize(event.getServer().overworld());
        ConnectionManager.getInstance().handleServerStart(event.getServer());
        SalesManager.initialize();
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void serverStopped(final FMLServerStoppedEvent event)
    {
        ConnectionManager.getInstance().handleServerStop(event.getServer());
        ConnectionManager.closeConnection();
        SalesManager.close();
    }
}
