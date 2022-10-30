package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.PlayerToken;
import me.dantaeusb.zettergallery.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Send info to client that player is not authorized
 * but can authorize using cross-auth code
 */
public class SGalleryAuthenticationCodeResponsePacket {
    public final PlayerToken.CrossAuthCode crossAuthCode;

    public SGalleryAuthenticationCodeResponsePacket(PlayerToken.CrossAuthCode crossAuthCode) {
        this.crossAuthCode = crossAuthCode;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SGalleryAuthenticationCodeResponsePacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String code = networkBuffer.readUtf(16);
            Date issued = new Date(networkBuffer.readLong());
            Date notAfter = new Date(networkBuffer.readLong());

            PlayerToken.CrossAuthCode crossAuthCode = new PlayerToken.CrossAuthCode(code, issued, notAfter);

            return new SGalleryAuthenticationCodeResponsePacket(crossAuthCode);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGalleryAuthorizationResponsePacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.crossAuthCode.code(), 16);
        networkBuffer.writeLong(this.crossAuthCode.issued().getTime());
        networkBuffer.writeLong(this.crossAuthCode.notAfter().getTime());
    }

    public static void handle(final SGalleryAuthenticationCodeResponsePacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryAuthorizationResponsePacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processGalleryPlayerNotAuthorized(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryAuthenticationCodeResponsePacket[code=" + this.crossAuthCode.code() + "]";
    }
}