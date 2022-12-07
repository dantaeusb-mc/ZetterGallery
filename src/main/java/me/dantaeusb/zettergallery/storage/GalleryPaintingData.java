package me.dantaeusb.zettergallery.storage;

import me.dantaeusb.zetter.client.gui.overlay.PaintingInfoOverlay;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataBuilder;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.ZetterGalleryCanvasTypes;
import me.dantaeusb.zettergallery.core.ZetterGalleryOverlays;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Only for client-side purposes
 * Contains information about painting downloaded from Zetter Gallery
 *
 * @todo: [LOW] Think about hiding real player UUID in Gallery
 */
public class GalleryPaintingData extends PaintingData {
    public static final String TYPE = "painting";
    public static final String CODE_PREFIX = ZetterGallery.MOD_ID + "_" + TYPE + "_";

    public static final CanvasDataBuilder<GalleryPaintingData> BUILDER = new GalleryPaintingDataBuilder();

    protected static final String NBT_TAG_GALLERY_UUID = "ZetterGalleryUuid";

    private UUID galleryPaintingUuid;

    public static String getCanvasCode(UUID canvasId) {
        return CODE_PREFIX + canvasId.toString();
    }

    /**
     * For offers onlu
     *
     * Returns dummy canvas data code for temporary gallery paintings
     * used to store canvas data in painting merchant offers
     * @return
     */
    public static String getDummyOfferCanvasCode(UUID galleryPaintingUuid) {
        return ZetterGallery.MOD_ID + "_offer_" + Long.toHexString(galleryPaintingUuid.getMostSignificantBits());
    }

    protected GalleryPaintingData() {}

    public void setMetaProperties(UUID galleryPaintingUuid, UUID authorUuid, String authorName, String name) {
        this.galleryPaintingUuid = galleryPaintingUuid;
        super.setMetaProperties(authorUuid, authorName, name);
    }

    public boolean isEditable() {
        return false;
    }

    @Override
    public PaintingInfoOverlay getOverlay() {
        return ZetterGalleryOverlays.GALLERY_PAINTING_INFO;
    }

    @Override
    public CanvasDataType<GalleryPaintingData> getType() {
        return ZetterGalleryCanvasTypes.GALLERY_PAINTING.get();
    }

    @Override
    public void correctData(ServerLevel level) {
        if (this.authorUuid == null) {
            this.authorUuid = new UUID(0L, 0L);
        }
    }

    public UUID getUUID() {
        return this.galleryPaintingUuid;
    }

    public CompoundTag save(CompoundTag compoundTag) {
        super.save(compoundTag);

        compoundTag.putUUID(NBT_TAG_GALLERY_UUID, this.galleryPaintingUuid);

        return compoundTag;
    }

    private static class GalleryPaintingDataBuilder implements CanvasDataBuilder<GalleryPaintingData> {
        public GalleryPaintingData createFresh(AbstractCanvasData.Resolution resolution, int width, int height) {
            GalleryPaintingData newPainting = new GalleryPaintingData();
            byte[] color = new byte[width * height * 4];
            ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

            for(int x = 0; x < width * height; ++x) {
                defaultColorBuffer.putInt(x * 4, Helper.CANVAS_COLOR);
            }

            newPainting.wrapData(resolution, width, height, color);
            return newPainting;
        }

        public GalleryPaintingData createWrap(AbstractCanvasData.Resolution resolution, int width, int height, byte[] color) {
            GalleryPaintingData newPainting = new GalleryPaintingData();
            newPainting.wrapData(resolution, width, height, color);
            return newPainting;
        }

        /*
         * Serialization
         */

        public GalleryPaintingData load(CompoundTag compoundTag) {
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

            if (compoundTag.contains(NBT_TAG_AUTHOR_UUID)) {
                newPainting.authorUuid = compoundTag.getUUID(NBT_TAG_AUTHOR_UUID);
            } else {
                newPainting.authorUuid = null;
            }

            newPainting.authorName = compoundTag.getString(NBT_TAG_AUTHOR_NAME);
            newPainting.name = compoundTag.getString(NBT_TAG_NAME);
            newPainting.banned = compoundTag.getBoolean(NBT_TAG_BANNED);
            newPainting.galleryPaintingUuid = compoundTag.getUUID(NBT_TAG_GALLERY_UUID);

            return newPainting;
        }

        /*
         * Networking
         */

        public GalleryPaintingData readPacketData(FriendlyByteBuf networkBuffer) {
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

            final UUID galleryPaintingUuid = networkBuffer.readUUID();
            final UUID authorUuid = networkBuffer.readUUID();
            final String authorName = networkBuffer.readUtf(64);
            final String title = networkBuffer.readUtf(32);

            newPainting.setMetaProperties(
                galleryPaintingUuid,
                authorUuid,
                authorName,
                title
            );

            return newPainting;
        }

        public void writePacketData(GalleryPaintingData canvasData, FriendlyByteBuf networkBuffer) {
            networkBuffer.writeByte(canvasData.resolution.ordinal());
            networkBuffer.writeInt(canvasData.width);
            networkBuffer.writeInt(canvasData.height);
            networkBuffer.writeInt(canvasData.getColorDataBuffer().remaining());
            networkBuffer.writeBytes(canvasData.getColorDataBuffer());
            networkBuffer.writeUUID(canvasData.galleryPaintingUuid);
            networkBuffer.writeUUID(canvasData.authorUuid);
            networkBuffer.writeUtf(canvasData.authorName, 64);
            networkBuffer.writeUtf(canvasData.name, 32);
        }
    }
}

