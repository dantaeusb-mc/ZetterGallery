package me.dantaeusb.zettergallery.gallery;

import me.dantaeusb.zettergallery.container.PaintingMerchantContainer;
import me.dantaeusb.zettergallery.gallery.salesmanager.PlayerFeed;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.http.GalleryConnection;
import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SalesManager {
    private static SalesManager instance;

    /**
     * Player -> Entity -> Storage Tree that handles storages
     */
    private final HashMap<UUID, PlayerFeed> playerFeeds = new HashMap<>();

    /**
     * List of painting codes that were sent for validation before
     * sale is allowed
     */
    private final List<String> validationRequestedCodes = new LinkedList<>();

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

    public void validateSale(ServerPlayer player, PaintingMerchantOffer offer) {
        PlayerFeed playerFeed = this.playerFeeds.get(player.getUUID());

        if (playerFeed == null) {
            return;
        }

        if (!playerFeed.isSaleAllowed()) {
            return;
        }

        GalleryConnection.getInstance().validate(player, offer.getPaintingData());
    }

    public void requestOffers(ServerPlayer player, PaintingMerchantMenu paintingMerchantMenu) {
        if (this.playerFeeds.containsKey(player.getUUID())) {
            PaintingMerchantContainer container = paintingMerchantMenu.getContainer();

            PlayerFeed feed = this.playerFeeds.get(player.getUUID());
            List<PaintingMerchantOffer> offers = this.getOffersFromFeed(feed, paintingMerchantMenu.getMerchantId(), paintingMerchantMenu.getMerchantLevel());

            container.handleOffers(offers);
        } else {
            // Will call handlePlayerFeed on response, which call handleOffers
            GalleryConnection.getInstance().getPlayerFeed(player);
        }
    }

    public void handlePlayerFeed(ServerPlayer player, PaintingsResponse response) {
        PlayerFeed feed = PlayerFeed.createFeedFromSaleResponse(player, response);

        this.playerFeeds.put(player.getUUID(), feed);

        if (player.containerMenu instanceof PaintingMerchantMenu) {
            PaintingMerchantMenu paintingMerchantMenu = (PaintingMerchantMenu)player.containerMenu;
            PaintingMerchantContainer container = paintingMerchantMenu.getContainer();

            List<PaintingMerchantOffer> offers = this.getOffersFromFeed(feed, paintingMerchantMenu.getMerchantId(), paintingMerchantMenu.getMerchantLevel());
            container.handleOffers(offers);
        }
    }

    /**
     * Depending on merchant ID and level pick some paintings from feed to show on sale
     * @param feed
     * @param merchantId
     * @param merchantLevel
     * @return
     */
    private List<PaintingMerchantOffer> getOffersFromFeed(PlayerFeed feed, UUID merchantId, int merchantLevel) {
        Random rng = new Random(feed.getPlayer().getUUID().getMostSignificantBits() ^ merchantId.getMostSignificantBits());

        final int totalCount = feed.getOffersCount();

        int showCount = 5 + merchantLevel * 2;
        showCount = Math.min(showCount, totalCount);

        List<Integer> available = IntStream.range(0, totalCount).boxed().collect(Collectors.toList());
        Collections.shuffle(available, rng);
        available = available.subList(0, showCount);

        return available.stream().map(feed.getOffers()::get).collect(Collectors.toList());
    }
}
