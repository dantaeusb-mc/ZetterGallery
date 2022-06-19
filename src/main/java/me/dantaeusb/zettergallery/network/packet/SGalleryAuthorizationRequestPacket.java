package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.Helper;
import me.dantaeusb.zettergallery.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SGalleryAuthorizationRequestPacket {
    private final String crossAuthorizationCode;

    public SGalleryAuthorizationRequestPacket(String crossAuthorizationCode) {
        this.crossAuthorizationCode = crossAuthorizationCode;
    }

    public String getCrossAuthorizationCode() {
        return this.crossAuthorizationCode;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SGalleryAuthorizationRequestPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String crossAuthorizationCode = networkBuffer.readUtf(Helper.GALLERY_CROSS_AUTH_CODE_LENGTH);

            return new SGalleryAuthorizationRequestPacket(crossAuthorizationCode);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGalleryAuthorizationRequestPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.crossAuthorizationCode, Helper.GALLERY_CROSS_AUTH_CODE_LENGTH);
    }

    public static void handle(final SGalleryAuthorizationRequestPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryTokenPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processGalleryPlayerAuthorizationRequest(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryAuthorizationRequestPacket[crossAuthorizationCode=" + this.crossAuthorizationCode + "]";
    }
}