package me.dantaeusb.zettergallery.network.http;

import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import com.google.gson.Gson;
import com.mojang.realmsclient.client.RealmsClientConfig;
import me.dantaeusb.zettergallery.network.http.stub.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @todo: make singleton
 */
public class GalleryConnection {
    private static final String API_VERSION = "v1";
    private static final String BASE_URI = "http://127.0.0.1/";
    private static final String SERVERS_ENDPOINT = "servers";
    private static final String TOKEN_ENDPOINT = "auth/token";
    private static final String CHECK_ENDPOINT = "auth/token/check";
    private static final String DROP_ENDPOINT = "auth/drop";
    private static final String PAINTINGS_ENDPOINT = "paintings";
    private static final String PAINTINGS_PURCHASES_ENDPOINT = "sales";
    private static final String PAINTINGS_IMPRESSION_ENDPOINT = "impressions";
    private static final String PAINTINGS_FEED_ENDPOINT = "paintings/feed";

    private static final Gson GSON = new Gson();

    private static GalleryConnection instance;

    private final PlayerTokenStorage tokenStorage = PlayerTokenStorage.getInstance();

    private final ExecutorService poolExecutor;

    private GalleryConnection() {
        this.poolExecutor = Executors.newSingleThreadExecutor();
    }

    public static GalleryConnection getInstance() {
        if (GalleryConnection.instance == null) {
            GalleryConnection.instance = new GalleryConnection();
        }

        return instance;
    }

    public void revalidateCookieStorage() {
        //this.cookieStorage
    }

    public void savePlayerToken(ServerPlayer player, String token) {
        this.tokenStorage.setPlayerToken(player, token);
    }

    public void removePlayerToken(ServerPlayer player) {
        this.tokenStorage.removePlayerToken(player);
    }

    private PlayerTokenStorage getTokenStorage() {
        return this.tokenStorage;
    }

