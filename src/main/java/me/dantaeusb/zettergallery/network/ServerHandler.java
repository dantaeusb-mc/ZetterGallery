package me.dantaeusb.zettergallery.network;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.SalesManager;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.http.GalleryConnection;
import me.dantaeusb.zettergallery.network.packet.CGalleryAuthorizationCheckPacket;
import me.dantaeusb.zettergallery.network.packet.CGalleryOffersRequestPacket;
import me.dantaeusb.zettergallery.network.packet.CGalleryProceedOfferPacket;
import me.dantaeusb.zettergallery.network.packet.CGallerySelectOfferPacket;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ServerHandler {
    public static void processGalleryAuthenticationRequest(final CGalleryAuthorizationCheckPacket packetIn, ServerPlayer sendingPlayer) {
        GalleryConnection.getInstance().checkPlayerToken((ServerPlayer) sendingPlayer);
    }

    public static void processGalleryOffersRequest(final CGalleryOffersRequestPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof PaintingMerchantMenu) {
            SalesManager.getInstance().requestOffers(sendingPlayer, (PaintingMerchantMenu) sendingPlayer.containerMenu);
        } else {
            // @todo: which player
            ZetterGallery.LOG.error("Player requested offers, but don't have painting merchant container opened");
        }
    }

    public static void processGallerySelectOffer(final CGallerySelectOfferPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof PaintingMerchantMenu) {
            PaintingMerchantMenu paintingMerchantMenu = (PaintingMerchantMenu)sendingPlayer.containerMenu;
            paintingMerchantMenu.updateCurrentOfferIndex(packetIn.getOfferIndex());

            // @todo: Might be different type
            UUID paintingUuid = ((GalleryPaintingData) paintingMerchantMenu.getCurrentOffer().getPaintingData()).getUUID();

            GalleryConnection.getInstance().registerImpression(sendingPlayer, paintingUuid);
        }
    }

    public static void processGalleryProceedOffer(final CGalleryProceedOfferPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof PaintingMerchantMenu) {
            // @todo: here we send request, but not yet proceed on container
            PaintingMerchantMenu paintingMerchantMenu = (PaintingMerchantMenu)sendingPlayer.containerMenu;
            paintingMerchantMenu.startCheckout();
        }
    }
}
