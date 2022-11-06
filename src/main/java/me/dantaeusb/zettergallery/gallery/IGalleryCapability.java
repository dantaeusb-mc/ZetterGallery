package me.dantaeusb.zettergallery.gallery;

import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.UUID;

public interface IGalleryCapability {
    Level getWorld();

    Object getPaintingMetadata(UUID paintingId);

    void registerPaintingMetadata(UUID paintingId, Object paintingMetadata);

    void unregisterPaintingMetadata(UUID paintingId);
}
