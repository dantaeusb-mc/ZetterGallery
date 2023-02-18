package me.dantaeusb.zettergallery.menu.paintingmerchant;

import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.gallery.AuthorizationCode;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.gallery.PlayerToken;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.network.packet.CAuthorizationCheckPacket;
import me.dantaeusb.zettergallery.network.packet.SAuthErrorPacket;
import me.dantaeusb.zettergallery.network.packet.SAuthenticationPlayerResponsePacket;
import me.dantaeusb.zettergallery.network.packet.SAuthorizationCodeResponsePacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;

public class MerchantAuthorizationController {
    // We do not share token info because token only belongs to one client (Minecraft server)
    private @Nullable PlayerToken.PlayerInfo playerInfo;
    private @Nullable AuthorizationCode authorizationCode;

    private boolean canSell = true;

    private final PlayerEntity player;
    private final PaintingMerchantMenu menu;

    private PlayerAuthorizationState state = PlayerAuthorizationState.SERVER_AUTHENTICATION;
    private GalleryError error;

    public MerchantAuthorizationController(PlayerEntity player, PaintingMerchantMenu menu) {
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

    public @Nullable AuthorizationCode getAuthorizationCode() {
        return this.authorizationCode;
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

    public boolean canSell(ItemStack painting) {
        return
                this.isAuthorized()
                && this.canSell
                && PaintingItem.getPaintingCode(painting) != null;
    }

    /**
     * Server-only, start auth flow by checking
     * Player's token and requesting one
     */
    public void startFlow() {
        ConnectionManager.getInstance().authenticateServerPlayer(
                (ServerPlayerEntity) this.player,
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
        } else if (playerToken.getAuthorizationCode() != null) {
            this.handleUnauthorized(playerToken.getAuthorizationCode());
        }

        // Ask to load offers only if we were waiting for server auth
        // When first loading or when retried after client authorization
        if (previousState == PlayerAuthorizationState.SERVER_AUTHENTICATION) {
            this.menu.getContainer().requestFeed();
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

        this.authorizationCode = null;
        this.playerInfo = authorizedAs;

        if (!this.player.level.isClientSide()) {
            SAuthenticationPlayerResponsePacket authorizationPacket = new SAuthenticationPlayerResponsePacket(authorizedAs);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) this.player), authorizationPacket);
        }
    }

    /**
     * If player is not authorized, let client player
     * know the code to cross-authorize server token
     * @param authorizationCodeInfo
     */
    public void handleUnauthorized(AuthorizationCode authorizationCodeInfo) {
        this.state = this.state.unauthorized();
        this.assertTargetState(PlayerAuthorizationState.CLIENT_AUTHORIZATION);

        this.authorizationCode = authorizationCodeInfo;

        if (!this.player.level.isClientSide()) {
            SAuthorizationCodeResponsePacket authorizationRequestPacket = new SAuthorizationCodeResponsePacket(authorizationCodeInfo);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) this.player), authorizationRequestPacket);
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

        if (this.player.level.isClientSide()) {
            CAuthorizationCheckPacket authenticationCheckPacket = new CAuthorizationCheckPacket();
            ZetterGalleryNetwork.simpleChannel.sendToServer(authenticationCheckPacket);
        } else {
            this.startFlow();
        }
    }

    public void handleError(GalleryError error) {
        final PlayerAuthorizationState previousState = this.getState();

        this.error = error;
        this.state = this.state.error();

        // Share error with offers if it happened on server auth stage
        if (previousState == PlayerAuthorizationState.SERVER_AUTHENTICATION) {
            this.menu.getContainer().handleError(this.error);
        }

        if (!this.player.level.isClientSide()) {
            // Send message to server, code in else section will be called
            SAuthErrorPacket selectOfferPacket = new SAuthErrorPacket(this.error);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) this.player), selectOfferPacket);
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
