package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.AuthorizationCode;
import me.dantaeusb.zettergallery.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Send info to client that player is not authorized
 * but can authorize using cross-auth code
 */
public class SAuthorizationCodeResponsePacket {
    public final AuthorizationCode authorizationCode;

    public SAuthorizationCodeResponsePacket(AuthorizationCode authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SAuthorizationCodeResponsePacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String code = networkBuffer.readUtf(32);
            Date issued = new Date(networkBuffer.readLong());
            Date notAfter = new Date(networkBuffer.readLong());

            AuthorizationCode crossAuthCode = new AuthorizationCode(code, issued, notAfter);

            return new SAuthorizationCodeResponsePacket(crossAuthCode);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGalleryAuthorizationResponsePacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.authorizationCode.code, 32);
        networkBuffer.writeLong(this.authorizationCode.issuedAt.getTime());
        networkBuffer.writeLong(this.authorizationCode.notAfter.getTime());
    }

    public static void handle(final SAuthorizationCodeResponsePacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryAuthorizationCodeResponsePacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processGalleryPlayerNotAuthorized(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryAuthorizationCodeResponsePacket[code=" + this.authorizationCode.code + "]";
    }
}