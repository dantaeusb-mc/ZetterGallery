package me.dantaeusb.zettergallery.network;

import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class ClientHandler {
    /**
     * Player is not authorized server to perform actions on their behalf
     * So we're sending them code to do so
     * @param packetIn
     * @param world
     */
    public static void processGalleryPlayerNotAuthorized(final SAuthorizationCodeResponsePacket packetIn, World world) {
        PlayerEntity player = Minecraft.getInstance().player;
        assert player != null;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).getAuthController().handleUnauthorized(packetIn.authorizationCode);
        }
    }

    /**
     * Player authorized server to perform actions on their behalf
     * So we're sending them info about account they've authorized
     * @param packetIn
     * @param world
     */
    public static void processGalleryPlayerAuthorized(final SAuthenticationPlayerResponsePacket packetIn, World world) {
        PlayerEntity player = Minecraft.getInstance().player;
        assert player != null;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).getAuthController().handleAuthorized(packetIn.playerInfo);
        }
    }

    /**
     * Get info about Merchant, because we're using non-default merchant menu
     * @param packetIn
     * @param world
     */
    public static void processPaintingMerchantInfo(final SMerchantInfoPacket packetIn, World world) {
        PlayerEntity player = Minecraft.getInstance().player;
        assert player != null;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).setMerchantId(packetIn.merchantId);
            ((PaintingMerchantMenu) player.containerMenu).setMerchantLevel(packetIn.merchantLevel);
        }
    }

    /**
     * Save offers from Server to Client's merchant container
     * @param packetIn
     * @param world
     */
    public static void processPaintingMerchantOffers(final SOffersPacket packetIn, World world) {
        PlayerEntity player = Minecraft.getInstance().player;
        assert player != null;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).getContainer().handleFeed(
                packetIn.getCycleInfo(), packetIn.getOffers()
            );
        }
    }

    /**
     * Process error that happened at some point of offers flow
     * @param packetIn
     * @param world
     */
    public static void processPaintingMerchantOffersError(final SOffersErrorPacket packetIn, World world) {
        PlayerEntity player = Minecraft.getInstance().player;
        assert player != null;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).getContainer().handleError(packetIn.getError());
        }
    }

    /**
     * Process error that happened at some point of auth flow
     * @param packetIn
     * @param world
     */
    public static void processPaintingMerchantAuthError(final SAuthErrorPacket packetIn, World world) {
        PlayerEntity player = Minecraft.getInstance().player;
        assert player != null;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).getAuthController().handleError(packetIn.getError());
        }
    }

    /**
     * Process data for some offer, typically a sell offer (validation, etc.)
     * @param packetIn
     * @param world
     */
    public static void processPaintingOfferState(final SOfferStatePacket packetIn, World world) {
        PlayerEntity player = Minecraft.getInstance().player;
        assert player != null;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).handleOfferState(packetIn.getCanvasCode(), packetIn.getState(), packetIn.getMessage());
        }
    }
}
