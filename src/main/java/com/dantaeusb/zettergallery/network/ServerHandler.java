package com.dantaeusb.zettergallery.network;

import com.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import com.dantaeusb.zettergallery.network.http.GalleryConnection;
import com.dantaeusb.zettergallery.network.packet.*;
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
