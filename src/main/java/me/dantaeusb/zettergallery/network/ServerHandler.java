package me.dantaeusb.zettergallery.network;

import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.packet.CGalleryAuthorizationCheckPacket;
import me.dantaeusb.zettergallery.network.packet.CGalleryProceedOfferPacket;
import me.dantaeusb.zettergallery.network.packet.CGallerySelectOfferPacket;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ServerHandler {
    /**
     * This means that client says that player might be authorized the server,
     * so the new check must be done. This will call check, and move menu
     * to according state after that, either repeating check or continuing to offers
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processGalleryAuthenticationRequest(final CGalleryAuthorizationCheckPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof PaintingMerchantMenu) {
            PaintingMerchantMenu menu = (PaintingMerchantMenu) sendingPlayer.containerMenu;

            menu.handleServerAuthenticationRetry();
        }
    }

    public static void processGallerySelectOffer(final CGallerySelectOfferPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof PaintingMerchantMenu) {
            PaintingMerchantMenu paintingMerchantMenu = (PaintingMerchantMenu)sendingPlayer.containerMenu;
            paintingMerchantMenu.updateCurrentOfferIndex(packetIn.getOfferIndex());

            if (paintingMerchantMenu.getCurrentOffer() == null) {
                throw new IllegalStateException("Selected offer is empty");
            }

            if (paintingMerchantMenu.getCurrentOffer().getPaintingData().isEmpty()) {
                throw new IllegalStateException("Painting doesn't have data to register impression");
            }

            UUID paintingUuid = ((GalleryPaintingData) paintingMerchantMenu.getCurrentOffer().getPaintingData().get()).getUUID();

            ConnectionManager.getInstance().registerImpression(sendingPlayer, paintingUuid, null, null);
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
