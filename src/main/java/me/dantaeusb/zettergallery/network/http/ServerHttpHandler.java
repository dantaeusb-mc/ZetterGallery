package me.dantaeusb.zettergallery.network.http;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.gallery.SalesManager;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.http.stub.AuthCheckResponse;
import me.dantaeusb.zettergallery.network.http.stub.AuthTokenResponse;
import me.dantaeusb.zettergallery.network.http.stub.GenericMessageResponse;
import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.network.packet.SGalleryAuthorizationResponsePacket;
import me.dantaeusb.zettergallery.network.packet.SGalleryAuthorizationRequestPacket;
import me.dantaeusb.zettergallery.network.packet.SGalleryErrorPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class ServerHttpHandler {
    public static void processPlayerToken(ServerPlayer player, AuthTokenResponse response) {
        try {
            GalleryConnection.getInstance().savePlayerToken(player, response.token);

            SGalleryAuthorizationRequestPacket authenticationPacket = new SGalleryAuthorizationRequestPacket(response.crossAuthorizationCode.code);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> player), authenticationPacket);
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
            ZetterGallery.LOG.trace(exception);
        }
    }

    public static void processPlayerTokenCheck(ServerPlayer player, AuthCheckResponse response) {
        try {
            SGalleryAuthorizationResponsePacket authenticationPacket = new SGalleryAuthorizationResponsePacket(response.playerRights.canBuy, response.playerRights.canSell);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> player), authenticationPacket);
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }

    /**
     * If we failed with current token, let's get another one
     * and try to authorize again. This likely mean that token
     * no longer exists or expired, so we don't need to ask
     * Gallery to drop the token.
     *
     * @param player
     * @param error
     */
    public static void processPlayerTokenCheckFail(ServerPlayer player, String error) {
        try {
            GalleryConnection.getInstance().removePlayerToken(player);
            GalleryConnection.getInstance().authorizeServerPlayer(player);
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }

    public static void processPlayerTokenDrop(ServerPlayer player, GenericMessageResponse response) {
        GalleryConnection.getInstance().removePlayerToken(player);
    }

    public static void processPlayerFeed(ServerPlayer player, PaintingsResponse response) {
        try {
            SalesManager.getInstance().handlePlayerFeed(player, response);
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }

    public static void processPurchaseResult(ServerPlayer player, PaintingsResponse response) {
        try {
            if (player.containerMenu instanceof PaintingMerchantMenu) {
                PaintingMerchantMenu paintingMerchantMenu = (PaintingMerchantMenu)player.containerMenu;
                paintingMerchantMenu.finalizeCheckout();
            }
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }

    public static void processSaleResult(ServerPlayer player, PaintingsResponse response) {
        try {
            if (player.containerMenu instanceof PaintingMerchantMenu) {
                PaintingMerchantMenu paintingMerchantMenu = (PaintingMerchantMenu)player.containerMenu;
                paintingMerchantMenu.finalizeCheckout();
            }
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }

    public static void processRequestConnectionError(ServerPlayer player, String message) {
        try {
            SGalleryErrorPacket authenticationPacket = new SGalleryErrorPacket(message);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> player), authenticationPacket);

            ZetterGallery.LOG.info("Authentication error");
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }
}
