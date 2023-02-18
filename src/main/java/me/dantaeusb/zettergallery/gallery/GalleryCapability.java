package me.dantaeusb.zettergallery.gallery;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class GalleryCapability {
    @CapabilityInject(Gallery.class)
    public static Capability<Gallery> CAPABILITY_GALLERY;

    public static void register() {
        CapabilityManager.INSTANCE.register(
            GalleryServer.class,
            new GalleryServer.GalleryStorage(),
            GalleryServer::new
        );
    }
}
