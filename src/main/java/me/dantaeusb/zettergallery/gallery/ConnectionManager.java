package me.dantaeusb.zettergallery.gallery;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.container.PaintingMerchantContainer;
import me.dantaeusb.zettergallery.gallery.salesmanager.PlayerFeed;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.http.GalleryConnection;
import me.dantaeusb.zettergallery.network.http.GalleryException;
import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import me.dantaeusb.zettergallery.util.EventConsumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This siтgleton is responsible for Gallery interaction
 * on behalf of a server.
 * <p>
 * Deferred server registration
 */
public class ConnectionManager {
    private static ConnectionManager instance;

    private GalleryConnection connection;
    private ServerInfo serverInfo;

    private Token serverToken;
    private UUID serverUuid;
    private PlayerTokenStorage playerTokenStorage = PlayerTokenStorage.getInstance();

    private ConnectionStatus status = ConnectionStatus.WAITING;
    private String errorMessage = "Connection is not ready";

    private long errorTimestamp;

    /**
     * Player feed cache
     */
    private final HashMap<UUID, PlayerFeed> playerFeeds = new HashMap<>();

    private long nextCycleEpoch;
    private String cycleSeed;

    private ConnectionManager() {
        this.connection = new GalleryConnection();
        this.serverInfo = ServerInfo.createSingleplayerServer();

        instance = this;
    }

    public static ConnectionManager getInstance() {
        if (ConnectionManager.instance == null) {
            ConnectionManager.instance = new ConnectionManager();
        }

        return instance;
    }

    /**
     * Check that current feed is not outdated, and update if it is
     * @todo: Update if player has trading screen opened
     */
    public void update() {
        if (this.nextCycleEpoch == 0L) {
            return;
        }

        long unixTime = System.currentTimeMillis();

        if (this.playerFeeds.size() > 0 && unixTime > this.nextCycleEpoch) {
            this.playerFeeds.clear();
        }
    }

    public void handleServerStart(MinecraftServer server) {
        this.serverInfo = ServerInfo.createMultiplayerServer("%servername%", server.getMotd());
    }

    public void handleServerStop(MinecraftServer server) {
        if (this.status == ConnectionStatus.READY) {
            this.dropServerToken();
        }
    }

    public GalleryConnection getConnection() {
        return this.connection;
    }

    private boolean hasToken() {
        return this.serverToken == null || !this.serverToken.valid();
    }

    enum ConnectionStatus {
        WAITING,
        READY,
        ERROR,
        ERROR_RETRY,
    }

    /*
     * Player
     */

    /**
     * Start server player authorization flow. If we have a token
     * already, just check if it works and what rights we have.
     * If we don't, request token and cross-authorization code,
     * which later will be sent to client. When client returns
     * to game after authorizing server, another call for this method
     * will be received and we'll be checking token access.
     *
     * @param player
     */
    public void authorizeServerPlayer(ServerPlayer player, BiConsumer<Boolean, Boolean> success, Consumer<String> retry, Consumer<String> error) {
        // @todo: check status here?
        if (this.status != ConnectionStatus.READY) {
            this.requestServerToken(
                    (token) -> {
                        this.authorizeServerPlayer(player, success, retry, error);
                    },
                    (exception) -> {
                        error.accept(exception.getMessage());
                    }
            );

            return;
        }

        if (this.playerTokenStorage.hasPlayerToken(player) && this.playerTokenStorage.getPlayerToken(player).valid()) {
            // Might be valid but unauthorized

            this.connection.checkPlayerToken(
                    this.playerTokenStorage.getPlayerTokenString(player),
                    (response) -> {
                        success.accept(true, true);
                    },
                    (exception) -> {
                        if (exception instanceof GalleryException) {
                            retry.accept(this.playerTokenStorage.getPlayerToken(player).crossAuthCode.code);
                        } else {
                            error.accept("Unable to authorize");
                        }
                        // @todo: depends on exception
                    }
            );
        } else {
            this.requestCrossAuthCode(
                    player,
                    (response) -> {
                        retry.accept(response.crossAuthCode.code);
                    },
                    error::accept
            );
        }
    }

