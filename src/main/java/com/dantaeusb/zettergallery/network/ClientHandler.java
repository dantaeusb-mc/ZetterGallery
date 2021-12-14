package com.dantaeusb.zettergallery.network;

import com.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import com.dantaeusb.zettergallery.network.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ClientHandler {
    public static void processGalleryPlayerAuthorization(final SGalleryAuthorizationResponsePacket packetIn, Level world) {
        Player player = Minecraft.getInstance().player;

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            ((PaintingMerchantMenu) player.containerMenu).handleAuthorization(packetIn.canBuy(), packetIn.canSell());
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
            ((PaintingMerchantMenu) player.containerMenu).handleAuthorizationRequest(packetIn.getCrossAuthorizationCode());
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
}
