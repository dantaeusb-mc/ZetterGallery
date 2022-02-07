package me.dantaeusb.zettergallery.gallery;

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
    private PlayerTokenStorage playerTokenStorage = PlayerTokenStorage.getInstance();

    private ConnectionStatus status = ConnectionStatus.WAITING;
    private String errorMessage = "Connection is not ready";

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

    public void handleServerStart(MinecraftServer server) {
        this.serverInfo = ServerInfo.createMultiplayerServer("%servername%", server.getMotd());
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
    }

    /*
     * Player
     */

    /**
     * Player feed cache
     */
    private final HashMap<UUID, PlayerFeed> playerFeeds = new HashMap<>();

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
                    (exception) -> {
                        error.accept(exception);
                    }
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

        ConnectionManager.getInstance().getConnection().validate(
                this.playerTokenStorage.getPlayerTokenString(player),
                offer.getPaintingData(),
                (response) -> success.accept(),
                (exception) -> error.accept(exception.getMessage())
        );
    }

    public void registerSale(ServerPlayer player, GalleryPaintingData paintingData, EventConsumer success, Consumer<String> error) {
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
            List<PaintingMerchantOffer> offers = this.getOffersFromFeed(feed, paintingMerchantMenu.getMerchantId(), paintingMerchantMenu.getMerchantLevel());

            success.accept(offers);
        } else {
            // Will call handlePlayerFeed on response, which call handleOffers
            this.getConnection().getPlayerFeed(
                    this.playerTokenStorage.getPlayerTokenString(player),
                    (response) -> {
                        PlayerFeed feed = this.createPlayerFeed(player, response);
                        List<PaintingMerchantOffer> offers = this.getOffersFromFeed(feed, paintingMerchantMenu.getMerchantId(), paintingMerchantMenu.getMerchantLevel());

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

    /**
     * Server
     */

    public void registerServer() {
        if (this.status != ConnectionStatus.READY) {
            this.requestServerToken(
                    (response) -> {
                    },
                    (exception) -> {
                    }
            );
        }
    }

    public void handleServerShutdown() {
        if (this.status == ConnectionStatus.READY) {

        }
    }

    private void requestCrossAuthCode(ServerPlayer player, Consumer<PlayerToken> tokenConsumer, Consumer<String> errorCallback) {
        if (this.status == ConnectionStatus.READY) {
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

        errorCallback.accept(this.errorMessage);
    }

    private void requestServerToken(Consumer<Token> success, Consumer<Exception> error) {
        if (this.status == ConnectionStatus.ERROR) {
            if (error != null) {
                error.accept(new RuntimeException(this.errorMessage));
            }

            return;
        }

        this.connection.registerServer(
                this.serverInfo,
                (response) -> {
                    Token serverToken = new Token(response.token.token, response.token.issued, response.token.notAfter);
                    this.serverToken = serverToken;
                    this.status = ConnectionStatus.READY;

                    if (success != null) {
                        success.accept(serverToken);
                    }
                },
                (exception) -> {
                    this.errorMessage = exception.getMessage();
                    this.status = ConnectionStatus.ERROR;

                    error.accept(exception);
                }
        );
    }
}
