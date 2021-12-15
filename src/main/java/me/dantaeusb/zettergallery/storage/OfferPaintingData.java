package me.dantaeusb.zettergallery.storage;

import com.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zettergallery.ZetterGallery;
import net.minecraft.nbt.CompoundTag;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Only for client-side purposes
 * Contains information about painting downloaded from Zetter Gallery
 */
public class OfferPaintingData extends AbstractCanvasData {
    private UUID uuid;
    private String authorName;
    private String title;

    private OfferPaintingData() {
        super();
    }

    public static OfferPaintingData create(UUID uuid, String authorName, String title, Resolution resolution,
                                           int width, int height, byte[] color
    ) {
        OfferPaintingData newPainting = new OfferPaintingData();
        newPainting.wrapData(resolution, width, height, color);
        newPainting.uuid = uuid;
        newPainting.authorName = authorName;
        newPainting.title = title;

        return newPainting;
    }

    protected void updateColorData(byte[] color) {
        // Don't check size mismatch cause we might use it as combined canvas

        this.color = color;
        this.canvasBuffer = ByteBuffer.wrap(this.color);
        this.canvasBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    public boolean isEditable() {
        return false;
    }

    public Type getType() {
        return Type.DUMMY;
    }

    public UUID getUniqueId() {
        return this.uuid;
    }

    public String getPaintingName() {
        return this.title;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public void read(CompoundTag compound) {
        ZetterGallery.LOG.error("Trying to read into dummy canvas!");
    }

    public CompoundTag write(CompoundTag compound) {
        ZetterGallery.LOG.error("Trying to save dummy canvas!");

        return compound;
    }
}

