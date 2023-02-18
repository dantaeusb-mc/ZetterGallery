package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.gallery.Gallery;
import me.dantaeusb.zettergallery.gallery.GalleryCapability;
import net.minecraft.world.World;

public class Helper {
    private static Helper instance;

    public static final String GALLERY_AUTH_SERVER_ENDPOINT = "auth/consent";

    public static final String GALLERY_FRONTEND = "https://zetter.gallery/";
    public static final String GALLERY_API = "https://api.zetter.gallery/";

    public static final String LOCALHOST_FRONTEND = "http://localhost:8080/";
    public static final String LOCALHOST_API = "http://localhost:3100/";

    public static final int GALLERY_CROSS_AUTH_CODE_LENGTH = 12;

    public static Helper getInstance() {
        if (Helper.instance == null) {
            Helper.instance = new Helper();
        }

        return Helper.instance;
    }

    public static Gallery getWorldGalleryCapability(World world) {
        Gallery galleryCapability;

        if (!world.isClientSide()) {
            // looking for a server canvas tracker in the overworld, since canvases are world-independent
            galleryCapability = world.getServer().overworld().getCapability(GalleryCapability.CAPABILITY_GALLERY).orElse(null);
        } else {
            galleryCapability = world.getCapability(GalleryCapability.CAPABILITY_GALLERY).orElse(null);
        }

        return galleryCapability;
    }
}
