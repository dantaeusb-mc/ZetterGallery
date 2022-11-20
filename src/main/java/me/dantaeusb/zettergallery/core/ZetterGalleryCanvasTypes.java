package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zetter.core.ZetterRegistries;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ZetterGalleryCanvasTypes {
    private static final DeferredRegister<CanvasDataType<?>> CANVAS_TYPES = DeferredRegister.create(ZetterRegistries.CANVAS_TYPE_REGISTRY_NAME, ZetterGallery.MOD_ID);

    public static final RegistryObject<CanvasDataType<GalleryPaintingData>> GALLERY_PAINTING = CANVAS_TYPES.register(GalleryPaintingData.TYPE, () -> new CanvasDataType<>(
        GalleryPaintingData::createFresh,
        GalleryPaintingData::createWrap,
        GalleryPaintingData::load,
        GalleryPaintingData::readPacketData,
        GalleryPaintingData::writePacketData
    ));

    public static void init(IEventBus bus) {
        CANVAS_TYPES.register(bus);
    }
}
