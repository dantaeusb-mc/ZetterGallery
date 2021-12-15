package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SGalleryAuthorizationResponsePacket {
    private final boolean canBuy;
    private final boolean canSell;

    public SGalleryAuthorizationResponsePacket(boolean canBuy, boolean canSell) {
        this.canBuy = canBuy;
        this.canSell = canSell;
    }

    public boolean canBuy() {
        return this.canBuy;
    }

    public boolean canSell() {
        return this.canSell;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SGalleryAuthorizationResponsePacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            boolean canBuy = networkBuffer.readBoolean();
            boolean canSell = networkBuffer.readBoolean();

            return new SGalleryAuthorizationResponsePacket(canBuy, canSell);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGalleryAuthorizationResponsePacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeBoolean(this.canBuy);
        networkBuffer.writeBoolean(this.canSell);
    }

    public static void handle(final SGalleryAuthorizationResponsePacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryAuthorizationResponsePacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processGalleryPlayerAuthorization(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryAuthorizationResponsePacket[canBuy=" + this.canBuy + ",canSell=" + this.canSell + "]";
    }
}