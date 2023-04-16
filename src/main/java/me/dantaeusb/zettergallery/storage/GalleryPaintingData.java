package me.dantaeusb.zettergallery.storage;

import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataBuilder;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.ZetterGalleryCanvasTypes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;

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
    public static final ResourceLocation OVERLAY_KEY = new ResourceLocation(ZetterGallery.MOD_ID, "gallery_painting_info");

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

    /**
     * Specifically for sale offer
     *
     * @return
     */
    public static String getDummySaleOfferCanvasCode() {
        return ZetterGallery.MOD_ID + "_offer_sale";
    }

    protected GalleryPaintingData(String canvasCode) {
        super(canvasCode);
    }

    public void setMetaProperties(UUID galleryPaintingUuid, UUID authorUuid, String authorName, String name) {
        this.galleryPaintingUuid = galleryPaintingUuid;
        super.setMetaProperties(authorUuid, authorName, name);
    }

    public boolean isEditable() {
        return false;
    }

    @Override
    public ResourceLocation getOverlay() {
        return OVERLAY_KEY;
    }

    @Override
    public CanvasDataType<GalleryPaintingData> getType() {
        return ZetterGalleryCanvasTypes.GALLERY_PAINTING.get();
    }

    @Override
    public void correctData(ServerWorld level) {
        if (this.authorUuid == null) {
            this.authorUuid = new UUID(0L, 0L);
        }
    }

    public UUID getUUID() {
        return this.galleryPaintingUuid;
    }

    @Override
    public CompoundNBT save(CompoundNBT compoundTag) {
        super.save(compoundTag);

        compoundTag.putUUID(NBT_TAG_GALLERY_UUID, this.galleryPaintingUuid);

        return compoundTag;
    }

    @Override
    public void load(CompoundNBT compoundTag) {
        this.width = compoundTag.getInt(NBT_TAG_WIDTH);
        this.height = compoundTag.getInt(NBT_TAG_HEIGHT);

        if (compoundTag.contains(NBT_TAG_RESOLUTION)) {
            int resolutionOrdinal = compoundTag.getInt(NBT_TAG_RESOLUTION);
            this.resolution = Resolution.values()[resolutionOrdinal];
        } else {
            this.resolution = Helper.getResolution();
        }

        this.updateColorData(compoundTag.getByteArray(NBT_TAG_COLOR));

        if (compoundTag.contains(NBT_TAG_AUTHOR_UUID)) {
            this.authorUuid = compoundTag.getUUID(NBT_TAG_AUTHOR_UUID);
        } else {
            this.authorUuid = null;
        }

        this.authorName = compoundTag.getString(NBT_TAG_AUTHOR_NAME);
        this.name = compoundTag.getString(NBT_TAG_NAME);
        this.banned = compoundTag.getBoolean(NBT_TAG_BANNED);
        this.galleryPaintingUuid = compoundTag.getUUID(NBT_TAG_GALLERY_UUID);
    }

    private static class GalleryPaintingDataBuilder implements CanvasDataBuilder<GalleryPaintingData> {
        @Override
        public GalleryPaintingData supply(String canvasCode) {
            return new GalleryPaintingData(canvasCode);
        }

        @Override
        public GalleryPaintingData createFresh(String canvasCode, AbstractCanvasData.Resolution resolution, int width, int height) {
            GalleryPaintingData newPainting = new GalleryPaintingData(canvasCode);
            byte[] color = new byte[width * height * 4];
            ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

            for(int x = 0; x < width * height; ++x) {
                defaultColorBuffer.putInt(x * 4, Helper.CANVAS_COLOR);
            }

            newPainting.wrapData(resolution, width, height, color);
            return newPainting;
        }

        @Override
        public GalleryPaintingData createWrap(String canvasCode, AbstractCanvasData.Resolution resolution, int width, int height, byte[] color) {
            GalleryPaintingData newPainting = new GalleryPaintingData(canvasCode);
            newPainting.wrapData(resolution, width, height, color);
            return newPainting;
        }

        /*
         * Networking
         */

        @Override
        public GalleryPaintingData readPacketData(PacketBuffer networkBuffer) {
            String canvasCode = networkBuffer.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);
            final GalleryPaintingData newPainting = new GalleryPaintingData(canvasCode);

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

        @Override
        public void writePacketData(String canvasCode, GalleryPaintingData canvasData, PacketBuffer networkBuffer) {
            networkBuffer.writeUtf(canvasCode, Helper.CANVAS_CODE_MAX_LENGTH);
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

