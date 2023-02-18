package me.dantaeusb.zettergallery.core;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.packet.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT;
import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_SERVER;

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
    public static final byte GALLERY_REFRESH_OFFERS = 56;
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

        simpleChannel.registerMessage(GALLERY_AUTHORIZATION_CHECK, CAuthorizationCheckPacket.class,
                CAuthorizationCheckPacket::writePacketData, CAuthorizationCheckPacket::readPacketData,
                CAuthorizationCheckPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(GALLERY_UNAUTHORIZED_RESPONSE, SAuthorizationCodeResponsePacket.class,
                SAuthorizationCodeResponsePacket::writePacketData, SAuthorizationCodeResponsePacket::readPacketData,
                SAuthorizationCodeResponsePacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_AUTHORIZED_RESPONSE, SAuthenticationPlayerResponsePacket.class,
                SAuthenticationPlayerResponsePacket::writePacketData, SAuthenticationPlayerResponsePacket::readPacketData,
                SAuthenticationPlayerResponsePacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_OFFERS_RESPONSE, SOffersPacket.class,
                SOffersPacket::writePacketData, SOffersPacket::readPacketData,
                SOffersPacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_SELECT_OFFER, CSelectOfferPacket.class,
                CSelectOfferPacket::writePacketData, CSelectOfferPacket::readPacketData,
                CSelectOfferPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(GALLERY_REFRESH_OFFERS, CFeedRefreshRequest.class,
            CFeedRefreshRequest::writePacketData, CFeedRefreshRequest::readPacketData,
            CFeedRefreshRequest::handle,
            Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(GALLERY_MERCHANT_INFO, SMerchantInfoPacket.class,
                SMerchantInfoPacket::writePacketData, SMerchantInfoPacket::readPacketData,
                SMerchantInfoPacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_OFFERS_ERROR, SOffersErrorPacket.class,
                SOffersErrorPacket::writePacketData, SOffersErrorPacket::readPacketData,
                SOffersErrorPacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_AUTH_ERROR, SAuthErrorPacket.class,
                SAuthErrorPacket::writePacketData, SAuthErrorPacket::readPacketData,
                SAuthErrorPacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(GALLERY_OFFER_STATE, SOfferStatePacket.class,
                SOfferStatePacket::writePacketData, SOfferStatePacket::readPacketData,
                SOfferStatePacket::handle,
                Optional.of(PLAY_TO_CLIENT));
    }

    public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
        return ZetterGalleryNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }

    public static boolean isThisProtocolAcceptedByServer(String protocolVersion) {
        return ZetterGalleryNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }
}
