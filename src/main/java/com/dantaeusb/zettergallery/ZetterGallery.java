package com.dantaeusb.zettergallery;

import com.dantaeusb.zettergallery.core.Config;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ZetterGallery.MOD_ID)
public class ZetterGallery
{
    public static final String MOD_ID = "zetter_gallery";
    public static boolean DEBUG_MODE = false;

    // get a reference to the event bus for this mod;  Registration events are fired on this bus.
    public static IEventBus MOD_EVENT_BUS;

    // Directly reference a log4j logger.
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static ZetterGallery instance;

    public static boolean quarkEnabled;

    public ZetterGallery() {
        instance = this;

        quarkEnabled = ModList.get().isLoaded("quark");
        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);
    }
}