    /*
     * Paintings
     */

    public void registerImpression(ServerPlayer player, UUID paintingUuid, EventConsumer success, EventConsumer error) {
        ConnectionManager.getInstance().getConnection().registerImpression(
                this.playerTokenStorage.getPlayerTokenString(player),
                paintingUuid,
                (response) -> {
                    success.accept();
                },
                (exception) -> {
                    error.accept();
                }
        );
    }

    public void registerPurchase(ServerPlayer player, UUID paintingUuid, int price, EventConsumer success, Consumer<String> error) {
        ConnectionManager.getInstance().getConnection().purchase(
                this.playerTokenStorage.getPlayerTokenString(player),
                paintingUuid,
                price,
                (response) -> {
                    success.accept();
                },
                (exception) -> {
                    error.accept(exception.getMessage());
                }
        );
    }

    public void validateSale(ServerPlayer player, PaintingMerchantOffer offer, EventConsumer success, Consumer<String> error) {
        PlayerFeed playerFeed = this.playerFeeds.get(player.getUUID());

        if (playerFeed == null) {
            error.accept("Unable to load feed");
            return;
        }

        if (!playerFeed.isSaleAllowed()) {
            error.accept("Sale is not allowed on this server");
            return;
        }

        if (offer.getPaintingData().isEmpty()) {
            error.accept("Painting data not ready");
            return;
        }

        ConnectionManager.getInstance().getConnection().validate(
                this.playerTokenStorage.getPlayerTokenString(player),
                offer.getPaintingData().get(),
                (response) -> success.accept(),
                (exception) -> error.accept(exception.getMessage())
        );
    }

    public void registerSale(ServerPlayer player, PaintingData paintingData, EventConsumer success, Consumer<String> error) {
        ConnectionManager.getInstance().getConnection().sell(
                this.playerTokenStorage.getPlayerTokenString(player),
                paintingData,
                (response) -> {
                    success.accept();
                },
                (exception) -> {
                    error.accept(exception.getMessage());
                }
        );
    }

    /*
     * Feed
     */

    public void requestOffers(ServerPlayer player, PaintingMerchantMenu paintingMerchantMenu, Consumer<List<PaintingMerchantOffer>> success, Consumer<String> error) {
        PaintingMerchantContainer container = paintingMerchantMenu.getContainer();

        if (this.playerFeeds.containsKey(player.getUUID())) {
            PlayerFeed feed = this.playerFeeds.get(player.getUUID());
            List<PaintingMerchantOffer> offers = this.getOffersFromFeed(this.cycleSeed, feed, paintingMerchantMenu.getMerchantId(), paintingMerchantMenu.getMerchantLevel());

            success.accept(offers);
        } else {
            // Will call handlePlayerFeed on response, which call handleOffers
            this.getConnection().getPlayerFeed(
                    this.playerTokenStorage.getPlayerTokenString(player),
                    (response) -> {
                        this.cycleSeed = response.cycleInfo.seed;
                        this.nextCycleEpoch = response.cycleInfo.endsAt.getTime();

                        PlayerFeed feed = this.createPlayerFeed(player, response);
                        List<PaintingMerchantOffer> offers = this.getOffersFromFeed(this.cycleSeed, feed, paintingMerchantMenu.getMerchantId(), paintingMerchantMenu.getMerchantLevel());

                        success.accept(offers);
                    },
                    (exception) -> {
                        error.accept(exception.getMessage());
                    }
            );
        }
    }

