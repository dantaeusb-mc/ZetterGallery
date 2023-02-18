package me.dantaeusb.zettergallery.capability.gallery;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class GalleryCapability {
    @CapabilityInject(Gallery.class)
    public static Capability<Gallery> CAPABILITY_GALLERY;

    public static void register() {
        CapabilityManager.INSTANCE.register(
            Gallery.class,
            new GalleryStorage(),
            GalleryServer::new
        );
    }
}
