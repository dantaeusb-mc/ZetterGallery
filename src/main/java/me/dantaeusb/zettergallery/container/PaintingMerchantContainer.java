package me.dantaeusb.zettergallery.container;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import com.google.common.collect.Lists;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.core.ZetterGalleryVillagerTrades;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.menu.paintingmerchant.MerchantAuthorizationController;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.network.packet.CGalleryProceedOfferPacket;
import me.dantaeusb.zettergallery.network.packet.CGallerySelectOfferPacket;
import me.dantaeusb.zettergallery.network.packet.SGalleryOfferStatePacket;
import me.dantaeusb.zettergallery.network.packet.SGalleryOffersPacket;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

public class PaintingMerchantContainer implements Container {
    public static final int STORAGE_SIZE = 2;

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);

    private final Player player;
    private final Merchant merchant;
    private final PaintingMerchantMenu menu;

    @Nullable
    private List<ContainerListener> listeners;
    private PaintingMerchantOffer currentOffer;

    // Waiting for the token check/update by default
    private OffersState state = OffersState.LOADING;
    private GalleryError error;

    private boolean locked = false;
    private int currentOfferIndex;

    @Nullable
    private List<PaintingMerchantOffer> offers;

    public PaintingMerchantContainer(Player player, Merchant merchant, PaintingMerchantMenu menu) {
        this.player = player;
        this.merchant = merchant;

        this.menu = menu;
    }

    public PaintingMerchantMenu getMenu() {
        return this.menu;
    }

    public OffersState getState() {
        return this.state;
    }

    public boolean hasError() {
        return this.state == OffersState.ERROR;
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

    /*
     * Zetter Networking
     */

    public void requrestOffers() {
        ConnectionManager.getInstance().requestOffers(
                (ServerPlayer) this.player,
                this,
                this::handleOffers,
                this::handleError
        );
    }

    /**
     * This is callback for offers request in FETCHING_SALES state.
     *
     * @param offers
     */
    public void handleOffers(List<PaintingMerchantOffer> offers) {
        if (this.state == OffersState.LOADING) {
            this.state = this.state.success();
        }

        this.offers = offers;
        this.updateCurrentOffer();
        this.registerOffersCanvases();

        if (!this.player.getLevel().isClientSide()) {
            SGalleryOffersPacket salesPacket = new SGalleryOffersPacket(this.getOffers());
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), salesPacket);
        }
    }

    public void registerOffersCanvases() {
        if (this.merchant.isClientSide() && this.getOffers() != null) {
            // Maybe delegate that to some kind of ClientSalesManager?
            ICanvasTracker tracker = Helper.getWorldCanvasTracker(this.merchant.getTradingPlayer().getLevel());

            for (PaintingMerchantOffer offer : this.getOffers()) {
                if (offer.getPaintingData().isEmpty()) {
                    throw new IllegalStateException("Painting doesn't have data to be registered");
                }

                PaintingData paintingData = offer.getPaintingData().get();
                paintingData.setManaged(true);

                tracker.registerCanvasData(offer.getCanvasCode(), paintingData);
            }
        }
    }

    public void unregisterOffersCanvases() {
        if (this.merchant.isClientSide() && this.getOffers() != null) {
            ICanvasTracker tracker = Helper.getWorldCanvasTracker(this.merchant.getTradingPlayer().getLevel());

            for (PaintingMerchantOffer offer : this.getOffers()) {
                tracker.unregisterCanvasData(offer.getCanvasCode());
            }
        }
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
            PaintingMerchantOffer offer = this.getCurrentOffer();

            if (offer.isSaleOffer()) {
                if (offer.getPaintingData().isEmpty()) {
                    Zetter.LOG.error("Painting data is not ready for checkout");
                    return;
                }

                ConnectionManager.getInstance().registerSale(
                        (ServerPlayer) this.player,
                        (PaintingData) offer.getPaintingData().get(),
                        this::finalizeCheckout,
                        offer::markError
                );
            } else {
                // Should never happen, theoretically
                if (offer.getPaintingData().isEmpty()) {
                    Zetter.LOG.error("Painting data is not ready for checkout");
                    return;
                }

                ConnectionManager.getInstance().registerPurchase(
                        (ServerPlayer) this.player,
                        ((GalleryPaintingData) offer.getPaintingData().get()).getUUID(),
                        offer.getPrice(),
                        this::finalizeCheckout,
                        offer::markError
                );
            }
        }

        this.lock();
    }

    /**
     * Response from server after sell/purchase request: either
     * request was fulfilled and player may take the painting or something went wrong
     * and we do nothing
     */
    public void finalizeCheckout() {
        if (this.getCurrentOffer() != null && this.getCurrentOffer().isReady()) {
            PaintingMerchantOffer offer = this.getCurrentOffer();

            this.setItem(OUTPUT_SLOT, offer.getOfferResult(this.merchant.getTradingPlayer().level));

            this.merchant.notifyTrade(this.getMerchantOffer(offer));
        }

        this.playTradeSound();

        this.unlock();
    }

    public void handleError(GalleryError error) {
        this.error = error;
        this.state = this.state.error();
    }

    public enum OffersState {
        LOADING {
            @Override
            public OffersState success() {
                return LOADED;
            }

            @Override
            public OffersState fail() {
                return ERROR;
            }
        },
        LOADED {
            @Override
            public OffersState success() {
                return LOADED;
            }

            @Override
            public OffersState fail() {
                return ERROR;
            }
        },
        ERROR {
            @Override
            public OffersState success() {
                return this;
            }

            @Override
            public OffersState fail() {
                return this;
            }
        };

        public abstract OffersState success();

        public abstract OffersState fail();

        private OffersState error() {
            return ERROR;
        }
    }

    /*
     * Zetter offers
     */

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }

    @Nullable
    public List<PaintingMerchantOffer> getOffers() {
        return this.offers;
    }

    public boolean hasOffers() {
        return this.offers != null && this.offers.size() > 0;
    }

    public int getOffersCount() {
        if (this.hasOffers()) {
            return this.getOffers().size();
        }

        return 0;
    }

    /**
     * Usually called on client to sync painting data
     * on current offer if it was not available
     * at the time of offer creating
     */
    public void updateCurrentOfferPaintingData(String canvasCode, PaintingData paintingData) {
        // If offer has changed
        if (this.getCurrentOffer() == null || !this.getCurrentOffer().getCanvasCode().equals(canvasCode)) {
            return;
        }

        this.currentOffer.updatePaintingData(paintingData);
    }

    /**
     * Called when items in container are changed,
     * i.e. when slot is clicked and onput or output changed
     *
     * Warning: this called multiple times, so in order
     * to prevent event spamming and data inconsistency, check
     * that offer is changed!
     */
    public void setChanged() {
        ItemStack inputStack = this.getInputSlot();

        if (this.hasOffers()) {
            if (inputStack.isEmpty()) {
                // Reset item if we were selling item
                this.updateCurrentOffer();

                if (this.getCurrentOffer() != null) {
                    this.getCurrentOffer().unfulfilled();
                }
            } else {
                if (inputStack.getItem() == ZetterItems.PAINTING.get()) {
                    // Current offer is to sell this painting, and we can proceed if validated
                    final String canvasCode = PaintingItem.getPaintingCode(inputStack);

                    /**
                     * If item was not changed, there's no need
                     * to update offer and send packet
                     */
                    if (this.currentOffer != null && this.currentOffer.isSaleOffer() && this.currentOffer.getCanvasCode().equals(canvasCode)) {
                        return;
                    }

                    PaintingData paintingData = Helper.getWorldCanvasTracker(this.merchant.getTradingPlayer().level).getCanvasData(canvasCode, PaintingData.class);
                    PaintingMerchantOffer offer = PaintingMerchantOffer.createOfferFromPlayersPainting(canvasCode, paintingData, 4);

                    this.currentOffer = offer;

                    // Ask Gallery if we can sell this painting
                    if (!this.merchant.isClientSide()) {
                        ConnectionManager.getInstance().validateSale(
                                (ServerPlayer) this.merchant.getTradingPlayer(),
                                offer,
                                () -> {
                                    offer.ready();

                                    SGalleryOfferStatePacket offerStatePacket = new SGalleryOfferStatePacket(offer.getCanvasCode(), PaintingMerchantOffer.State.READY, "Ready");
                                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.merchant.getTradingPlayer()), offerStatePacket);
                                },
                                (error) -> {
                                    offer.markError(error);

                                    SGalleryOfferStatePacket offerStatePacket = new SGalleryOfferStatePacket(offer.getCanvasCode(), PaintingMerchantOffer.State.ERROR, error.getClientMessage());
                                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.merchant.getTradingPlayer()), offerStatePacket);
                                }
                        );
                    }
                } else if (inputStack.getItem() == Items.EMERALD) {
                    this.updateCurrentOffer();

                    if (this.getCurrentOffer() != null && this.getCurrentOffer().getPrice() <= inputStack.getCount()) {
                        this.getCurrentOffer().ready();
                    }
                }
            }

            if (this.listeners != null) {
                for(ContainerListener containerlistener : this.listeners) {
                    containerlistener.containerChanged(this);
                }
            }
        }
    }

    private void playTradeSound() {
        if (!this.merchant.isClientSide()) {
            Entity entity = (Entity) this.merchant;
            entity.getLevel().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), this.merchant.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
        }
    }

    @Nullable
    public PaintingMerchantOffer getCurrentOffer() {
        return this.currentOffer;
    }

    public int getCurrentOfferIndex() {
        return this.currentOfferIndex;
    }

    public void updateCurrentOffer() {
        this.setCurrentOfferIndex(this.getCurrentOfferIndex());
    }

    private MerchantOffer getMerchantOffer(PaintingMerchantOffer offer) {
        if (offer.isSaleOffer()) {
            return this.merchant.getOffers().get(ZetterGalleryVillagerTrades.SELL_OFFER_ID);
        } else {
            return this.merchant.getOffers().get(ZetterGalleryVillagerTrades.BUY_OFFER_ID);
        }
    }

    public void setCurrentOfferIndex(int index) {
        if (!this.hasOffers()) {
            ZetterGallery.LOG.error("No offers loaded yet");
            return;
        }

        if (index >= this.offers.size()) {
            ZetterGallery.LOG.error("There's no offer with such index");
            return;
        }

        this.currentOfferIndex = index;
        this.currentOffer = this.offers.get(index);

        if (this.merchant.getTradingPlayer().level.isClientSide()) {
            CGallerySelectOfferPacket selectOfferPacket = new CGallerySelectOfferPacket(index);
            ZetterGalleryNetwork.simpleChannel.sendToServer(selectOfferPacket);
        }
    }

    /*
     * Basic things
     */

    public void addListener(ContainerListener listener) {
        if (this.listeners == null) {
            this.listeners = Lists.newArrayList();
        }

        this.listeners.add(listener);
    }

    public void removeListener(ContainerListener listener) {
        if (this.listeners != null) {
            this.listeners.remove(listener);
        }
    }

    @Override
    public int getContainerSize() {
        return STORAGE_SIZE;
    }

    /**
     * @todo: move all interactions to TE
     * @return
     */
    public ItemStack getInputSlot() {
        return this.getItem(INPUT_SLOT);
    }

    public ItemStack getOutputSlot() {
        return this.getItem(OUTPUT_SLOT);
    }

    public boolean stillValid(Player player) {
        return this.merchant.getTradingPlayer() == player;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return !this.locked;
    }

    public boolean canTakeItem(int index) {
        return !this.locked;
    }

    @Override
    public void startOpen(Player player) {

    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.itemStacks.size(); ++i) {
            if (!this.itemStacks.get(i).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public void stopOpen() {
        //
    }

    public void clearContent() {
        this.itemStacks.clear();
    }

    @Override
    public ItemStack getItem(int index) {
        return this.itemStacks.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return ContainerHelper.removeItem(this.itemStacks, index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(this.itemStacks, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.itemStacks.set(index, stack);
    }
}
