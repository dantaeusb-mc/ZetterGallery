package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @todo: Is that okay that we don't have classic handler here?
 */
public class SMerchantInfoPacket {
    public final UUID merchantId;
    public final int merchantLevel;

    public SMerchantInfoPacket(UUID merchantId, int merchantLevel) {
        this.merchantId = merchantId;
        this.merchantLevel = merchantLevel;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SMerchantInfoPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            UUID merchantId = networkBuffer.readUUID();
            int merchantLevel = networkBuffer.readInt();

            return new SMerchantInfoPacket(merchantId, merchantLevel);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGalleryMerchantInfoPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUUID(this.merchantId);
        networkBuffer.writeInt(this.merchantLevel);
    }

    public static void handle(final SMerchantInfoPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryMerchantInfoPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processPaintingMerchantInfo(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryMerchantInfoPacket[merchantId=" + this.merchantId + ",merchantLevel=" + this.merchantLevel + "]";
    }
}