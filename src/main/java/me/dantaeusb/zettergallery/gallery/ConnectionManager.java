package me.dantaeusb.zettergallery.gallery;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.Helper;
import me.dantaeusb.zettergallery.network.http.GalleryConnection;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.network.http.stub.AuthTokenResponse;
import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.trading.PaintingMerchantSaleOffer;
import me.dantaeusb.zettergallery.util.EventConsumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * This singleton is responsible for Gallery interaction
 * on behalf of a server.
 * <p>
 * Deferred server registration
 */
public class ConnectionManager {
    private static @Nullable ConnectionManager instance;

    private final GalleryConnection connection;
    private final PlayerTokenStorage playerTokenStorage = PlayerTokenStorage.getInstance();

    private Level overworld;
    private ServerInfo serverInfo;

    private Token serverToken;
    private Token refreshToken;
    private UUID serverUuid;

    private ConnectionStatus status = ConnectionStatus.WAITING;
    private String errorMessage = "Connection is not ready";

    private long errorTimestamp;

    private ConnectionManager(Level overworld) {
        this.overworld = overworld;
        this.connection = new GalleryConnection();

        instance = this;
    }

    public static ConnectionManager getInstance() {
        if (ConnectionManager.instance == null) {
            throw new IllegalStateException("Connection Manager is not ready, no client app capability in the world");
        }

        return instance;
    }

    public static void initialize(Level overworld) {
        ConnectionManager.instance = new ConnectionManager(overworld);
    }

    public static void close() {
        ConnectionManager.instance = null;
    }

    public void handleServerStart(MinecraftServer server) {
        if (server.isDedicatedServer()) {
            this.serverInfo = ServerInfo.createMultiplayerServer(server.getLocalIp() + ":" + server.getPort(), server.getMotd(), server.getServerVersion());
        } else {
            this.serverInfo = ServerInfo.createSingleplayerServer(server.getServerVersion());
        }
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
        UNRECOVERABLE_ERROR,
    }

    /*
     * Player
     */

    /**
     * Register server player entity which is used for anonymous usage by default and
     * also get auth code for particular player for this server to log in later if needed
     *
     * @param player
     * @param tokenConsumer
     * @param errorConsumer
     */
    private void registerServerPlayer(ServerPlayer player, Consumer<PlayerToken> tokenConsumer, Consumer<GalleryError> errorConsumer) {
        GalleryServerCapability galleryServerCapability = (GalleryServerCapability) Helper.getWorldGalleryCapability(this.overworld);

        this.connection.registerPlayer(
            this.serverToken.token,
            player,
            (response) -> {
                PlayerToken playerReservedToken = new PlayerToken(
                    response.token.token,
                    response.token.issuedAt,
                    response.token.notAfter
                );

                if (response.poolingAuthorizationCode != null) {
                    playerReservedToken.setAuthorizationCode(response.poolingAuthorizationCode);
                }

                this.playerTokenStorage.setPlayerToken(player, playerReservedToken);

                tokenConsumer.accept(playerReservedToken);
            },
            errorConsumer
        );
    }

    /**
     * As authorization codes are very short-lived, we need to periodically update
     * the authorization code so player will be able to log in anytime
     *
     * @param player
     * @param authorizationCodeConsumer
     * @param errorConsumer
     */
    private void requestAuthorizationCode(ServerPlayer player, Consumer<AuthorizationCode> authorizationCodeConsumer, Consumer<GalleryError> errorConsumer) {
        this.connection.requestServerPlayerAuthorizationCode(
            this.serverToken.token,
            (authorizationCode) -> {
                if (this.playerTokenStorage.getPlayerToken(player) == null) {
                    ZetterGallery.LOG.error("No token to update authorization code");
                }

                this.playerTokenStorage.getPlayerToken(player).setAuthorizationCode(authorizationCode);

                authorizationCodeConsumer.accept(authorizationCode);
            },
            errorConsumer
        );
    }

