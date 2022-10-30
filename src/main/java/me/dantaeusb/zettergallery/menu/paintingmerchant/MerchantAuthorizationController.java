package me.dantaeusb.zettergallery.menu.paintingmerchant;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.gallery.PlayerToken;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.network.packet.CGalleryAuthorizationCheckPacket;
import me.dantaeusb.zettergallery.network.packet.SGalleryAuthenticationCodeResponsePacket;
import me.dantaeusb.zettergallery.network.packet.SGalleryAuthenticationPlayerResponsePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class MerchantAuthorizationController {
    // We do not share token info because token only belongs to one client (Minecraft server)
    private @Nullable PlayerToken.PlayerInfo playerInfo;
    private @Nullable PlayerToken.CrossAuthCode crossAuthorizationCode;

    private boolean canSell = true;

    private final Player player;
    private final PaintingMerchantMenu menu;

    private PlayerAuthorizationState state = PlayerAuthorizationState.SERVER_AUTHENTICATION;
    private GalleryError error;

    public MerchantAuthorizationController(Player player, PaintingMerchantMenu menu) {
        this.player = player;
        this.menu = menu;
    }

    public PaintingMerchantMenu getMenu() {
        return this.menu;
    }

    public boolean isAuthorized() {
        return this.playerInfo != null;
    }

    public @Nullable PlayerToken.PlayerInfo getPlayerInfo() {
        return this.playerInfo;
    }

    public @Nullable PlayerToken.CrossAuthCode getCrossAuthorizationCode() {
        return this.crossAuthorizationCode;
    }

    public PlayerAuthorizationState getState() {
        return this.state;
    }

    public boolean hasError() {
        return this.state == PlayerAuthorizationState.ERROR;
    }

    public @Nullable GalleryError getError() {
        if (this.hasError()) {
            if (this.error == null) {
                this.error = new GalleryError(GalleryError.UNKNOWN, "Something went wrong");
            }

            return this.error;
        }

        return null;
    }

    public boolean canSell() {
        return this.isAuthorized() && this.canSell;
    }

    /**
     * Server-only, start auth flow by checking
     * Player's token and requesting one
     */
    public void startFlow() {
        ConnectionManager.getInstance().authenticateServerPlayer(
                (ServerPlayer) this.player,
                this::handleServerAuthentication,
                this::handleError
        );
    }

    /**
     * Handle authentication (token request) response:
     * Only on server
     * @param playerToken
     */
    private void handleServerAuthentication(PlayerToken playerToken) {
        final PlayerAuthorizationState previousState = this.getState();

        if (playerToken.isAuthorized()) {
            this.handleAuthorized(playerToken.getAuthorizedAs());
        } else {
            this.handleUnauthorized(playerToken.getCrossAuthCode());
        }

        // Ask to load offers only if we were waiting for server auth
        // When first loading or when retried after client authorization
        if (previousState == PlayerAuthorizationState.SERVER_AUTHENTICATION) {
            this.menu.getContainer().requrestOffers();
        }
    }

    /**
     * If player is authorized, let client player
     * know the info about profile they're using
     * @param authorizedAs
     */
    public void handleAuthorized(PlayerToken.PlayerInfo authorizedAs) {
        this.state = this.state.authorized();
        this.assertTargetState(PlayerAuthorizationState.LOGGED_IN);

        this.playerInfo = authorizedAs;

        if (!this.player.getLevel().isClientSide()) {
            SGalleryAuthenticationPlayerResponsePacket authorizationPacket = new SGalleryAuthenticationPlayerResponsePacket(authorizedAs);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), authorizationPacket);
        }
    }

    /**
     * If player is not authorized, let client player
     * know the code to cross-authorize server token
     * @param crossAuthorizationCode
     */
    public void handleUnauthorized(PlayerToken.CrossAuthCode crossAuthorizationCode) {
        this.state = this.state.unauthorized();
        this.assertTargetState(PlayerAuthorizationState.CLIENT_AUTHORIZATION);

        this.crossAuthorizationCode = crossAuthorizationCode;

        if (!this.player.getLevel().isClientSide()) {
            SGalleryAuthenticationCodeResponsePacket authorizationRequestPacket = new SGalleryAuthenticationCodeResponsePacket(crossAuthorizationCode);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), authorizationRequestPacket);
        }
    }

    /**
     * Say to server that player is back in game after they left
     * for authorization link. This should make server to check
     * authorization for player token again, and will send
     * player's rights back if everything's alright.
     * This is CLIENT_AUTHORIZATION state callback to get back
     * to SERVER_AUTHENTICATION state one more time
     */
    public void handleAuthorizationRetry() {
        this.state = this.state.retry();
        this.assertTargetState(PlayerAuthorizationState.SERVER_AUTHENTICATION);

        if (this.player.getLevel().isClientSide()) {
            CGalleryAuthorizationCheckPacket authenticationCheckPacket = new CGalleryAuthorizationCheckPacket();
            ZetterGalleryNetwork.simpleChannel.sendToServer(authenticationCheckPacket);
        } else {
            this.startFlow();
        }
    }

    public void handleError(GalleryError error) {
        final PlayerAuthorizationState previousState = this.getState();

        this.error = error;
        this.state = this.state.error();

        // Share error with offers
        if (previousState == PlayerAuthorizationState.SERVER_AUTHENTICATION) {
            this.menu.getContainer().handleError(this.error);
        }
    }

    private void assertTargetState(PlayerAuthorizationState state) {
        if (this.state != state) {
            if (this.state != PlayerAuthorizationState.ERROR) {
                throw new IllegalStateException("Unexpected state transition");
            }

            if (this.error == null) {
                this.error = new GalleryError(GalleryError.UNKNOWN_FSM_ERROR, "Invalid state");
            }

            ZetterGallery.LOG.error("Ended up in invalid state, expected: " + state.toString());
        }
    }

    public enum PlayerAuthorizationState {
        SERVER_AUTHENTICATION {
            @Override
            public PlayerAuthorizationState unauthorized() {
                return CLIENT_AUTHORIZATION;
            }

            @Override
            public PlayerAuthorizationState authorized() {
                return LOGGED_IN;
            }
        },
        CLIENT_AUTHORIZATION {
            @Override
            public PlayerAuthorizationState retry() {
                return SERVER_AUTHENTICATION;
            }
        },
        LOGGED_IN, // Final state
        ERROR; // Final state

        public PlayerAuthorizationState error() {
            return ERROR;
        }

        // @todo: [MID] Ugly state machine!
        public PlayerAuthorizationState retry() {
            return ERROR;
        }

        public PlayerAuthorizationState unauthorized() {
            return ERROR;
        }

        public PlayerAuthorizationState authorized() {
            return ERROR;
        }
    }
}
