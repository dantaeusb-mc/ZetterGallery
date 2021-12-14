package com.dantaeusb.zettergallery.network.packet;

import com.dantaeusb.zettergallery.ZetterGallery;
import com.dantaeusb.zettergallery.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @todo: Is that okay that we don't have classic handler here?
 */
public class SGalleryErrorPacket {
    private final String message;

    public SGalleryErrorPacket(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SGalleryErrorPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String message = networkBuffer.readUtf(32767);

            return new SGalleryErrorPacket(message);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGalleryAuthenticationPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.message, 32767);
    }

    public static void handle(final SGalleryErrorPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryErrorPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processPaintingMerchantError(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryErrorPacket[message=" + this.message + "]";
    }
}