    private PlayerFeed createPlayerFeed(ServerPlayer player, PaintingsResponse response) {
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
    private List<PaintingMerchantOffer> getOffersFromFeed(String seed, PlayerFeed feed, UUID merchantId, int merchantLevel) {
        ByteBuffer buffer = ByteBuffer.wrap(seed.getBytes(StandardCharsets.UTF_8), 0, 8);
        long seedLong = buffer.getLong();

        Random rng = new Random(seedLong ^ feed.getPlayer().getUUID().getMostSignificantBits() ^ merchantId.getMostSignificantBits());

        final int totalCount = feed.getOffersCount();

        int showCount = 5 + merchantLevel * 2;
        showCount = Math.min(showCount, totalCount);

        List<Integer> available = IntStream.range(0, totalCount).boxed().collect(Collectors.toList());
        Collections.shuffle(available, rng);
        available = available.subList(0, showCount);

        List<PaintingMerchantOffer> randomOffers = available.stream().map(feed.getOffers()::get).collect(Collectors.toList());

        // Remove duplicates from offers list if there are same paintings in different feeds
        List<String> offerIds = new LinkedList<>();
        List<PaintingMerchantOffer> offers = new LinkedList<>();

        for (PaintingMerchantOffer offer : randomOffers) {
            if (offerIds.contains(offer.getCanvasCode())) {
                continue;
            }

            offerIds.add(offer.getCanvasCode());
            offers.add(offer);
        }

         return offers;
    }

    /*
     * Server
     */

    /**
     * Request cross-auth token for particular player for this server
     * @param player
     * @param tokenConsumer
     * @param errorCallback
     */
    private void requestCrossAuthCode(ServerPlayer player, Consumer<PlayerToken> tokenConsumer, Consumer<String> errorCallback) {
        this.connection.requestPlayerToken(
                this.playerTokenStorage.getPlayerTokenString(player),
                (response) -> {
                    PlayerToken playerReservedToken = new PlayerToken(response.token, response.issued, response.notAfter);

                    if (response.crossAuthorizationCode != null) {
                        playerReservedToken.setCrossAuthCode(response.crossAuthorizationCode.code, response.crossAuthorizationCode.issued, response.crossAuthorizationCode.notAfter);
                    }

                    this.playerTokenStorage.setPlayerToken(player, playerReservedToken);
                    tokenConsumer.accept(playerReservedToken);
                },
                (exception) -> {
                    errorCallback.accept(exception.getMessage());
                }
        );
    }

    /**
     * Request token for current server, register it in Zetter Gallery
     * @param success
     * @param error
     */
    private void requestServerToken(Consumer<Token> success, Consumer<Exception> error) {
        if (this.status == ConnectionStatus.ERROR) {
            if (error != null) {
                error.accept(new RuntimeException(this.errorMessage));
            }

            return;
        }

        if (this.status == ConnectionStatus.ERROR_RETRY) {
            // 30 seconds since error passed
            if (System.currentTimeMillis() > this.errorTimestamp + 30 * 1000) {
                this.status = ConnectionStatus.WAITING;
            } else {
                if (error != null) {
                    error.accept(new RuntimeException(this.errorMessage));
                }

                return;
            }
        }

        this.connection.registerServer(
                this.serverInfo,
                (response) -> {
                    Token serverToken = new Token(response.token.token, response.token.issued, response.token.notAfter);
                    this.serverToken = serverToken;
                    this.serverUuid = response.uuid;
                    this.status = ConnectionStatus.READY;

                    if (success != null) {
                        success.accept(serverToken);
                    }
                },
                (exception) -> {
                    Zetter.LOG.error(exception);

                    // Invalid version is unrecoverable
                    if (exception instanceof GalleryException && ((GalleryException) exception).getCode() == 403) {
                        this.errorMessage = exception.getMessage();
                        this.status = ConnectionStatus.ERROR;

                        error.accept(exception);
                        return;
                    }

                    this.errorMessage = "Cannot connect to Zetter Gallery. Please try again later.";
                    this.status = ConnectionStatus.ERROR_RETRY;
                    this.errorTimestamp = System.currentTimeMillis();

                    error.accept(exception);
                }
        );
    }

    /**
     * Discard current token
     */
    private void dropServerToken() {
        this.playerTokenStorage.flush();

        this.connection.unregisterServer(
                this.serverToken.token,
                this.serverUuid,
                (message) -> {},
                Zetter.LOG::error
        );
    }
}
