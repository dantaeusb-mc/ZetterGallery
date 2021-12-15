package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Absolute copy of request packet but we have to copy them cause
 * there's no way to determine which purpose packet is used for
 * unless they're different classes for some reason
 */
public class CGalleryOffersRequestPacket {

    public CGalleryOffersRequestPacket() {
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CGalleryOffersRequestPacket readPacketData(FriendlyByteBuf buf) {
        CGalleryOffersRequestPacket packet = new CGalleryOffersRequestPacket();

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {

    }

    public static void handle(final CGalleryOffersRequestPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            ZetterGallery.LOG.warn("EntityPlayerMP was null when CGalleryAuthenticationCheckPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processGalleryOffersRequest(packetIn, sendingPlayer));
    }
}