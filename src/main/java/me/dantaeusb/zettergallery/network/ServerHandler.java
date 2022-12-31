package me.dantaeusb.zettergallery.network;

import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.packet.CAuthorizationCheckPacket;
import me.dantaeusb.zettergallery.network.packet.CFeedRefreshRequest;
import me.dantaeusb.zettergallery.network.packet.CSelectOfferPacket;
import net.minecraft.server.level.ServerPlayer;

public class ServerHandler {
    /**
     * This means that client says that player might be authorized the server,
     * so the new check must be done. This will call check, and move menu
     * to according state after that, either repeating check or continuing to offers
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processGalleryAuthenticationRequest(final CAuthorizationCheckPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof PaintingMerchantMenu) {
            PaintingMerchantMenu menu = (PaintingMerchantMenu) sendingPlayer.containerMenu;

            menu.getAuthController().handleAuthorizationRetry();
        }
    }

    public static void processGallerySelectOffer(final CSelectOfferPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof PaintingMerchantMenu) {
            PaintingMerchantMenu paintingMerchantMenu = (PaintingMerchantMenu)sendingPlayer.containerMenu;
            paintingMerchantMenu.updateCurrentOfferIndex(packetIn.offerIndex);
        }
    }

    /**
     * Trigger updating of player's offers in painting merchant menu
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processGalleryFeedRefreshRequest(final CFeedRefreshRequest packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof PaintingMerchantMenu) {
            PaintingMerchantMenu menu = (PaintingMerchantMenu) sendingPlayer.containerMenu;

            menu.getContainer().requestFeed();
        }
    }
}
