package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import static net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT;
import static net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = ZetterGallery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterGalleryNetwork {
    public static SimpleChannel simpleChannel;
    // @todo: rename this on release, it's zetter:zetter_channel 0.1
    public static final ResourceLocation simpleChannelRL = new ResourceLocation(ZetterGallery.MOD_ID, "zetter_channel");
    public static final String MESSAGE_PROTOCOL_VERSION = "0.1";

    public static final byte GALLERY_AUTHORIZATION_REQUEST = 50;
    public static final byte GALLERY_AUTHORIZATION_CHECK = 51;
    public static final byte GALLERY_UNAUTHORIZED_RESPONSE = 52;
    public static final byte GALLERY_AUTHORIZED_RESPONSE = 53;
    public static final byte GALLERY_OFFERS_RESPONSE = 54;
    public static final byte GALLERY_SELECT_OFFER = 55;
    public static final byte GALLERY_UPDATE_OFFER = 56;
    public static final byte GALLERY_PROCEED_OFFER = 57;
    public static final byte GALLERY_MERCHANT_INFO = 58;
    public static final byte GALLERY_OFFERS_ERROR = 60;
    public static final byte GALLERY_OFFER_STATE = 61;
    public static final byte GALLERY_AUTH_ERROR = 62;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onCommonSetupEvent(FMLCommonSetupEvent event) {
        simpleChannel = NetworkRegistry.newSimpleChannel(
                simpleChannelRL,
                () -> MESSAGE_PROTOCOL_VERSION,
                ZetterGalleryNetwork::isThisProtocolAcceptedByClient,
                ZetterGalleryNetwork::isThisProtocolAcceptedByServer
        );

        simpleChannel.registerMessage(GALLERY_AUTHORIZATION_CHECK, CGalleryAuthorizationCheckPacket.class,
                CGalleryAuthorizationCheckPacket::writePacketData, CGalleryAuthorizationCheckPacket::readPacketData,
                CGalleryAuthorizationCheckPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(GALLERY_UNAUTHORIZED_RESPONSE, SGalleryAuthorizationCodeResponsePacket.class,
                SGalleryAuthorizationCodeResponsePacket::writePacketData, SGalleryAuthorizationCodeResponsePacket::readPacketData,
                SGalleryAuthorizationCodeResponsePacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_AUTHORIZED_RESPONSE, SGalleryAuthenticationPlayerResponsePacket.class,
                SGalleryAuthenticationPlayerResponsePacket::writePacketData, SGalleryAuthenticationPlayerResponsePacket::readPacketData,
                SGalleryAuthenticationPlayerResponsePacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_OFFERS_RESPONSE, SGalleryOffersPacket.class,
                SGalleryOffersPacket::writePacketData, SGalleryOffersPacket::readPacketData,
                SGalleryOffersPacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_SELECT_OFFER, CGallerySelectOfferPacket.class,
                CGallerySelectOfferPacket::writePacketData, CGallerySelectOfferPacket::readPacketData,
                CGallerySelectOfferPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(GALLERY_PROCEED_OFFER, CGalleryProceedOfferPacket.class,
                CGalleryProceedOfferPacket::writePacketData, CGalleryProceedOfferPacket::readPacketData,
                CGalleryProceedOfferPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(GALLERY_MERCHANT_INFO, SGalleryMerchantInfoPacket.class,
                SGalleryMerchantInfoPacket::writePacketData, SGalleryMerchantInfoPacket::readPacketData,
                SGalleryMerchantInfoPacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_OFFERS_ERROR, SGalleryOffersErrorPacket.class,
                SGalleryOffersErrorPacket::writePacketData, SGalleryOffersErrorPacket::readPacketData,
                SGalleryOffersErrorPacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_AUTH_ERROR, SGalleryAuthErrorPacket.class,
                SGalleryAuthErrorPacket::writePacketData, SGalleryAuthErrorPacket::readPacketData,
                SGalleryAuthErrorPacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_OFFER_STATE, SGalleryOfferStatePacket.class,
                SGalleryOfferStatePacket::writePacketData, SGalleryOfferStatePacket::readPacketData,
                SGalleryOfferStatePacket::handle,
                Optional.of(PLAY_TO_CLIENT));
    }

    public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
        return ZetterGalleryNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }

    public static boolean isThisProtocolAcceptedByServer(String protocolVersion) {
        return ZetterGalleryNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }
}
