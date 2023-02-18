package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zetter.core.ZetterRegistries;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class ZetterGalleryCanvasTypes {
    private static final DeferredRegister<CanvasDataType<?>> CANVAS_TYPES = DeferredRegister.create(ZetterRegistries.HACKY_TYPE, ZetterGallery.MOD_ID);

    public static final RegistryObject<CanvasDataType<GalleryPaintingData>> GALLERY_PAINTING = CANVAS_TYPES.register(GalleryPaintingData.TYPE, () -> new CanvasDataType<>(
        GalleryPaintingData.BUILDER
    ));

    public static void init(IEventBus bus) {
        CANVAS_TYPES.register(bus);
    }
}
