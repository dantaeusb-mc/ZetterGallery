package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.gallery.SalesManager;
import net.minecraftforge.event.TickEvent;
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
        SalesManager.initialize();
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void serverStopped(final ServerStoppingEvent event)
    {
        ConnectionManager.getInstance().handleServerStop(event.getServer());
        ConnectionManager.closeConnection();
        SalesManager.close();
    }

    @SubscribeEvent
    public static void serverTick(final TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            ConnectionManager.getInstance().handleTick(event.getServer());
        }
    }
}
