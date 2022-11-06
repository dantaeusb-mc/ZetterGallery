package me.dantaeusb.zettergallery.gallery;

import com.google.common.collect.Maps;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import java.util.BitSet;
import java.util.Map;
import java.util.UUID;

public class GalleryClientCapability implements IGalleryCapability {
    private Level world;
    Map<UUID, Object> paintingsMetadata = Maps.newHashMap();

    public GalleryClientCapability(Level world) {
        this.world = world;
    }

    @Override
    public Object getPaintingMetadata(UUID paintingId) {
        return this.paintingsMetadata.get(paintingId);
    }

    @Override
    public void registerPaintingMetadata(UUID paintingId, Object paintingMetadata) {
        this.paintingsMetadata.put(paintingId, paintingMetadata);
    }

    @Override
    public void unregisterPaintingMetadata(UUID paintingId) {
        this.paintingsMetadata.remove(paintingId);
    }

    @Override
    public Level getWorld() {
        return this.world;
    }
}
