package me.dantaeusb.zettergallery.network;

import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.packet.SGalleryAuthorizationRequestPacket;
import me.dantaeusb.zettergallery.network.packet.SGalleryAuthorizationResponsePacket;
import me.dantaeusb.zettergallery.network.packet.SGalleryErrorPacket;
import me.dantaeusb.zettergallery.network.packet.SGallerySalesPacket;
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
