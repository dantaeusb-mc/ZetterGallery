package me.dantaeusb.zettergallery.gallery.salesmanager;

import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Vector;

public class PlayerFeed {
    private final ServerPlayer player;
    private final List<PaintingMerchantOffer> offers;
    private final boolean saleAllowed;

    public PlayerFeed(ServerPlayer player, boolean saleAllowed, List<PaintingMerchantOffer> offers) {
        this.player = player;
        this.saleAllowed = saleAllowed;
        this.offers = offers;
    }

    public static PlayerFeed createFeedFromSaleResponse(ServerPlayer player, PaintingsResponse response) {
        final List<PaintingMerchantOffer> offers = new Vector<>();

        for (String feedName : response.feeds.keySet()) {
            for (PaintingsResponse.PaintingItem item : response.feeds.get(feedName)) {
                PaintingMerchantOffer offer = PaintingMerchantOffer.createOfferFromResponse(item);

                offers.add(offer);
            }
        }

        return new PlayerFeed(player, true, offers);
    }

    public boolean isSaleAllowed() {
        return this.saleAllowed;
    }

    public List<PaintingMerchantOffer> getOffers() {
        return this.offers;
    }

    public int getOffersCount() {
        return this.offers.size();
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }
}
