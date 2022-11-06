package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterGalleryServerEvents {
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void serverStarted(final ServerStartedEvent event)
    {
        ConnectionManager.initialize(event.getServer().overworld());
        ConnectionManager.getInstance().handleServerStart(event.getServer());
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void serverStopped(final ServerStoppingEvent event)
    {
        ConnectionManager.getInstance().handleServerStop(event.getServer());
        ConnectionManager.close();
    }
}
