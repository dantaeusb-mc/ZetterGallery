package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.ClientHandler;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Handling offer errors
 */
public class SOffersErrorPacket extends SAbstractErrorPacket {
    public SOffersErrorPacket(GalleryError error) {
        super(error);
    }

    public SOffersErrorPacket(int code, String message) {
        super(code, message);
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SOffersErrorPacket readPacketData(PacketBuffer networkBuffer) {
        try {
            int code = networkBuffer.readInt();
            String message = networkBuffer.readUtf(32767);

            return new SOffersErrorPacket(code, message);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGalleryOffersErrorPacket: " + e);
            return null;
        }
    }

    public static void handle(final SOffersErrorPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<World> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryOffersErrorPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processPaintingMerchantOffersError(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryOffersErrorPacket[code=" + this.code + ",message=" + this.message + "]";
    }
}