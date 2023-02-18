package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.ServerHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Absolute copy of request packet but we have to copy them cause
 * there's no way to determine which purpose packet is used for
 * unless they're different classes for some reason
 */
public class CSelectOfferPacket {
    public final int offerIndex;

    public CSelectOfferPacket(int offerIndex) {
        this.offerIndex = offerIndex;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CSelectOfferPacket readPacketData(PacketBuffer buf) {
        final int offerIndex = buf.readInt();

        CSelectOfferPacket packet = new CSelectOfferPacket(offerIndex);

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeInt(this.offerIndex);
    }

    public static void handle(final CSelectOfferPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            ZetterGallery.LOG.warn("EntityPlayerMP was null when CGallerySelectOfferPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processGallerySelectOffer(packetIn, sendingPlayer));
    }
}