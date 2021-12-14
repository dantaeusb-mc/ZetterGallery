package com.dantaeusb.zettergallery.storage;

import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zettergallery.ZetterGallery;
import net.minecraft.nbt.CompoundTag;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import com.dantaeusb.zetter.storage.AbstractCanvasData.Type;

/**
 * Only for client-side purposes
 * Contains information about painting downloaded from Zetter Gallery
 */
public class OfferPaintingData extends AbstractCanvasData {
    private final UUID uuid;
    private final String authorName;
    private final String title;

    public OfferPaintingData(UUID uuid, String authorName, String title) {
        super();

        this.uuid = uuid;
        this.authorName = authorName;
        this.title = title;
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

