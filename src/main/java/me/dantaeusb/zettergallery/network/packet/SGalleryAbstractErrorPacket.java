package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.ClientHandler;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;


public abstract class SGalleryAbstractErrorPacket {
    public final int code;
    public final String message;

    public SGalleryAbstractErrorPacket(GalleryError error) {
        this.code = error.getCode();
        this.message = error.getClientMessage();
    }

    public SGalleryAbstractErrorPacket(int code, String message) {
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