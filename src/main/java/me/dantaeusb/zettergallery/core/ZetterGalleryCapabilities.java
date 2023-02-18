package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.capability.gallery.GalleryCapabilityProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterGalleryCapabilities
{
    private static ResourceLocation GALLERY_CAPABILITY_LOCATION = new ResourceLocation(ZetterGallery.MOD_ID, "gallery_capability");

    @SubscribeEvent
    public static void attachCapabilityToWorldHandler(AttachCapabilitiesEvent<World> event) {
        World world = event.getObject();

        // This capability only exists on server in the overworld
        if (!world.isClientSide() && world.dimension() == World.OVERWORLD) {
            event.addCapability(GALLERY_CAPABILITY_LOCATION, new GalleryCapabilityProvider(world));
        }
    }
}