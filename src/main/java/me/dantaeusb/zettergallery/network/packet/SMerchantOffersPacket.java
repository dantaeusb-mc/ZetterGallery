package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Custom implementation of ClientboundMerchantOffersPacket
 * because it checks for MerchantMenu and we don't want to override it
 *
 * @see net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket
 */
public class SMerchantOffersPacket {
    public final UUID merchantId;
    public final int containerId;
    public final MerchantOffers offers;
    public final int villagerLevel;
    public final int villagerXp;

    public SMerchantOffersPacket(UUID merchantId, int containerId, MerchantOffers offers, int villagerLevel, int villagerXp) {
        this.merchantId = merchantId;
        this.containerId = containerId;
        this.offers = offers;
        this.villagerLevel = villagerLevel;
        this.villagerXp = villagerXp;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SMerchantOffersPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            final UUID merchantId = networkBuffer.readUUID();
            final int containerId = networkBuffer.readVarInt();
            final MerchantOffers offers = MerchantOffers.createFromStream(networkBuffer);
            final int villagerLevel = networkBuffer.readVarInt();
            final int villagerXp = networkBuffer.readVarInt();

            return new SMerchantOffersPacket(merchantId, containerId, offers, villagerLevel, villagerXp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SMerchantOffersPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUUID(this.merchantId);
        networkBuffer.writeVarInt(this.containerId);
        this.offers.writeToStream(networkBuffer);
        networkBuffer.writeVarInt(this.villagerLevel);
        networkBuffer.writeVarInt(this.villagerXp);
    }

    public static void handle(final SMerchantOffersPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SMerchantOffersPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processPaintingMerchantInfo(packetIn, clientWorld.get()));
    }

    @Override
    public String toString() {
        return "SMerchantOffersPacket[merchantId=" + this.merchantId + ",containerId=" + this.containerId + ",villagerLevel=" + this.villagerLevel + "]";
    }
}