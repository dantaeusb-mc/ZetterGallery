package me.dantaeusb.zettergallery.network;

import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.http.GalleryConnection;
import me.dantaeusb.zettergallery.network.packet.CGalleryAuthorizationCheckPacket;
import me.dantaeusb.zettergallery.network.packet.CGalleryOffersRequestPacket;
import me.dantaeusb.zettergallery.network.packet.CGallerySelectOfferPacket;
import net.minecraft.server.level.ServerPlayer;

public class ServerHandler {
    public static void processGalleryAuthenticationRequest(final CGalleryAuthorizationCheckPacket packetIn, ServerPlayer sendingPlayer) {
        GalleryConnection.getInstance().checkPlayerToken((ServerPlayer) sendingPlayer);
    }

    public static void processGalleryOffersRequest(final CGalleryOffersRequestPacket packetIn, ServerPlayer sendingPlayer) {
        GalleryConnection.getInstance().getPlayerFeed((ServerPlayer) sendingPlayer);
    }

    public static void processGallerySelectOffer(final CGallerySelectOfferPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof PaintingMerchantMenu) {
            PaintingMerchantMenu paintingMerchantContainer = (PaintingMerchantMenu)sendingPlayer.containerMenu;
            paintingMerchantContainer.updateCurrentOfferIndex(packetIn.getOfferIndex());
        }
    }
}
