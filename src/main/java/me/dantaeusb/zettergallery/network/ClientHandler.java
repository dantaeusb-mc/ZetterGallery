package me.dantaeusb.zettergallery.network;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ClientHandler {
    public static void processGalleryPlayerAuthorization(final SGalleryAuthorizationResponsePacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).handleServerAuthenticationSuccess(packetIn.canBuy(), packetIn.canSell());
        }
    }

    /**
     * Process information about player's right and adjust container
     * (i.e. forbid painting selling)
     *
     * @param packetIn
     * @param world
     */
    public static void processGalleryPlayerAuthorizationRequest(final SGalleryAuthorizationRequestPacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).handleServerAuthenticationFail(packetIn.getCrossAuthorizationCode());
        }
    }

    public static void processPaintingMerchantInfo(final SGalleryMerchantInfoPacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).setMerchantId(packetIn.getMerchantId());
            ((PaintingMerchantMenu) player.containerMenu).setMerchantLevel(packetIn.getMerchantLevel());
        }
    }

    public static void processPaintingMerchantOffers(final SGallerySalesPacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).handleOffers(packetIn.isSellAllowed(), packetIn.getOffers());
        }
    }

    public static void processPaintingMerchantError(final SGalleryErrorPacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).handleError(packetIn.getMessage());
        }
    }

    public static void processPaintingOfferState(final SGalleryOfferStatePacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).handleOfferState(packetIn.getCanvasCode(), packetIn.getState(), packetIn.getMessage());
        }
    }
}
