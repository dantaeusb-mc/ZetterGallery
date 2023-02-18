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
public class CAuthorizationCheckPacket {

    public CAuthorizationCheckPacket() {
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CAuthorizationCheckPacket readPacketData(PacketBuffer buf) {
        CAuthorizationCheckPacket packet = new CAuthorizationCheckPacket();

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {

    }

    public static void handle(final CAuthorizationCheckPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            ZetterGallery.LOG.warn("EntityPlayerMP was null when CGalleryAuthenticationCheckPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processGalleryAuthenticationRequest(packetIn, sendingPlayer));
    }
}