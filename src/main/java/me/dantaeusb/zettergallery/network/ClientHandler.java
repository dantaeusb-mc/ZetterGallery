package me.dantaeusb.zettergallery.network;

import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

public class ClientHandler {
    /**
     * Player is not authorized server to perform actions on their behalf
     * So we're sending them code to do so
     * @param packetIn
     * @param world
     */
    public static void processGalleryPlayerNotAuthorized(final SAuthorizationCodeResponsePacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;
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
    public static void processGalleryPlayerAuthorized(final SAuthenticationPlayerResponsePacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;
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
    public static void processPaintingMerchantInfo(final SMerchantOffersPacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;
        assert player != null;
        AbstractContainerMenu playerContainerMenu = player.containerMenu;

        if (packetIn.containerId == playerContainerMenu.containerId && player.containerMenu instanceof PaintingMerchantMenu paintingMerchantMenu) {
            paintingMerchantMenu.setMerchantId(packetIn.merchantId);
            paintingMerchantMenu.setOffers(packetIn.offers);
            paintingMerchantMenu.setXp(packetIn.villagerXp);
            paintingMerchantMenu.setMerchantLevel(packetIn.villagerLevel);
        }
    }

    /**
     * Save offers from Server to Client's merchant container
     * @param packetIn
     * @param world
     */
    public static void processPaintingMerchantOffers(final SOffersPacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;
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
    public static void processPaintingMerchantOffersError(final SOffersErrorPacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;
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
    public static void processPaintingMerchantAuthError(final SAuthErrorPacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;
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
    public static void processPaintingOfferState(final SOfferStatePacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;
        assert player != null;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).handleOfferState(packetIn.getCanvasCode(), packetIn.getState(), packetIn.getMessage());
        }
    }
}
