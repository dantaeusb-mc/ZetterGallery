package me.dantaeusb.zettergallery.storage;

import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Only for client-side purposes
 * Contains information about painting downloaded from Zetter Gallery
 */
public class GalleryPaintingData extends PaintingData {
    public static final String TYPE = "gallery_painting";
    public static final String CODE_PREFIX = ZetterGallery.MOD_ID + "_" + TYPE + "_";

    protected static final String NBT_TAG_UUID = "ZetterGalleryUuid";

    private UUID uuid;

    public static String getCanvasCode(UUID canvasId) {
        return CODE_PREFIX + canvasId.toString();
    }

    protected GalleryPaintingData() {
        super();
    }

    public static GalleryPaintingData createFresh(AbstractCanvasData.Resolution resolution, int width, int height) {
        GalleryPaintingData newPainting = new GalleryPaintingData();
        byte[] color = new byte[width * height * 4];
        ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

        for(int x = 0; x < width * height; ++x) {
            defaultColorBuffer.putInt(x * 4, Helper.CANVAS_COLOR);
        }

        newPainting.wrapData(resolution, width, height, color);
        return newPainting;
    }

    public static GalleryPaintingData createWrap(AbstractCanvasData.Resolution resolution, int width, int height, byte[] color) {
        GalleryPaintingData newPainting = new GalleryPaintingData();
        newPainting.wrapData(resolution, width, height, color);
        return newPainting;
    }

    public void setMetaProperties(UUID uuid, String authorName, String title) {
        this.uuid = uuid;
        super.setMetaProperties(authorName, title);
    }

    public boolean isEditable() {
        return false;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    /*
     * Serialization
     */

    public static GalleryPaintingData load(CompoundTag compoundTag) {
        final GalleryPaintingData newPainting = new GalleryPaintingData();

        newPainting.width = compoundTag.getInt(NBT_TAG_WIDTH);
        newPainting.height = compoundTag.getInt(NBT_TAG_HEIGHT);

        if (compoundTag.contains(NBT_TAG_RESOLUTION)) {
            int resolutionOrdinal = compoundTag.getInt(NBT_TAG_RESOLUTION);
            newPainting.resolution = Resolution.values()[resolutionOrdinal];
        } else {
            newPainting.resolution = Helper.getResolution();
        }

        newPainting.updateColorData(compoundTag.getByteArray(NBT_TAG_COLOR));

        newPainting.authorName = compoundTag.getString(NBT_TAG_AUTHOR_NAME);
        newPainting.title = compoundTag.getString(NBT_TAG_TITLE);
        newPainting.banned = compoundTag.getBoolean(NBT_TAG_BANNED);

        return newPainting;
    }

    public CompoundTag save(CompoundTag compoundTag) {
        super.save(compoundTag);

        compoundTag.putUUID(NBT_TAG_UUID, this.uuid);

        return compoundTag;
    }

    /*
     * Networking
     */

    public static GalleryPaintingData readPacketData(FriendlyByteBuf networkBuffer) {
        final GalleryPaintingData newPainting = new GalleryPaintingData();

        final byte resolutionOrdinal = networkBuffer.readByte();
        AbstractCanvasData.Resolution resolution = AbstractCanvasData.Resolution.values()[resolutionOrdinal];

        final int width = networkBuffer.readInt();
        final int height = networkBuffer.readInt();

        final int colorDataSize = networkBuffer.readInt();
        ByteBuffer colorData = networkBuffer.readBytes(colorDataSize).nioBuffer();
        byte[] unwrappedColorData = new byte[width * height * 4];
        colorData.get(unwrappedColorData);

        newPainting.wrapData(
            resolution,
            width,
            height,
            unwrappedColorData
        );

        final UUID uuid = networkBuffer.readUUID();
        final String authorName = networkBuffer.readUtf(64);
        final String title = networkBuffer.readUtf(32);

        newPainting.setMetaProperties(
            uuid,
            authorName,
            title
        );

        return newPainting;
    }

    public static void writePacketData(GalleryPaintingData canvasData, FriendlyByteBuf networkBuffer) {
        networkBuffer.writeByte(canvasData.resolution.ordinal());
        networkBuffer.writeInt(canvasData.width);
        networkBuffer.writeInt(canvasData.height);
        networkBuffer.writeInt(canvasData.getColorDataBuffer().remaining());
        networkBuffer.writeBytes(canvasData.getColorDataBuffer());
        networkBuffer.writeUUID(canvasData.uuid);
        networkBuffer.writeUtf(canvasData.authorName, 64);
        networkBuffer.writeUtf(canvasData.title, 32);
    }
}

