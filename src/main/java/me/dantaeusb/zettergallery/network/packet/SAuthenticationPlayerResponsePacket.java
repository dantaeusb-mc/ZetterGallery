package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.PlayerToken;
import me.dantaeusb.zettergallery.network.ClientHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Send info to player that they are
 * authorizes as a player specified in this packet
 */
public class SAuthenticationPlayerResponsePacket {
    public final PlayerToken.PlayerInfo playerInfo;

    public SAuthenticationPlayerResponsePacket(PlayerToken.PlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SAuthenticationPlayerResponsePacket readPacketData(PacketBuffer networkBuffer) {
        try {
            UUID playerUuid = networkBuffer.readUUID();
            String playerNickname = networkBuffer.readUtf(32767);

            return new SAuthenticationPlayerResponsePacket(new PlayerToken.PlayerInfo(playerUuid, playerNickname));
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGalleryAuthenticationPlayerResponsePacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeUUID(this.playerInfo.uuid());
        networkBuffer.writeUtf(this.playerInfo.nickname(), 32767);
    }

    public static void handle(final SAuthenticationPlayerResponsePacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<World> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryAuthorizationResponsePacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processGalleryPlayerAuthorized(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryAuthenticationPlayerResponsePacket[uuid=" + this.playerInfo.uuid() + ",nickname=" + this.playerInfo.nickname() + "]";
    }
}