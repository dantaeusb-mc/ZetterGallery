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

/**
 * Handling auth errors
 */
public class SGalleryAuthErrorPacket extends SGalleryAbstractErrorPacket {
    public SGalleryAuthErrorPacket(GalleryError error) {
        super(error);
    }

    public SGalleryAuthErrorPacket(int code, String message) {
        super(code, message);
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SGalleryAuthErrorPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            int code = networkBuffer.readInt();
            String message = networkBuffer.readUtf(32767);

            return new SGalleryAuthErrorPacket(code, message);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGalleryAuthErrorPacket: " + e);
            return null;
        }
    }

    public static void handle(final SGalleryAuthErrorPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryAuthErrorPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processPaintingMerchantAuthError(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryAuthErrorPacket[code=" + this.code + ",message=" + this.message + "]";
    }
}