    /**
     * Start server player authorization flow. If we have a token
     * already, just check if it works and what rights we have.
     * If we don't have a token, request token and cross-authorization code,
     * which later will be sent to client. When client returns
     * to game after authorizing server, another call for this method
     * will be received and we'll be checking token access.
     *
     * @param player
     */
    public void authenticateServerPlayer(ServerPlayer player, Consumer<PlayerToken> successConsumer, Consumer<GalleryError> errorConsumer) {
        // Check that server is registered, try again when registered or handle error if cannot be registered
        if (!this.authenticateServerClient(
            (token) -> {
                this.authenticateServerPlayer(player, successConsumer, errorConsumer);
            },
            errorConsumer
        )) {
            return;
        }

        if (this.playerTokenStorage.hasPlayerToken(player)) {
            PlayerToken playerToken = this.playerTokenStorage.getPlayerToken(player);
            assert playerToken != null;

            // @todo: [MID] Check if playerToken expired
            if (!playerToken.valid()) {
                this.playerTokenStorage.removePlayerToken(player);

                this.registerServerPlayer(
                    player,
                    successConsumer,
                    errorConsumer
                );

                return;
            }

            if (playerToken.isAuthorized()) {
                successConsumer.accept(playerToken);
                // @todo: [HIGH] Check it's not expired!
            } else if (playerToken.authorizationCode != null) {
                GalleryServerCapability galleryServerCapability = (GalleryServerCapability) Helper.getWorldGalleryCapability(this.overworld);

                this.connection.requestServerPlayerToken(
                    galleryServerCapability.getClientInfo(),
                    playerToken.token,
                    playerToken.authorizationCode.code,
                    authTokenResponse -> {
                        this.playerTokenStorage.removePlayerToken(player);

                        PlayerToken newPlayerToken = new PlayerToken(
                            authTokenResponse.token,
                            authTokenResponse.issuedAt,
                            authTokenResponse.notAfter
                        );

                        // @todo: [HIGH] This is wrong, ask Gallery
                        newPlayerToken.setAuthorizedAs(new PlayerToken.PlayerInfo(
                            player.getUUID(),
                            player.getName().getString()
                        ));

                        this.playerTokenStorage.setPlayerToken(player, newPlayerToken);

                        successConsumer.accept(newPlayerToken);
                    },
                    error -> {
                        // @todo: [MED] Check that it works!
                        if (error.getCode() == 401) {
                            // Code is fine, but it's not authorized yet
                            successConsumer.accept(playerToken);
                        } else {
                            // Code is likely broken, make a new one!
                            ZetterGallery.LOG.error(error.getMessage());
                            playerToken.dropAuthorizationCode();

                            this.requestAuthorizationCode(
                                player,
                                (authorizationCode) -> {
                                    this.authenticateServerPlayer(player, successConsumer, errorConsumer);
                                },
                                errorConsumer
                            );
                        }
                    }
                );
            } else {
                errorConsumer.accept(new GalleryError(GalleryError.UNKNOWN, "No authorization code"));
            }
        } else {
            this.registerServerPlayer(
                player,
                successConsumer,
                errorConsumer
            );
        }
    }

    /*
     * Paintings
     */

    public void registerImpression(ServerPlayer player, UUID paintingUuid, EventConsumer successConsumer, EventConsumer errorConsumer) {
        ConnectionManager.getInstance().getConnection().registerImpression(
            this.playerTokenStorage.getPlayerTokenString(player),
            paintingUuid,
            (response) -> {
                successConsumer.accept();
            },
            (exception) -> {
                // throw away error data, we cannot do anything about unregestired impression
                errorConsumer.accept();
            }
        );
    }

    public void registerPurchase(ServerPlayer player, UUID paintingUuid, int price, EventConsumer successConsumer, Consumer<GalleryError> errorConsumer) {
        ConnectionManager.getInstance().getConnection().registerPurchase(
            this.playerTokenStorage.getPlayerTokenString(player),
            paintingUuid,
            price,
            (response) -> {
                successConsumer.accept();
            },
            errorConsumer
        );
    }

    public void validateSale(ServerPlayer player, PaintingMerchantSaleOffer offer, EventConsumer successConsumer, Consumer<GalleryError> errorConsumer) {
        if (!SalesManager.getInstance().canPlayerSell(player)) {
            errorConsumer.accept(new GalleryError(GalleryError.SERVER_SALE_DISALLOWED, "Sale is not allowed on this server"));
            return;
        }

        if (offer.isLoading()) {
            errorConsumer.accept(new GalleryError(GalleryError.SERVER_RECEIVED_INVALID_PAINTING_DATA, "Painting data not ready"));
            return;
        }

        ConnectionManager.getInstance().getConnection().validatePainting(
            this.playerTokenStorage.getPlayerTokenString(player),
            offer.getPaintingName(),
            offer.getDummyPaintingData(),
            (response) -> successConsumer.accept(),
            errorConsumer
        );
    }