    public void registerServer() {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                URL authUri = GalleryConnection.getUri(SERVERS_ENDPOINT);

                ServerResponse response = makeRequest(authUri, "POST", ServerResponse.class, (String) null, null);

                executor.submitAsync(() -> ServerHttpHandler.processRegistration(response));
            } catch (GalleryException e) {
                // We don't expect errors here so we're treating it as connection issue
                ZetterGallery.LOG.error("Unable to request token for player, Gallery returned error: " + e.getMessage());

                executor.submitAsync(() -> ServerHttpHandler.processRegistrationFailed(e.getMessage()));
            } catch (IOException e) {
                ZetterGallery.LOG.error("Unable to request token for player: " + e.getMessage());

                // @todo: translate
                executor.submitAsync(() -> ServerHttpHandler.processRegistrationFailed("Connection error"));;
            }
        });
    }

    public void unregisterServer() {

    }

    /**
     * Start server player authentication flow. If we have a token
     * already, just check if it works and what rights we have.
     * If we don't, request token and cross-authorization code,
     * which later will be sent to client. When client returns
     * to game after authorizing server, another call for this method
     * will be received and we'll be checking token access.
     *
     * @param playerEntity
     */
    public void authenticateServerPlayer(ServerPlayer playerEntity) {
        if (this.getTokenStorage().hasPlayerToken(playerEntity)) {
            this.checkPlayerToken(playerEntity);
        } else {
            this.requestPlayerToken(playerEntity);
        }
    }

    /**
     * Requests a new token for player with additional
     * cross-auth code that can be used by player to
     * authorize server to perform tasks on player's
     * behalf
     *
     * @param playerEntity
     */
    public void requestPlayerToken(ServerPlayer playerEntity) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final HashMap<String, String> query = new HashMap<>();
                query.put("crossAuthorizationRole", TokenRequest.CrossAuthorizationRole.PLAYER_SERVER.toString());

                URL authUri = GalleryConnection.getUri(TOKEN_ENDPOINT, query);
                // new TokenRequest(TokenRequest.CrossAuthorizationRole.PLAYER_SERVER) was used here, but we just add params to URL
                AuthTokenResponse response = makeRequest(authUri, "GET", AuthTokenResponse.class, playerEntity, null);

                executor.submitAsync(() -> ServerHttpHandler.processPlayerToken(playerEntity, response));
            } catch (GalleryException e) {
                // We don't expect errors here so we're treating it as connection issue
                ZetterGallery.LOG.error("Unable to request token for player, Gallery returned error: " + e.getMessage());

                executor.submitAsync(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, e.getMessage()));
            } catch (IOException e) {
                ZetterGallery.LOG.error("Unable to request token for player: " + e.getMessage());

                // @todo: translate
                executor.submitAsync(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, "Connection error"));
            }
        });
    }

    /**
     * Check that token from player is still valid
     * and check player's privileges
     * (are they banned from submitting lewd pics)
     *
     * @param playerEntity
     */
    public void checkPlayerToken(ServerPlayer playerEntity) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final URL checkUri = GalleryConnection.getUri(CHECK_ENDPOINT);
                AuthCheckResponse response = makeRequest(checkUri, "GET", AuthCheckResponse.class, playerEntity, null);

                executor.submitAsync(() -> ServerHttpHandler.processPlayerTokenCheck(playerEntity, response));
            } catch (GalleryException e) {
                executor.submitAsync(() -> ServerHttpHandler.processPlayerTokenCheckFail(playerEntity, e.getMessage()));
            } catch (IOException e) {
                ZetterGallery.LOG.error("Unable to authenticate player: " + e.getMessage());

                // @todo: translate
                executor.submitAsync(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, "Connection error"));
            }
        });
    }

    /**
     * Deactivates player's token. Usually
     * happens on player log-out event.
     *
     * @param playerEntity
     */
    public void dropPlayerToken(ServerPlayer playerEntity) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final URL checkUri = GalleryConnection.getUri(DROP_ENDPOINT);
                GenericMessageResponse response = makeRequest(checkUri, "GET", GenericMessageResponse.class, playerEntity, null);

                executor.submitAsync(() -> ServerHttpHandler.processPlayerTokenDrop(playerEntity, response));
            } catch (GalleryException e) {
                // We don't expect errors here so we're treating it as connection issue
                ZetterGallery.LOG.error("Unable to drop token for player, Gallery returned error: " + e.getMessage());

                executor.submitAsync(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, e.getMessage()));
            } catch (IOException e) {
                ZetterGallery.LOG.error("Unable to drop token for player: " + e.getMessage());

                // @todo: translate
                executor.submitAsync(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, "Connection error"));
            }
        });
    }

    /**
     * Retrieves player's personal feed on Zetter Gallery
     *
     * @param playerEntity
     */
    public void getPlayerFeed(ServerPlayer playerEntity) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final URL saleUri = GalleryConnection.getUri(PAINTINGS_FEED_ENDPOINT);
                PaintingsResponse response = makeRequest(saleUri, "GET", PaintingsResponse.class, playerEntity, null);

                executor.submitAsync(() -> ServerHttpHandler.processPlayerFeed(playerEntity, response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error("Unable to authenticate player: " + e.getMessage());

                executor.execute(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, "Connection error"));
            } catch (IOException e) {
                ZetterGallery.LOG.error("Unable to authenticate player: " + e.getMessage());

                executor.execute(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, "Connection error"));
            }
        });
    }

    /**
     * Register that player sees a painting
     *
     * @param playerEntity
     * @param paintingData
     */
    public void registerImpression(ServerPlayer playerEntity, UUID paintingUuid) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final URL saleUri = GalleryConnection.getUri(PAINTINGS_ENDPOINT + "/" + paintingUuid.toString() + "/" + PAINTINGS_IMPRESSION_ENDPOINT);
                GenericMessageResponse response = makeRequest(saleUri, "POST", GenericMessageResponse.class, playerEntity, null);

                executor.submitAsync(() -> ServerHttpHandler.processImpressionResult(playerEntity, response));
            } catch (GalleryException|IOException e) {
                ZetterGallery.LOG.error("Unable to register painting impression: " + e.getMessage());
            }
        });
    }

    /**
     * Purchases painting on behalf of player
     *
     * @param playerEntity player who is purchasing painting
     * @param paintingUuid uuid of purchased painting
     * @param price price in emeralds, 1-10
     */
    public void purchase(ServerPlayer playerEntity, UUID paintingUuid, int price) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final PurchaseRequest request = new PurchaseRequest(price);

                final URL saleUri = GalleryConnection.getUri(PAINTINGS_ENDPOINT + "/" + paintingUuid.toString() + "/" + PAINTINGS_PURCHASES_ENDPOINT);
                GenericMessageResponse response = makeRequest(saleUri, "POST", GenericMessageResponse.class, playerEntity, request);

                executor.submitAsync(() -> ServerHttpHandler.processPurchaseResult(playerEntity, response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error("Unable to purchase painting: " + e.getMessage());

                // @todo: handle failed token check
                executor.execute(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, "Connection error"));
            } catch (IOException e) {
                ZetterGallery.LOG.error("Unable to purchase painting: " + e.getMessage());

                executor.execute(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, "Connection error"));
            }
        });
    }

    /**
     * Check if painting can be submitted by player
     *
     * @param playerEntity
     * @param paintingData
     */
    public void validate(ServerPlayer playerEntity, PaintingData paintingData) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final SaleRequest request = new SaleRequest(paintingData);

                final HashMap<String, String> query = new HashMap<>();
                query.put("save", "false");
                final URL validateUri = GalleryConnection.getUri(PAINTINGS_ENDPOINT, query);

                PaintingsResponse response = makeRequest(validateUri, "POST", PaintingsResponse.class, playerEntity, request);

                executor.submitAsync(() -> ServerHttpHandler.processSaleResult(playerEntity, response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error("Unable to validate painting: " + e.getMessage());

                // @todo: handle failed token check
                executor.execute(() -> ServerHttpHandler.processSaleValidationResult(playerEntity, e));
            } catch (IOException e) {
                ZetterGallery.LOG.error("Unable to validate painting: " + e.getMessage());

                // @todo: translate
                executor.execute(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, "Connection error"));
            }
        });
    }

    /**
     * Submits painting on behalf of player
     *
     * @param playerEntity
     * @param paintingData
     */
    public void sell(ServerPlayer playerEntity, PaintingData paintingData) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final SaleRequest request = new SaleRequest(paintingData);

                final HashMap<String, String> query = new HashMap<>();
                query.put("save", "false");
                final URL saleUri = GalleryConnection.getUri(PAINTINGS_ENDPOINT, query);
                PaintingsResponse response = makeRequest(saleUri, "POST", PaintingsResponse.class, playerEntity, request);

                executor.submitAsync(() -> ServerHttpHandler.processSaleResult(playerEntity, response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error("Unable to sell painting: " + e.getMessage());

                // @todo: handle failed token check
            } catch (IOException e) {
                ZetterGallery.LOG.error("Unable to sell painting: " + e.getMessage());

                // @todo: translate
                executor.execute(() -> ServerHttpHandler.processRequestConnectionError(playerEntity, "Connection error"));
            }
        });
    }

    protected static URL getUri(String endpoint) throws MalformedURLException {
        return GalleryConnection.getUri(endpoint, null);
    }

    protected static URL getUri(String endpoint, @Nullable Map<String, String> queryMap) throws MalformedURLException {
        String query = "";

        if (queryMap != null && !queryMap.isEmpty()) {
            List<String> queryElements = new Vector<>();
            queryMap.forEach((String key, String value) -> {
                queryElements.add(key + "=" + value);
            });

            query = "?" + String.join("&", queryElements);
        }


        return new URL(BASE_URI + API_VERSION + "/" + endpoint + query);
    }

    protected static <T> T makeRequest(URL uri, String method, Class<T> classOfT, ServerPlayer player, @Nullable Object input) throws IOException, GalleryException {
        final String token = GalleryConnection.getInstance().getTokenStorage().getPlayerToken(player);
        return makeRequest(uri, method, classOfT, token, input);
    }

    protected static <T> T makeRequest(URL uri, String method, Class<T> classOfT, @Nullable String token, @Nullable Object input) throws IOException, GalleryException {
        Proxy proxy = RealmsClientConfig.getProxy();
        final HttpURLConnection connection;

        if (proxy != null) {
            connection = (HttpURLConnection) uri.openConnection(proxy);
        } else {
            connection = (HttpURLConnection) uri.openConnection();
        }

        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }

        connection.setRequestMethod(method);
        GalleryConnection.prepareRequest(connection);

        if (method.equals("POST") && input != null) {
            GalleryConnection.writeRequest(connection, input);
        } else {
            connection.connect();
        }

        StringBuilder responseBody = new StringBuilder();

        if (!(connection.getResponseCode() >= 200 && connection.getResponseCode() < 300)) {
            Scanner scanner = new Scanner(connection.getErrorStream());

            while (scanner.hasNext()) {
                responseBody.append(scanner.nextLine());
            }

            GenericMessageResponse errorResponse = GSON.fromJson(responseBody.toString(), GenericMessageResponse.class);

            scanner.close();
            connection.disconnect();

            throw new GalleryException(errorResponse.message);
        } else {
            Scanner scanner = new Scanner(connection.getInputStream());

            while (scanner.hasNext()) {
                responseBody.append(scanner.nextLine());
            }

            scanner.close();
            connection.disconnect();
        }

        return GSON.fromJson(responseBody.toString(), classOfT);
    }

    private static void prepareRequest(HttpURLConnection connection) {
        connection.setConnectTimeout(60 * 1000);
        connection.setReadTimeout(60 * 1000);
        connection.setRequestProperty("Accept", "application/json");
    }

    private static void writeRequest(HttpURLConnection connection, Object input) throws IOException {
        String jsonInput = GSON.toJson(input);

        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Content-Length", String.valueOf(jsonInput.length()));

        OutputStream stream = connection.getOutputStream();
        stream.write(jsonInput.getBytes(StandardCharsets.UTF_8));
        stream.close();
    }
}
