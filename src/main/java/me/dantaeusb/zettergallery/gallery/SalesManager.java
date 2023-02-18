package me.dantaeusb.zettergallery.gallery;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.container.PaintingMerchantContainer;
import me.dantaeusb.zettergallery.gallery.salesmanager.PlayerFeed;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.trading.PaintingMerchantPurchaseOffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This singleton is responsible for caching Painting Feeds, updating
 * and triggering client painting feed refresh for players
 * who are in process of trade with Painting Merchant
 */
public class SalesManager {
    private static @Nullable SalesManager instance;

    /**
     * Player feed cache
     */
    private final HashMap<Integer, PaintingsResponse.CycleInfo> cycles = new HashMap<>();
    private final HashMap<UUID, PlayerFeed> playerFeeds = new HashMap<>();

    private final List<ServerPlayerEntity> trackingPlayers = new ArrayList<>();

    private Integer currentCycleIncrementId = null;

    private int tick = 0;

    private SalesManager() {
        instance = this;
    }

    public static SalesManager getInstance() {
        if (SalesManager.instance == null) {
            throw new IllegalStateException("Painting Feed Manager is not ready, no client app capability in the world");
        }

        return instance;
    }

    public static void initialize() {
        SalesManager.instance = new SalesManager();
    }

    public static void close() {
        SalesManager.instance = null;
    }

    public void registerTrackingPlayer(ServerPlayerEntity player) {
        if (this.trackingPlayers.contains(player)) {
            return;
        }

        this.trackingPlayers.add(player);
    }

    public void unregisterTrackingPlayer(ServerPlayerEntity player) {
        if (!this.trackingPlayers.contains(player)) {
            return;
        }

        this.trackingPlayers.remove(player);
    }

    public @Nullable PaintingsResponse.CycleInfo getCycleInfo(int cycleIncrementId) {
        if (!this.cycles.containsKey(cycleIncrementId)) {
            return null;
        }

        return this.cycles.get(cycleIncrementId);
    }

    public @Nullable PaintingsResponse.CycleInfo getCurrentCycleInfo() {
        if (this.currentCycleIncrementId == null) {
            return null;
        }

        if (!this.cycles.containsKey(this.currentCycleIncrementId)) {
            return null;
        }

        PaintingsResponse.CycleInfo currentCycle = this.cycles.get(this.currentCycleIncrementId);

        if (new Date().getTime() > currentCycle.endsAt.getTime()) {
            return null;
        }

        return currentCycle;
    }

    /**
     * Check that current feed is not outdated, and update if it is
     */
    public void tick() {
        // Quite heavy operation, do every 1.5s
        if (++this.tick % 30 != 0) {
            return;
        }

        if (this.trackingPlayers.isEmpty()) {
            return;
        }

        Iterator<ServerPlayerEntity> trackingPlayerIterator = this.trackingPlayers.iterator();
        do {
            ServerPlayerEntity trackingPlayer = trackingPlayerIterator.next();

            if (!(trackingPlayer.containerMenu instanceof PaintingMerchantMenu)) {
                ZetterGallery.LOG.warn("Player " + trackingPlayer.getName().getString() + " does not have Painting Merchant Menu opened, but not unregistered!");

                trackingPlayerIterator.remove();
                continue;
            }

            PaintingMerchantMenu paintingMerchantMenu = (PaintingMerchantMenu) trackingPlayer.containerMenu;

            if (paintingMerchantMenu.getContainer().canUpdate()) {
                this.acquireMerchantOffers(
                    trackingPlayer,
                    ((PaintingMerchantMenu) trackingPlayer.containerMenu).getContainer(),
                    (cycle, feed) -> {
                        // Typically we don't force update, but if time has come
                        if (paintingMerchantMenu.getContainer().needUpdate()) {
                            paintingMerchantMenu.getContainer().handleFeed(cycle, feed);
                        }
                    },
                    (error) -> ((PaintingMerchantMenu) trackingPlayer.containerMenu).getContainer().handleError(error)
                );
            }
        } while (trackingPlayerIterator.hasNext());

        // Cleanup every 5 minutes
        if (this.tick % 5 * 60 * 20 == 0) {
            this.cleanup();
        }
    }

    /**
     * Remove old cycles created more than 30 minutes ago and remove feed information
     */
    private void cleanup() {
        this.cycles.entrySet().removeIf((cycleInfoEntry ->
            (new Date().getTime() - cycleInfoEntry.getValue().endsAt.getTime()) > 30 * 60 * 1000
        ));

        this.playerFeeds.entrySet().removeIf(playerFeedEntry ->
            !this.cycles.containsKey(playerFeedEntry.getValue().getCycleIncrementId())
        );
    }

    /*
     * Feed
     */

    public boolean canPlayerSell(PlayerEntity player) {
        // We do not care here if the feed is still relevant
        if (this.playerFeeds.containsKey(player.getUUID())) {
            PlayerFeed feed = this.playerFeeds.get(player.getUUID());
            return feed.isSaleAllowed();
        }

        return false;
    }

    /**
     * Returns player's feed if it is loaded and is still active
     * Does NOT request player feed
     * @param player
     * @return
     */
    public @Nullable PlayerFeed getPlayerFeed(PlayerEntity player) {
        if (this.playerFeeds.containsKey(player.getUUID())) {
            PlayerFeed feed = this.playerFeeds.get(player.getUUID());

            if (!this.cycles.containsKey(feed.getCycleIncrementId())) {
                return null;
            }

            if (this.cycles.get(feed.getCycleIncrementId()).endsAt.getTime() < new Date().getTime()) {
                return null;
            }

            return feed;
        }

        return null;
    }