    public void registerSale(ServerPlayer player, PaintingMerchantSaleOffer offer, EventConsumer successConsumer, Consumer<GalleryError> errorConsumer) {
        if (!SalesManager.getInstance().canPlayerSell(player)) {
            errorConsumer.accept(new GalleryError(GalleryError.SERVER_SALE_DISALLOWED, "Sale is not allowed on this server"));
            return;
        }

        if (offer.isLoading()) {
            errorConsumer.accept(new GalleryError(GalleryError.SERVER_RECEIVED_INVALID_PAINTING_DATA, "Painting data not ready"));
            return;
        }

        ConnectionManager.getInstance().getConnection().sellPainting(
            this.playerTokenStorage.getPlayerTokenString(player),
            offer.getPaintingName(),
            offer.getDummyPaintingData(),
            (response) -> {
                successConsumer.accept();
            },
            errorConsumer
        );
    }

    /*
     * Feed
     */

    public void requestFeed(
        ServerPlayer player,
        Consumer<PaintingsResponse> successConsumer,
        Consumer<GalleryError> errorConsumer
    ) {
        this.getConnection().getPlayerFeed(
            this.playerTokenStorage.getPlayerTokenString(player),
            successConsumer,
            errorConsumer
        );
    }

    /*
     * Server
     */

    /**
     * Check that sever is registered and have a valid token,
     * return true if registered and ready and call success consumer,
     * return false is not and call retry or error consumer dependent
     * on the recoverability of issue.
     *
     * @todo: [MID] Test
     *
     * @param retryConsumer
     * @param errorConsumer
     * @return
     */
    private boolean authenticateServerClient(Consumer<Token> retryConsumer, @Nullable Consumer<GalleryError> errorConsumer) {
        if (this.status == ConnectionStatus.READY && this.serverToken.valid()) {
            if (this.serverToken.needRefresh()) {
                GalleryServerCapability galleryServerCapability = (GalleryServerCapability) Helper.getWorldGalleryCapability(this.overworld);

                if (galleryServerCapability.getClientInfo() != null) {
                    this.refreshServerToken(
                        this.refreshToken,
                        retryConsumer,
                        errorConsumer
                    );
                }

                return false;
            }

            return true;
        }

        // @todo: [HIGH] Refresh server tokens

        if (this.status == ConnectionStatus.UNRECOVERABLE_ERROR) {
            if (errorConsumer != null) {
                errorConsumer.accept(new GalleryError(GalleryError.SERVER_INVALID_VERSION, this.errorMessage));
            }

            return false;
        }

        // 30 seconds since error not yet passed, otherwise proceed
        if (this.status == ConnectionStatus.ERROR && System.currentTimeMillis() <= this.errorTimestamp + 30 * 1000) {
            if (errorConsumer != null) {
                errorConsumer.accept(new GalleryError(GalleryError.SERVER_INVALID_VERSION, this.errorMessage));
            }

            return false;
        }

        GalleryServerCapability galleryServerCapability = (GalleryServerCapability) Helper.getWorldGalleryCapability(this.overworld);
        if (galleryServerCapability.getClientInfo() != null) {
            this.getServerToken(
                galleryServerCapability.getClientInfo(),
                retryConsumer,
                errorConsumer
            );
        } else {
            this.createServerClient(
                () -> {
                    this.getServerToken(
                        galleryServerCapability.getClientInfo(),
                        retryConsumer,
                        errorConsumer
                    );
                },
                errorConsumer
            );
        }

        return false;
    }

