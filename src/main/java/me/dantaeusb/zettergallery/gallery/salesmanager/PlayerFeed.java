package me.dantaeusb.zettergallery.gallery.salesmanager;

import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.trading.PaintingMerchantPurchaseOffer;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.List;
import java.util.Vector;

public class PlayerFeed {
    private final int cycleIncrementId;
    private final ServerPlayerEntity player;
    private final List<PaintingMerchantPurchaseOffer> offers;
    private final boolean saleAllowed;

    public PlayerFeed(ServerPlayerEntity player, int cycleIncrementId, boolean saleAllowed, List<PaintingMerchantPurchaseOffer> offers) {
        this.player = player;
        this.cycleIncrementId = cycleIncrementId;
        this.saleAllowed = saleAllowed;
        this.offers = offers;
    }

    public static PlayerFeed createFeedFromSaleResponse(ServerPlayerEntity player, PaintingsResponse response) {
        final List<PaintingMerchantPurchaseOffer> offers = new Vector<>();

        for (String feedName : response.feeds.keySet()) {
            for (PaintingsResponse.PaintingItem item : response.feeds.get(feedName)) {
                PaintingMerchantPurchaseOffer offer = PaintingMerchantPurchaseOffer.createOfferFromGalleryResponse(item);
                offer.setCycleInfo(response.cycleInfo.incrementId, feedName);

                offers.add(offer);
            }
        }

        return new PlayerFeed(player, response.cycleInfo.incrementId, true, offers);
    }

    public boolean isSaleAllowed() {
        return this.saleAllowed;
    }

    public List<PaintingMerchantPurchaseOffer> getOffers() {
        return this.offers;
    }

    public int getOffersCount() {
        return this.offers.size();
    }

    public ServerPlayerEntity getPlayer() {
        return this.player;
    }

    public int getCycleIncrementId() {
        return this.cycleIncrementId;
    }
}
