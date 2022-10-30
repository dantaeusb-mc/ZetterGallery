package me.dantaeusb.zettergallery.network.http;

import com.google.gson.JsonSyntaxException;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import com.google.gson.Gson;
import me.dantaeusb.zettergallery.core.Helper;
import me.dantaeusb.zettergallery.gallery.PlayerTokenStorage;
import me.dantaeusb.zettergallery.gallery.ServerInfo;
import me.dantaeusb.zettergallery.network.http.stub.*;
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
import java.util.function.Consumer;

/**
 * @todo: make singleton
 */
public class GalleryConnection {
    private static final String API_VERSION = "v1";
    private static final String SERVERS_ENDPOINT = "servers";
    private static final String TOKEN_ENDPOINT = "auth/token";
    private static final String CHECK_ENDPOINT = "auth/token/check";
    private static final String DROP_ENDPOINT = "auth/drop";
    private static final String PAINTINGS_ENDPOINT = "paintings";
    private static final String PAINTINGS_PURCHASES_ENDPOINT = "sales";
    private static final String PAINTINGS_IMPRESSION_ENDPOINT = "impressions";
    private static final String PAINTINGS_FEED_ENDPOINT = "paintings/feed";

    private static final Gson GSON = new Gson();

    private final PlayerTokenStorage tokenStorage = PlayerTokenStorage.getInstance();

    private final ExecutorService poolExecutor;

    public GalleryConnection() {
        this.poolExecutor = Executors.newSingleThreadExecutor();
    }

    public void registerServer(ServerInfo serverInfo, Consumer<ServerResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final RegisterRequest request = new RegisterRequest(serverInfo.singleplayer, serverInfo.title, serverInfo.motd, serverInfo.galleryVersion);
                URL authUri = GalleryConnection.getUri(SERVERS_ENDPOINT);

                ServerResponse response = makeRequest(authUri, "POST", ServerResponse.class, (String) null, request);

                executor.submitAsync(() -> successConsumer.accept(response));
            } catch (GalleryException e) {
                ZetterGallery.LOG.error(String.format("Unable to request token for server, Gallery returned error: [%d] %s", e.getCode(), e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(e.getCode(), e.getMessage())));
            } catch (Exception e) {
                ZetterGallery.LOG.error(String.format("Unable to request token for server: %s", e.getMessage()));

                executor.submitAsync(() -> errorConsumer.accept(new GalleryError(0, e.getMessage())));
            }
        });
    }

    public void unregisterServer(String serverToken, UUID serverUuid, Consumer<GenericMessageResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                URL authUri = GalleryConnection.getUri(SERVERS_ENDPOINT + "/" + serverUuid.toString());

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
     *
     *
     */
    public void requestPlayerToken(@Nullable String token, Consumer<AuthTokenResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
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
                AuthTokenResponse response = makeRequest(authUri, "GET", AuthTokenResponse.class, token, null);

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
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

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
     * happens on player log-out event.
     *
     */
    public void dropPlayerToken(String token, Consumer<GenericMessageResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final URL checkUri = GalleryConnection.getUri(DROP_ENDPOINT);
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
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

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
    public void registerImpression(String token, UUID paintingUuid, Consumer<GenericMessageResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final URL saleUri = GalleryConnection.getUri(PAINTINGS_ENDPOINT + "/" + paintingUuid.toString() + "/" + PAINTINGS_IMPRESSION_ENDPOINT);
                GenericMessageResponse response = makeRequest(saleUri, "POST", GenericMessageResponse.class, token, null);

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
    public void registerPurchase(String token, UUID paintingUuid, int price, Consumer<GenericMessageResponse> success, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final PurchaseRequest request = new PurchaseRequest(price);

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
    public void validatePainting(String token, PaintingData paintingData, Consumer<PaintingsResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
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
     * @param token
     * @param paintingData
     */
    public void sellPainting(String token, PaintingData paintingData, Consumer<PaintingsResponse> successConsumer, Consumer<GalleryError> errorConsumer) {
        this.poolExecutor.execute(() -> {
            /**
             * @link {#NetworkEvent.enqueueWork}
             */
            BlockableEventLoop<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

            try {
                final SaleRequest request = new SaleRequest(paintingData);

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
