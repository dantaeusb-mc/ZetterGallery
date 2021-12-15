package com.dantaeusb.zettergallery.network.http;

import com.dantaeusb.zettergallery.ZetterGallery;
import com.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import com.dantaeusb.zettergallery.gallery.SalesManager;
import com.dantaeusb.zettergallery.network.http.stub.AuthCheckResponse;
import com.dantaeusb.zettergallery.network.http.stub.AuthTokenResponse;
import com.dantaeusb.zettergallery.network.http.stub.GenericMessageResponse;
import com.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import com.dantaeusb.zettergallery.network.packet.SGalleryAuthorizationResponsePacket;
import com.dantaeusb.zettergallery.network.packet.SGalleryAuthorizationRequestPacket;
import com.dantaeusb.zettergallery.network.packet.SGalleryErrorPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class ServerHttpHandler {
    public static void processPlayerToken(ServerPlayer playerEntity, AuthTokenResponse response) {
        try {
            GalleryConnection.getInstance().savePlayerToken(playerEntity, response.token);

            SGalleryAuthorizationRequestPacket authenticationPacket = new SGalleryAuthorizationRequestPacket(response.crossAuthorizationCode.code);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> playerEntity), authenticationPacket);
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
            ZetterGallery.LOG.trace(exception);
        }
    }

    public static void processPlayerTokenCheck(ServerPlayer playerEntity, AuthCheckResponse response) {
        try {
            SGalleryAuthorizationResponsePacket authenticationPacket = new SGalleryAuthorizationResponsePacket(response.playerRights.canBuy, response.playerRights.canSell);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> playerEntity), authenticationPacket);
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
     * @param playerEntity
     * @param error
     */
    public static void processPlayerTokenCheckFail(ServerPlayer playerEntity, String error) {
        try {
            GalleryConnection.getInstance().removePlayerToken(playerEntity);
            GalleryConnection.getInstance().authorizeServerPlayer(playerEntity);
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }

    public static void processPlayerTokenDrop(ServerPlayer playerEntity, GenericMessageResponse response) {
        GalleryConnection.getInstance().removePlayerToken(playerEntity);
    }

    public static void processPlayerFeed(ServerPlayer playerEntity, PaintingsResponse response) {
        try {
            SalesManager.getInstance().handlePlayerFeed(playerEntity, response);
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }

    public static void processRequestConnectionError(ServerPlayer playerEntity, String message) {
        try {
            SGalleryErrorPacket authenticationPacket = new SGalleryErrorPacket(message);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> playerEntity), authenticationPacket);

            ZetterGallery.LOG.info("Authentication error");
        } catch (Exception exception) {
            ZetterGallery.LOG.error(exception.getMessage());
        }
    }
}
