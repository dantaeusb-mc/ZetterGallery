package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.network.http.GalleryError;
import net.minecraft.network.FriendlyByteBuf;


public abstract class SAbstractErrorPacket {
    public final int code;
    public final String message;

    public SAbstractErrorPacket(GalleryError error) {
        this.code = error.getCode();
        this.message = error.getClientMessage();
    }

    public SAbstractErrorPacket(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public GalleryError getError() {
        return new GalleryError(this.code, this.message);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.code);
        networkBuffer.writeUtf(this.message, 32767);
    }
}