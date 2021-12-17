package me.dantaeusb.zettergallery.storage;

import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Only for client-side purposes
 * Contains information about painting downloaded from Zetter Gallery
 */
public class GalleryPaintingData extends PaintingData {
    public static final String CODE_PREFIX = ZetterGallery.MOD_ID + "_painting_";

    protected static final String NBT_TAG_UUID = "gallery_uuid";

    private UUID uuid;

    public static String getCanvasCode(UUID canvasId) {
        return CODE_PREFIX + canvasId.toString();
    }

    protected GalleryPaintingData() {
        super();
    }

    public static GalleryPaintingData create(UUID uuid, String authorName, String title, Resolution resolution,
                                             int width, int height, byte[] color
    ) {
        GalleryPaintingData newPainting = new GalleryPaintingData();
        newPainting.wrapData(resolution, width, height, color);
        newPainting.uuid = uuid;
        newPainting.authorName = authorName;
        newPainting.title = title;

        return newPainting;
    }

    public boolean isEditable() {
        return false;
    }

    /**
     * @todo: change type!
     * @return
     */
    public Type getType() {
        return Type.DUMMY;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public String getPaintingName() {
        return this.title;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public void read(CompoundTag compoundTag) {
        super.load(compoundTag);

        this.uuid = compoundTag.getUUID(NBT_TAG_UUID);
    }

    public CompoundTag write(CompoundTag compoundTag) {
        super.save(compoundTag);

        compoundTag.putUUID(NBT_TAG_UUID, this.uuid);

        return compoundTag;
    }
}

