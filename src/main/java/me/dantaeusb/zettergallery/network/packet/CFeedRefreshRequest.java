package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.ServerHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Asks player to check if token is authorized after
 * cross-authorization attempt
 */
public class CFeedRefreshRequest {
    public CFeedRefreshRequest() {
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CFeedRefreshRequest readPacketData(PacketBuffer buf) {
        CFeedRefreshRequest packet = new CFeedRefreshRequest();

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {

    }

    public static void handle(final CFeedRefreshRequest packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            ZetterGallery.LOG.warn("EntityPlayerMP was null when CGalleryFeedRefreshRequest was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processGalleryFeedRefreshRequest(packetIn, sendingPlayer));
    }
}