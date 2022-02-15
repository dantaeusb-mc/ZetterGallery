package me.dantaeusb.zettergallery.menu;

import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.container.PaintingMerchantContainer;
import me.dantaeusb.zettergallery.core.ZetterGalleryMenus;
import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.network.packet.*;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class PaintingMerchantMenu extends AbstractContainerMenu implements ContainerListener {
    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    public static final int PLAYER_INVENTORY_XPOS = 8;
    public static final int PLAYER_INVENTORY_YPOS = 154;

    private final Player player;
    private final Merchant merchant;
    private final PaintingMerchantContainer container;

    private UUID merchantId;
    private int merchantLevel;

    private State state = State.SERVER_AUTHENTICATION;
    @Nullable
    private String crossAuthorizationCode;

    @Nullable
    private String error;

    private PaintingMerchantMenu(int windowID, Inventory invPlayer, Merchant merchant) {
        super(ZetterGalleryMenus.PAINTING_MERCHANT, windowID);

        this.player = invPlayer.player;
        this.merchant = merchant;
        this.container = new PaintingMerchantContainer(merchant);
        this.container.addListener(this);

        // gui position of the player material slots
        final int INPUT_XPOS = 15;
        final int INPUT_YPOS = 83;

        // gui position of the player material slots
        final int OUTPUT_XPOS = 149;
        final int OUTPUT_YPOS = 83;

        this.addSlot(new PaintingMerchantMenu.SlotInput(this.container, 0, INPUT_XPOS, INPUT_YPOS));
        this.addSlot(new PaintingMerchantMenu.SlotOutput(this.container, 1, OUTPUT_XPOS, OUTPUT_YPOS));

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 212;

        // Add the players hotbar to the gui - the [xpos, ypos] location of each item
        for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            this.addSlot(new Slot(invPlayer, x, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }

        // Add the rest of the players inventory to the gui
        for (int y = 0; y < PLAYER_INVENTORY_ROW_COUNT; y++) {
            for (int x = 0; x < PLAYER_INVENTORY_COLUMN_COUNT; x++) {
                int slotNumber = HOTBAR_SLOT_COUNT + y * PLAYER_INVENTORY_COLUMN_COUNT + x;
                int xpos = PLAYER_INVENTORY_XPOS + x * SLOT_X_SPACING;
                int ypos = PLAYER_INVENTORY_YPOS + y * SLOT_Y_SPACING;
                this.addSlot(new Slot(invPlayer, slotNumber, xpos, ypos));
            }
        }

        // We don't call this.update() here because it can use callback before menu created on client
        // Instead, we hook for PlayerContainerEvent.Open event in ZetterGalleryGameEvents.
    }

    public static PaintingMerchantMenu createMenuServerSide(int windowID, Inventory playerInventory, Merchant merchant) {
        return new PaintingMerchantMenu(windowID, playerInventory, merchant);
    }

    public static PaintingMerchantMenu createMenuClientSide(int windowID, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf networkBuffer) {
        Merchant merchant = new ClientSideMerchant(playerInventory.player);

        return new PaintingMerchantMenu(windowID, playerInventory, merchant);
    }

    public void setMerchantLevel(int level) {
        this.merchantLevel = level;
    }

    public int getMerchantLevel() {
        return this.merchantLevel;
    }

    public void setMerchantId(UUID uuid) {
        this.merchantId = uuid;
    }

    public UUID getMerchantId() {
        return this.merchantId;
    }

    public PaintingMerchantContainer getContainer() {
        return this.container;
    }

    @Override
    public void containerChanged(Container container) {

    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean stillValid(Player player) {
        //return this.merchant.getCustomer() == player;
        return this.container.stillValid(player);
    }

    /**
     * @param playerIn
     * @param sourceSlotIndex
     * @return
     */
    @Override
    public ItemStack quickMoveStack(Player playerIn, int sourceSlotIndex) {
        ItemStack outStack = ItemStack.EMPTY;
        Slot sourceSlot = this.slots.get(sourceSlotIndex);

        if (sourceSlot != null && sourceSlot.hasItem()) {
            ItemStack sourceStack = sourceSlot.getItem();
            outStack = sourceStack.copy();

            // Palette
            if (sourceSlotIndex == 0) {
                if (!this.moveItemStackTo(sourceStack, 2, 10, true)) {
                    return ItemStack.EMPTY;
                }

                //sourceSlot.onSlotChange(sourceStack, outStack);

                // Inventory
            } else {
                if (sourceStack.getItem() == ZetterItems.PALETTE) {
                    if (!this.moveItemStackTo(sourceStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (sourceStack.isEmpty()) {
                sourceSlot.set(ItemStack.EMPTY);
            } else {
                sourceSlot.setChanged();
            }

            if (sourceStack.getCount() == outStack.getCount()) {
                return ItemStack.EMPTY;
            }

            sourceSlot.onTake(playerIn, sourceStack);
        }

        return outStack;
    }

    public Merchant getMerchant() {
        return this.merchant;
    }

    /**
     * Pass calls to storage
     */

    public boolean hasOffers() {
        return this.container.hasOffers() && !this.state.equals(State.ERROR);
    }

    /**
     * Start sell/purchase process - send message to the server that player
     * intents to buy a painting
     */
    public void startCheckout() {
        if (this.merchant.getTradingPlayer().getLevel().isClientSide()) {
            // Send message to server, code in else section will be called
            CGalleryProceedOfferPacket selectOfferPacket = new CGalleryProceedOfferPacket();
            ZetterGalleryNetwork.simpleChannel.sendToServer(selectOfferPacket);
        } else {
            PaintingMerchantOffer offer = this.container.getCurrentOffer();

            if (offer.isSaleOffer()) {
                ConnectionManager.getInstance().registerSale(
                        (ServerPlayer) this.player,
                        (GalleryPaintingData) offer.getPaintingData(),
                        this::finalizeCheckout,
                        offer::markError
                );
            } else {
                ConnectionManager.getInstance().registerPurchase(
                        (ServerPlayer) this.player,
                        ((GalleryPaintingData) offer.getPaintingData()).getUUID(),
                        offer.getPrice(),
                        this::finalizeCheckout,
                        offer::markError
                );
            }
        }

        this.container.lock();
    }

    /**
     * Response from server after sell/purchase request: either
     * request was fulfilled and player may take the painting or something went wrong
     * and we do nothing
     */
    public void finalizeCheckout() {
        this.container.finishSale();
        this.playTradeSound();

        this.container.unlock();
    }

    public int getOffersCount() {
        return this.container.getOffersCount();
    }

    @Nullable
    public PaintingMerchantOffer getCurrentOffer() {
        return this.container.getCurrentOffer();
    }

    public void updateCurrentOfferIndex(int index) {
        this.container.setCurrentOfferIndex(index);
    }

    public int getCurrentOfferIndex() {
        return this.container.getCurrentOfferIndex();
    }

    public boolean isSaleAllowed() {
        return this.container.isSaleAllowed();
    }

    public State getState() {
        return this.state;
    }

    public @Nullable
    String getError() {
        return this.error;
    }

    /**
     * Return specific cross-authorization code, which should
     * authorize server to do some actions on behalf of player.
     * This should be used on GUI to provide link to player.
     *
     * @return
     */
    @Nullable
    public String getCrossAuthorizationCode() {
        return this.crossAuthorizationCode;
    }

    /**
     * General callback for all state changes. If something has to be
     * done in new state, no matter which state we came from
     *
     * Move to the state machine enum?
     */
    public void update() {
        if (!this.player.level.isClientSide()) {
            switch (this.state) {
                case SERVER_AUTHENTICATION:
                    ConnectionManager.getInstance().authorizeServerPlayer(
                            (ServerPlayer) this.player,
                            // to FETCHING_SALES
                            this::handleServerAuthenticationSuccess,
                            // to CLIENT_AUTHORIZATION
                            this::handleServerAuthenticationFail,
                            this::handleError
                    );
                    break;
                case CLIENT_AUTHORIZATION:
                    // Server
                    SGalleryAuthorizationRequestPacket authorizationRequestPacket = new SGalleryAuthorizationRequestPacket(this.crossAuthorizationCode);
                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), authorizationRequestPacket);
                    break;
                case FETCHING_OFFERS:
                    SGalleryAuthorizationResponsePacket authorizationResponsePacket = new SGalleryAuthorizationResponsePacket(true, true);
                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), authorizationResponsePacket);

                    ConnectionManager.getInstance().requestOffers(
                            (ServerPlayer) this.player,
                            this,
                            (offers) -> {
                                this.handleOffers(true, offers);
                            },
                            this::handleError
                    );
                    break;
                case RETRY:
                    break;
                case READY:
                    // sync with client when updating, calls the same method on client side
                    SGallerySalesPacket salesPacket = new SGallerySalesPacket(this.isSaleAllowed(), this.getContainer().getOffers());
                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), salesPacket);

                    break;
                case ERROR:
                    SGalleryErrorPacket errorPacket = new SGalleryErrorPacket(this.error);
                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), errorPacket);

                    break;
                default:
                    break;
            }
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
    public void requestUpdateAuthenticationStatus() {
        CGalleryAuthorizationCheckPacket authenticationCheckPacket = new CGalleryAuthorizationCheckPacket();
        ZetterGalleryNetwork.simpleChannel.sendToServer(authenticationCheckPacket);

        this.state = this.state.success();
    }

    /**
     * Assert that we are in given state, and if everything
     * is correct, change to the next state by
     * success path
     *
     * @param assertState
     */
    private void assertStateSuccess(State assertState) {
        if (this.state == assertState) {
            this.state = this.state.success();
        } else {
            this.state = this.state.error();
        }

        this.update();
    }

    /**
     * Assert that we are in given state, and if everything
     * is correct, change to the next state by
     * fail path
     *
     * @param assertState
     */
    private void assertStateFail(State assertState) {
        if (this.state == assertState) {
            this.state = this.state.fail();
        } else {
            this.state = this.state.error();
        }

        this.update();
    }

    /**
     * General callback for handling errors,
     * also called on many states update fail
     *
     * @param error
     */
    public void handleError(String error) {
        this.state = this.state.error();
        this.error = error;

        this.update();
    }

    private void playTradeSound() {
        if (!this.merchant.isClientSide()) {
            Entity entity = (Entity) this.merchant;
            entity.getLevel().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), this.merchant.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
        }
    }

    /*
     * Event handling
     */

    /**
     * This is callback for offers request in FETCHING_SALES state.
     *
     * @param sellAllowed
     * @param offers
     */
    public void handleOffers(boolean sellAllowed, List<PaintingMerchantOffer> offers) {
        if (this.state == State.FETCHING_OFFERS) {
            this.state = this.state.success();
        }

        this.container.handleOffers(offers);
        this.registerOffersCanvases();

        this.update();
    }

    public void handleOfferState(PaintingMerchantOffer.State state, String message) {
        if (state == PaintingMerchantOffer.State.ERROR) {
            this.getCurrentOffer().markError(message);
        } else if (state == PaintingMerchantOffer.State.READY) {
            this.getCurrentOffer().ready();
        }
    }

    public void handleServerAuthenticationSuccess(boolean canBuy, boolean canSell) {
        this.assertStateSuccess(State.SERVER_AUTHENTICATION);
    }

    public void handleServerAuthenticationFail(String crossAuthorizationCode) {
        this.crossAuthorizationCode = crossAuthorizationCode;
        this.assertStateFail(State.SERVER_AUTHENTICATION);
    }

    public void handleServerAuthenticationRetry() {
        this.assertStateSuccess(State.CLIENT_AUTHORIZATION);
    }

    public void registerOffersCanvases() {
        if (this.merchant.isClientSide() && this.getContainer().getOffers() != null) {
            // Maybe delegate that to some kind of ClientSalesManager?
            ICanvasTracker tracker = Helper.getWorldCanvasTracker(this.merchant.getTradingPlayer().getLevel());

            for (PaintingMerchantOffer offer : this.getContainer().getOffers()) {
                PaintingData paintingData = offer.getPaintingData();
                paintingData.setManaged(true);

                tracker.registerCanvasData(offer.getCanvasCode(), paintingData);
            }
        }
    }

    public void unregisterOffersCanvases() {
        if (this.merchant.isClientSide() && this.getContainer().getOffers() != null) {
            ICanvasTracker tracker = Helper.getWorldCanvasTracker(this.merchant.getTradingPlayer().getLevel());

            for (PaintingMerchantOffer offer : this.getContainer().getOffers()) {
                tracker.unregisterCanvasData(offer.getCanvasCode());
            }
        }
    }

    /*
     * Helpers
     */

    /**
     * Called when the container is closed.
     */
    public void removed(Player player) {
        super.removed(player);
        this.merchant.setTradingPlayer((Player) null);

        if (!this.merchant.isClientSide()) {
            this.unregisterOffersCanvases();

            if (!player.isAlive() || player instanceof ServerPlayer && ((ServerPlayer) player).hasDisconnected()) {
                ItemStack itemstack = this.container.removeItemNoUpdate(PaintingMerchantContainer.INPUT_SLOT);
                if (!itemstack.isEmpty()) {
                    player.drop(itemstack, false);
                }

                itemstack = this.container.removeItemNoUpdate(PaintingMerchantContainer.OUTPUT_SLOT);
                if (!itemstack.isEmpty()) {
                    player.drop(itemstack, false);
                }
            } else {
                player.getInventory().placeItemBackInInventory(this.container.removeItemNoUpdate(0));
            }
        }
    }

    private boolean areItemStacksEqual(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem() && ItemStack.isSame(stack1, stack2);
    }

    public class SlotInput extends Slot {
        public SlotInput(Container inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!PaintingMerchantMenu.this.hasOffers()) {
                return false;
            }

            if (!PaintingMerchantMenu.this.container.canPlaceItem(PaintingMerchantContainer.INPUT_SLOT, stack)) {
                return false;
            }

            if (stack.getItem() == Items.EMERALD) {
                return true;
            }

            if (stack.getItem() == ZetterItems.PAINTING && PaintingMerchantMenu.this.isSaleAllowed()) {
                return true;
            }

            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return PaintingMerchantMenu.this.container.canTakeItem(PaintingMerchantContainer.INPUT_SLOT);
        }
    }

    public class SlotOutput extends Slot {
        public SlotOutput(Container inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        /**
         * Wrong? Or side effected
         *
         * @param player
         * @param stack
         */
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            //PaintingMerchantMenu.this.takeOutput(player, stack);
        }
    }

    public enum State {
        SERVER_AUTHENTICATION {
            @Override
            public State success() {
                return FETCHING_OFFERS;
            }

            @Override
            public State fail() {
                return CLIENT_AUTHORIZATION;
            }
        },
        CLIENT_AUTHORIZATION {
            @Override
            public State success() {
                return SERVER_AUTHENTICATION;
            }

            @Override
            public State fail() {
                return ERROR;
            }
        },
        FETCHING_OFFERS {
            @Override
            public State success() {
                return READY;
            }

            @Override
            public State fail() {
                return CLIENT_AUTHORIZATION;
            }
        },
        READY {
            @Override
            public State success() {
                return READY;
            }

            @Override
            public State fail() {
                return RETRY;
            }
        },
        RETRY {
            @Override
            public State success() {
                return READY;
            }

            @Override
            public State fail() {
                return this;
            }
        },
        ERROR {
            @Override
            public State success() {
                return this;
            }

            @Override
            public State fail() {
                return this;
            }
        };

        public abstract State success();

        public abstract State fail();

        public State error() {
            return ERROR;
        }

        ;
    }
}
