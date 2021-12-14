package com.dantaeusb.zettergallery.gallery;

import com.dantaeusb.zettergallery.gallery.salesmanager.PlayerFeed;
import com.dantaeusb.zettergallery.network.http.GalleryConnection;
import com.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import com.dantaeusb.zettergallery.tileentity.PaintingMerchantTileEntity;
import com.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @todo: get rid of pair and javafx dependency
 */

public class SalesManager {
    private static SalesManager instance;

    /**
     * Player -> Entity -> Storage Tree that handles storages
     */
    final HashMap<UUID, PlayerFeed> playerFeed = new HashMap<>();

    long timestamp;

    private SalesManager() {
        this.refreshFeed(System.currentTimeMillis());

        instance = this;
    }

    public static SalesManager getInstance() {
        if (SalesManager.instance == null) {
            SalesManager.instance = new SalesManager();
        }

        return instance;
    }

    public void refreshFeed(long timestamp) {
        this.timestamp = timestamp;


    }

    public void requestOffers(ServerPlayer player, UUID merchantId, int merchantLevel, Consumer<List<PaintingMerchantOffer>> offersConsumer) {
        if (this.playerFeed.containsKey(player.getUUID())) {
            PlayerFeed feed = this.playerFeed.get(player.getUUID());

            List<PaintingMerchantOffer> list = this.getOffersFromFeed(feed, merchantId, merchantLevel);
            offersConsumer.accept(list);
        } else {
            this.requestPlayerFeed(player, (feed) -> {
                List<PaintingMerchantOffer> list = this.getOffersFromFeed(feed, merchantId, merchantLevel);
                offersConsumer.accept(list);
            });
        }
    }

    private List<PaintingMerchantOffer> getOffersFromFeed(PlayerFeed feed, UUID merchantId, int merchantLevel) {
        Random rng = new Random(feed.getPlayer().getUUID().getMostSignificantBits() ^ merchantId.getMostSignificantBits());

        final int totalCount = feed.getOffersCount();

        int showCount = 5 + merchantLevel * 2;
        showCount = Math.max(showCount, totalCount);

        List<PaintingMerchantOffer> offers = new Vector<>();

        return rng.ints(showCount, 0, totalCount).mapToObj(offers::get).collect(Collectors.toList());
    }

    public void requestPlayerFeed(ServerPlayer player, Consumer<PlayerFeed> playerOffersConsumer) {
        if (this.playerFeed.containsKey(player.getUUID())) {
            playerOffersConsumer.accept(this.playerFeed.get(player.getUUID()));
        }

        GalleryConnection.getInstance().getPlayerFeed(player);
    }

    public void handlePlayerFeed(ServerPlayer player, PaintingsResponse response) {
        PlayerFeed feed = PlayerFeed.createFeedFromSaleResponse(player, response);


    }
}