    /**
     * UUIDs of players for who the update is requested already, and we should
     * not do a new request unless last finished
     */
    private final List<UUID> playersWaitingUpdateUuid = new ArrayList<>();

    /**
     * We call it acquire because we're not just checking availability,
     * but using callbacks to asynchronously get offers from service
     * and return later if they're not available on hand
     *
     * @param player
     * @param paintingMerchantContainer
     * @param successConsumer
     * @param errorConsumer
     */
    public void acquireMerchantOffers(
        ServerPlayerEntity player, PaintingMerchantContainer paintingMerchantContainer,
        BiConsumer<PaintingsResponse.CycleInfo, List<PaintingMerchantPurchaseOffer>> successConsumer,
        Consumer<GalleryError> errorConsumer
    ) {
        PlayerFeed feed = this.getPlayerFeed(player);

        if (feed != null) {
            PaintingsResponse.CycleInfo cycle = this.cycles.get(feed.getCycleIncrementId());

            List<PaintingMerchantPurchaseOffer> offers = this.getOffersFromFeed(
                cycle.seed,
                feed,
                paintingMerchantContainer.getMenu().getMerchantId(),
                paintingMerchantContainer.getMenu().getMerchantLevel()
            );

            successConsumer.accept(cycle, offers);
        } else {
            if (this.playersWaitingUpdateUuid.contains(player.getUUID())) {
                if (ZetterGallery.DEBUG_MODE) {
                    ZetterGallery.LOG.warn("Discarding player request for the new feed as it's already queried");
                }

                return;
            }

            // Will call handlePlayerFeed on response, which call handleOffers
            ConnectionManager.getInstance().requestFeed(
                player,
                paintingsResponse -> {
                    this.playersWaitingUpdateUuid.remove(player.getUUID());

                    PaintingsResponse.CycleInfo cycleInfo = this.processCurrentCycleInfo(paintingsResponse.cycleInfo);
                    PlayerFeed playerFeed = this.createPlayerFeed(player, paintingsResponse);

                    List<PaintingMerchantPurchaseOffer> offers = this.getOffersFromFeed(
                        cycleInfo.seed,
                        playerFeed,
                        paintingMerchantContainer.getMenu().getMerchantId(),
                        paintingMerchantContainer.getMenu().getMerchantLevel()
                    );

                    successConsumer.accept(cycleInfo, offers);
                } ,
                error -> {
                    this.playersWaitingUpdateUuid.remove(player.getUUID());

                    errorConsumer.accept(error);
                }
            );

            this.playersWaitingUpdateUuid.add(player.getUUID());
        }
    }

    private PaintingsResponse.CycleInfo processCurrentCycleInfo(PaintingsResponse.CycleInfo cycleInfo) {
        if (this.cycles.containsKey(cycleInfo.incrementId)) {
            if (!this.cycles.get(cycleInfo.incrementId).seed.equals(cycleInfo.seed)) {
                throw new IllegalStateException("Got a new cycle with the same id as the current cycle, but data differs.");
            }

            return cycleInfo;
        }

        long currentEpochTime = new Date().getTime();
        if (cycleInfo.endsAt.getTime() < currentEpochTime || cycleInfo.startsAt.getTime() > currentEpochTime) {
            throw new IllegalStateException("Got a new cycle, but the system time is out of cycle bounds! Please check system time.");
        }

        this.cycles.put(cycleInfo.incrementId, cycleInfo);
        this.currentCycleIncrementId = cycleInfo.incrementId;

        return cycleInfo;
    }

    private PlayerFeed createPlayerFeed(ServerPlayerEntity player, PaintingsResponse response) {
        PlayerFeed feed = PlayerFeed.createFeedFromSaleResponse(player, response);
        this.playerFeeds.put(player.getUUID(), feed);

        return feed;
    }

    /**
     * Depending on merchant ID and level pick some paintings from feed to show on sale
     *
     * @param feed
     * @param merchantId
     * @param merchantLevel
     * @return
     */
    private List<PaintingMerchantPurchaseOffer> getOffersFromFeed(String seed, PlayerFeed feed, UUID merchantId, int merchantLevel) {
        ByteBuffer buffer = ByteBuffer.wrap(seed.getBytes(StandardCharsets.UTF_8), 0, 8);
        long seedLong = buffer.getLong();

        Random rng = new Random(seedLong ^ feed.getPlayer().getUUID().getMostSignificantBits() ^ merchantId.getMostSignificantBits());

        final int totalCount = feed.getOffersCount();

        int showCount = 5 + merchantLevel * 2;
        showCount = Math.min(showCount, totalCount);

        List<Integer> available = IntStream.range(0, totalCount).boxed().collect(Collectors.toList());
        Collections.shuffle(available, rng);
        available = available.subList(0, showCount);

        List<PaintingMerchantPurchaseOffer> randomOffers = available.stream().map(feed.getOffers()::get).collect(Collectors.toList());

        // Remove duplicates from offers list if there are same paintings in different feeds
        List<String> offerIds = new LinkedList<>();
        List<PaintingMerchantPurchaseOffer> offers = new LinkedList<>();

        for (PaintingMerchantPurchaseOffer offer : randomOffers) {
            if (offerIds.contains(offer.getDummyCanvasCode())) {
                continue;
            }

            offerIds.add(offer.getDummyCanvasCode());
            offers.add(offer);
        }

        return offers;
    }
}