    /**
     * Creates new client with extra properties and saves client id
     * and client secret to capability for later use
     *
     * Now this server will be connected to that record
     * in Zetter Gallery, and will be able to use credentials
     * to get access token
     */
    private void createServerClient(EventConsumer successConsumer, @Nullable Consumer<GalleryError> errorConsumer) {
        GalleryServerCapability galleryServerCapability = (GalleryServerCapability) Helper.getWorldGalleryCapability(this.overworld);

        this.connection.createServerClient(
            this.serverInfo,
            serverResponse -> {
                if (serverResponse.client == null) {
                    if (errorConsumer != null) {
                        errorConsumer.accept(new GalleryError(GalleryError.UNKNOWN, "Cannot find necessary client info in response"));
                    }

                    return;
                }

                galleryServerCapability.saveClientInfo(serverResponse.client);

                successConsumer.accept();
            },
            error -> {
                // Invalid version is unrecoverable
                if (error.getCode() == 403) {
                    this.errorMessage = "Server's Zetter Gallery version is out of date. Please update.";
                    this.status = ConnectionStatus.UNRECOVERABLE_ERROR;
                } else {
                    this.errorMessage = "Cannot connect. Please try later.";
                    this.status = ConnectionStatus.ERROR;
                }

                this.errorTimestamp = System.currentTimeMillis();
                error.setClientMessage(this.errorMessage);

                if (errorConsumer != null) {
                    errorConsumer.accept(error);
                }
            }
        );
    }

    /**
     * Calls for new token given in exchange for client id and client secret
     *
     * @param clientInfo
     * @param retryConsumer
     * @param errorConsumer
     */
    private void getServerToken(GalleryServerCapability.ClientInfo clientInfo, Consumer<Token> retryConsumer, @Nullable Consumer<GalleryError> errorConsumer) {
        this.connection.requestToken(
            clientInfo,
            authTokenResponse -> {
                this.handleServerAuthorizationSuccess(authTokenResponse);

                retryConsumer.accept(this.serverToken);
            },
            error -> {
                // Cannot use client_credentials
                this.handleServerAuthorizationIssue(error);

                if (errorConsumer != null) {
                    errorConsumer.accept(error);
                }
            }
        );
    }

    /**
     * Calls for new token given in exchange for client id and client secret
     *
     * @param refreshToken
     * @param retryConsumer
     * @param errorConsumer
     */
    private void refreshServerToken(Token refreshToken, Consumer<Token> retryConsumer, @Nullable Consumer<GalleryError> errorConsumer) {
        this.connection.refreshToken(
            refreshToken,
            authTokenResponse -> {
                this.handleServerAuthorizationSuccess(authTokenResponse);

                retryConsumer.accept(this.serverToken);
            },
            error -> {
                // Cannot use refresh_token
                this.handleServerAuthorizationIssue(error);

                if (errorConsumer != null) {
                    errorConsumer.accept(error);
                }
            }
        );
    }

    /**
     * Save server client token when successfully authorized
     * with client id or refresh token
     * @param authTokenResponse
     */
    private void handleServerAuthorizationSuccess(AuthTokenResponse authTokenResponse) {
        this.serverToken = new Token(
            authTokenResponse.token,
            authTokenResponse.issuedAt,
            authTokenResponse.notAfter
        );

        if (authTokenResponse.refreshToken != null) {
            this.refreshToken = new Token(
                authTokenResponse.refreshToken.token,
                authTokenResponse.refreshToken.issuedAt,
                authTokenResponse.refreshToken.notAfter
            );
        }

        this.status = ConnectionStatus.READY;
    }

    /**
     * In case if server cannot authorize with request error, wipe all existing info
     * and try to do everything from start. If that's internal error - just ignore it
     */
    private void handleServerAuthorizationIssue(GalleryError error) {
        // Cannot use existing credentials
        if (error.getCode() == 400) {
            ZetterGallery.LOG.error("Unable to use existing refresh token got error: " + error.getMessage());

            if (this.serverToken != null) {
                //@todo: [MID] this.connection.dropServerToken

                this.serverToken = null;
                this.refreshToken = null;
            }

            GalleryServerCapability galleryServerCapability = (GalleryServerCapability) Helper.getWorldGalleryCapability(this.overworld);
            galleryServerCapability.removeClientInfo();
        }

        this.errorMessage = "Cannot connect. Please try later.";
        this.status = ConnectionStatus.ERROR;
        this.errorTimestamp = System.currentTimeMillis();

        error.setClientMessage(this.errorMessage);
    }

    /**
     * Discard current token
     */
    private void dropServerToken() {
        this.playerTokenStorage.flush();

        this.connection.unregisterServer(
            this.serverToken.token,
            (message) -> {
            },
            Zetter.LOG::error
        );
    }
}
