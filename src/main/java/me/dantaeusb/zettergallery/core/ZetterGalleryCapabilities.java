package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.GalleryCapabilityProvider;
import me.dantaeusb.zettergallery.gallery.GalleryServerCapability;
import me.dantaeusb.zettergallery.gallery.IGalleryCapability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterGalleryCapabilities
{
    public static Capability<IGalleryCapability> GALLERY = CapabilityManager.get(new CapabilityToken<>(){});

    private static ResourceLocation GALLERY_CAPABILITY_LOCATION = new ResourceLocation(ZetterGallery.MOD_ID, "gallery_capability");

    @SubscribeEvent
    public static void attachCapabilityToWorldHandler(AttachCapabilitiesEvent<Level> event) {
        Level world = event.getObject();

        // This capability only exists on server in the overworld
        if (!world.isClientSide() && world.dimension() == Level.OVERWORLD) {
            event.addCapability(GALLERY_CAPABILITY_LOCATION, new GalleryCapabilityProvider(world));
        }
    }

    @SubscribeEvent
    public static void registerCapabilityHandler(RegisterCapabilitiesEvent event) {
        event.register(IGalleryCapability.class);
    }
}