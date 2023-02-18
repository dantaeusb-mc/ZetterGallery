package me.dantaeusb.zettergallery.network.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.Helper;
import me.dantaeusb.zettergallery.gallery.AuthorizationCode;
import me.dantaeusb.zettergallery.gallery.GalleryServer;
import me.dantaeusb.zettergallery.gallery.ServerInfo;
import me.dantaeusb.zettergallery.gallery.Token;
import me.dantaeusb.zettergallery.network.http.stub.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class GalleryConnection implements AutoCloseable {
    private static final String API_VERSION = "v1";
    // Game Server is a specific type of oAuth2 client
    private static final String SERVERS_ENDPOINT = "servers";
    private static final String SERVERS_PLAYERS_ENDPOINT = "servers/players";
    private static final String SERVERS_PLAYERS_TOKEN_ENDPOINT = "servers/players/token";
    private static final String SERVERS_PLAYERS_AUTHORIZATION_CODE_ENDPOINT = "servers/players/authorization-code";
    private static final String CLIENTS_ENDPOINT = "clients";
    private static final String TOKEN_ENDPOINT = "auth/token";
    private static final String CHECK_ENDPOINT = "auth/token/check";
    private static final String REVOKE_ENDPOINT = "auth/token/revoke";
    private static final String PAINTINGS_ENDPOINT = "paintings";
    private static final String PAINTINGS_PURCHASES_ENDPOINT = "sales";
    private static final String PAINTINGS_IMPRESSION_ENDPOINT = "impressions";
    private static final String PAINTINGS_FEED_ENDPOINT = "paintings/feed";

    private static final Gson GSON = new Gson();

    private final ExecutorService poolExecutor;

    public GalleryConnection() {
        this.poolExecutor = Executors.newSingleThreadExecutor();
    }

    public void close() {
        this.poolExecutor.shutdownNow();
    }

    public void createServerClient(ServerInfo serverInfo, Consumer<ServerResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final ServerRegisterRequest request = new ServerRegisterRequest(serverInfo.singleplayer, serverInfo.motd, serverInfo.gameVersion, serverInfo.galleryVersion);
                URL authUri = GalleryConnection.getUri(SERVERS_ENDPOINT);

                ServerResponse response = makeRequest(authUri, "POST", ServerResponse.class, null, request);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to register server client app, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to register server client app: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Send request to the token response with client info
     * (ID and secret), expect to receive token in exchange
     * @param clientInfo
     * @param successConsumer
     * @param errorConsumer
     */
    public void requestToken(GalleryServer.ClientInfo clientInfo, Consumer<AuthTokenResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final HashMap<String, String> query = new HashMap<>();
                query.put("grantType", "client_credentials");
                query.put("clientId", clientInfo.id);
                query.put("clientSecret", clientInfo.secret);
                query.put("scope", "server");
                query.put("requestRefresh", "true");
                URL authUri = GalleryConnection.getUri(TOKEN_ENDPOINT, query);

                AuthTokenResponse response = makeRequest(authUri, "GET", AuthTokenResponse.class, null, null);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to exchange token for server, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to exchange token for server: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Send request to the token response with client info
     * (ID and secret), expect to receive token in exchange
     * @param refreshToken
     * @param successConsumer
     * @param errorConsumer
     */
    public void refreshToken(Token refreshToken, Consumer<AuthTokenResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final HashMap<String, String> query = new HashMap<>();
                query.put("grantType", "refresh_token");
                query.put("refreshToken", refreshToken.token);
                query.put("requestRefresh", "true");

                URL authUri = GalleryConnection.getUri(TOKEN_ENDPOINT, query);

                AuthTokenResponse response = makeRequest(authUri, "GET", AuthTokenResponse.class, null, null);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to exchange token for server, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to exchange token for server: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Ask to delete Server Client entity,
     * and revoke related tokens
     * @param serverToken
     * @param successConsumer
     * @param errorConsumer
     */
    public void unregisterServer(String serverToken, Consumer<GenericMessageResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                URL authUri = GalleryConnection.getUri(REVOKE_ENDPOINT);

                GenericMessageResponse response = makeRequest(authUri, "DELETE", GenericMessageResponse.class, serverToken, null);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to drop server token, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to drop server token: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Requests a new token for player with additional
     * cross-auth code that can be used by player to
     * authorize server to perform tasks on player's
     * behalf
     */
    public void registerPlayer(String serverToken, PlayerEntity serverPlayer, Consumer<ServerPlayerResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final ServerPlayerRegisterRequest request = new ServerPlayerRegisterRequest(serverPlayer.getUUID(), serverPlayer.getName().getString());
                URL authUri = GalleryConnection.getUri(SERVERS_PLAYERS_ENDPOINT);

                ServerPlayerResponse response = makeRequest(authUri, "POST", ServerPlayerResponse.class, serverToken, request);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to request player token, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), "Unable to request player token")));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to request player token: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, "Unable to request player token")));
            }
        });
    }

    /**
     * Specific server player token. Allows to connect player, server player and client altogether
     *
     * @todo: [HIGH] Response is different type
     *
     * @param clientInfo
     * @param successConsumer
     * @param errorConsumer
     */
    public void requestServerPlayerToken(GalleryServer.ClientInfo clientInfo, String currentToken, String authorizationCode, Consumer<AuthTokenResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final HashMap<String, String> query = new HashMap<>();
                query.put("grantType", "authorization_code");
                query.put("clientId", clientInfo.id);
                query.put("clientSecret", clientInfo.secret);
                query.put("authorizationCode", authorizationCode);
                query.put("scope", "player_server");
                query.put("requestRefresh", "true");
                URL authUri = GalleryConnection.getUri(SERVERS_PLAYERS_TOKEN_ENDPOINT, query);

                AuthTokenResponse response = makeRequest(authUri, "GET", AuthTokenResponse.class, currentToken, null);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to exchange token for server player, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to exchange token for server player: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Get authorization token if one that was provided on player registration is outdated
     *
     * @param serverToken
     * @param successConsumer
     * @param errorConsumer
     */
    public void requestServerPlayerAuthorizationCode(String serverToken, Consumer<AuthorizationCode> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                URL authUri = GalleryConnection.getUri(SERVERS_PLAYERS_AUTHORIZATION_CODE_ENDPOINT);

                AuthorizationCode response = makeRequest(authUri, "GET", AuthorizationCode.class, serverToken, null);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable get authorization code for server player, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable get authorization code for server player: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Check that token from player is still valid
     * and check player's privileges
     * (are they banned from submitting lewd pics)
     *
     */
    public void checkPlayerToken(String token, Consumer<AuthCheckResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final URL checkUri = GalleryConnection.getUri(CHECK_ENDPOINT);
                AuthCheckResponse response = makeRequest(checkUri, "GET", AuthCheckResponse.class, token, null);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to request player token, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to request player token: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Deactivates player's token. Usually
     * happens 10 minutes later after player log-out event.
     *
     * @todo: [MED] Call this after player log out and timeout
     */
    public void dropPlayerToken(String token, Consumer<GenericMessageResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final URL checkUri = GalleryConnection.getUri(REVOKE_ENDPOINT);
                GenericMessageResponse response = makeRequest(checkUri, "GET", GenericMessageResponse.class, token, null);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to drop player token, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to drop player token: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Retrieves player's personal feed on Zetter Gallery
     *
     */
    public void getPlayerFeed(String token, Consumer<PaintingsResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final URL saleUri = GalleryConnection.getUri(PAINTINGS_FEED_ENDPOINT);
                PaintingsResponse response = makeRequest(saleUri, "GET", PaintingsResponse.class, token, null);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to request player feed, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to request player feed: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Register that player sees a painting
     *
     */
    public void registerImpression(String token, UUID paintingUuid, int cycleId, Consumer<GenericMessageResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final ImpressionRequest request = new ImpressionRequest(cycleId);

                final URL impressionUri = GalleryConnection.getUri(PAINTINGS_ENDPOINT + "/" + paintingUuid.toString() + "/" + PAINTINGS_IMPRESSION_ENDPOINT);
                GenericMessageResponse response = makeRequest(impressionUri, "POST", GenericMessageResponse.class, token, request);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to register player impression, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to register player impression: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Purchases painting on behalf of player
     *
     * @param paintingUuid uuid of purchased painting
     * @param price price in emeralds, 1-10
     */
    public void registerPurchase(String token, UUID paintingUuid, int price, int cycleId, Consumer<GenericMessageResponse> success, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final PurchaseRequest request = new PurchaseRequest(price, cycleId);

                final URL saleUri = GalleryConnection.getUri(PAINTINGS_ENDPOINT + "/" + paintingUuid.toString() + "/" + PAINTINGS_PURCHASES_ENDPOINT);
                GenericMessageResponse response = makeRequest(saleUri, "POST", GenericMessageResponse.class, token, request);

                executor.submitAsync(() -> success.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to register player purchase, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to register player purchase: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Check if painting can be submitted by player
     *
     * @param token
     * @param paintingData
     */
    public void validatePainting(String token, String paintingName, AbstractCanvasData paintingData, Consumer<PaintingsResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final SaleRequest request = new SaleRequest(paintingName, paintingData);

                final HashMap<String, String> query = new HashMap<>();
                query.put("save", "false");
                final URL validateUri = GalleryConnection.getUri(PAINTINGS_ENDPOINT, query);

                PaintingsResponse response = makeRequest(validateUri, "POST", PaintingsResponse.class, token, request);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to validate player painting, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to validate player painting: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    /**
     * Submits painting on behalf of player
     *
     * @todo: [HIGH] Wrong response type
     *
     * @param token
     * @param paintingData
     */
    public void sellPainting(String token, String paintingName, AbstractCanvasData paintingData, Consumer<PaintingsResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final SaleRequest request = new SaleRequest(paintingName, paintingData);

                final URL saleUri = GalleryConnection.getUri(PAINTINGS_ENDPOINT);
                PaintingsResponse response = makeRequest(saleUri, "POST", PaintingsResponse.class, token, request);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to sell player painting, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to sell player painting: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
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

        if (ZetterGallery.DEBUG_LOCALHOST) {
            return new URL(Helper.LOCALHOST_API + API_VERSION + "/" + endpoint + query);
        } else {
            return new URL(Helper.GALLERY_API + API_VERSION + "/" + endpoint + query);
        }
    }

    protected static <T> T makeRequest(URL uri, String method, Class<T> classOfT, @Nullable String token, @Nullable Object input) throws IOException, GalleryException {
        final HttpURLConnection connection = (HttpURLConnection) uri.openConnection();

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

            GenericMessageResponse errorResponse;

            scanner.close();
            connection.disconnect();

            try {
                errorResponse = GSON.fromJson(responseBody.toString(), GenericMessageResponse.class);
            } catch (JsonSyntaxException exception) {
                throw new GalleryException(connection.getResponseCode(), connection.getResponseMessage());
            }

            throw new GalleryException(connection.getResponseCode(), errorResponse.message);
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
        connection.setConnectTimeout(10 * 1000);
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
