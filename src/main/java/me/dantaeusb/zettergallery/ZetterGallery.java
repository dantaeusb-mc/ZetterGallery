package me.dantaeusb.zettergallery;

import me.dantaeusb.zetter.core.ZetterContainerMenus;
import me.dantaeusb.zettergallery.core.ZetterGalleryContainerMenus;
import me.dantaeusb.zettergallery.core.ZetterGalleryVillagers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ZetterGallery.MOD_ID)
public class ZetterGallery
{
    public static final String MOD_ID = "zettergallery";
    public static boolean DEBUG_MODE = false;
    public static boolean DEBUG_LOCALHOST = true;

    // get a reference to the event bus for this mod;  Registration events are fired on this bus.
    public static IEventBus MOD_EVENT_BUS;

    // Directly reference a log4j logger.
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static ZetterGallery instance;

    public ZetterGallery() {
        instance = this;

        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();

        ZetterGalleryVillagers.init(MOD_EVENT_BUS);
        ZetterGalleryContainerMenus.init(MOD_EVENT_BUS);
    }
}
