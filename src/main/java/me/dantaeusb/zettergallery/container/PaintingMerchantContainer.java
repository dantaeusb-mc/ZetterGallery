package me.dantaeusb.zettergallery.container;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterItems;
import com.google.common.collect.Lists;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.core.ZetterGalleryVillagerTrades;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.gallery.SalesManager;
import me.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.network.packet.*;
import me.dantaeusb.zettergallery.trading.IPaintingMerchantOffer;
import me.dantaeusb.zettergallery.trading.PaintingMerchantPurchaseOffer;
import me.dantaeusb.zettergallery.trading.PaintingMerchantSaleOffer;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Date;
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
    private IPaintingMerchantOffer currentOffer;

    // Waiting for the token check/update by default
    private OffersState state = OffersState.LOADING;
    private GalleryError error;

    // @todo: [HIGH] I don't think we need that anymore
    private boolean locked = false;
    private int selectedPurchaseOfferIndex;

    // Time, in milliseconds, for which we allow player to use previous feed
    // By design of Zetter Gallery API, it's allowed to query only one
    // Previous cycle, so it should be feed time (Currently 5minutes) at max
    // We use half of that time
    private static final int FORCE_FEED_UPDATE_TIMEOUT = (int) (2.5f * 30 * 1000);

    private PaintingsResponse.CycleInfo currentCycle;

    @Nullable
    private List<PaintingMerchantPurchaseOffer> purchaseOffers;

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

    public @Nullable PaintingsResponse.CycleInfo getCurrentCycle() {
        return this.currentCycle;
    }

    public int getSecondsToNextCycle() {
        if (this.currentCycle == null) {
            return 0;
        }

        return (int) (this.currentCycle.endsAt.getTime() - new Date().getTime()) / 1000;
    }

    public int getSecondsToForceUpdateCycle() {
        if (this.currentCycle == null) {
            return 0;
        }

        return (int) ((this.currentCycle.endsAt.getTime() + FORCE_FEED_UPDATE_TIMEOUT) - new Date().getTime()) / 1000;
    }

    public boolean canUpdate() {
        if (this.currentCycle == null) {
            return false;
        }

        return this.getSecondsToNextCycle() < 0;
    }

    public boolean needUpdate() {
        if (this.currentCycle == null) {
            return false;
        }

        return this.getSecondsToForceUpdateCycle() < 0;
    }

    /**
     * Called when items in container are changed,
     * i.e. when slot is clicked and onput or output changed
     *
     * Warning: this called multiple times, so in order
     * to prevent event spamming and data inconsistency, check
     * that offer is changed!
     */
    @Override
    public void setChanged() {
        ItemStack inputStack = this.getInputSlot();
        PaintingMerchantPurchaseOffer selectedPurchaseOffer = null;

        if (this.purchaseOffers != null && this.purchaseOffers.size() > this.selectedPurchaseOfferIndex) {
            selectedPurchaseOffer = this.purchaseOffers.get(this.selectedPurchaseOfferIndex);
        }

        if (this.hasOffers()) {
            if (inputStack.isEmpty()) {
                if (selectedPurchaseOffer != null && !selectedPurchaseOffer.equals(this.currentOffer)) {
                    this.currentOffer = selectedPurchaseOffer;
                }

                if (this.currentOffer != null) {
                    this.currentOffer.unfulfilled();
                }

                this.setItem(OUTPUT_SLOT, ItemStack.EMPTY);
            } else {
                if (inputStack.getItem() == ZetterItems.PAINTING.get()) {
                    // Current offer is to sell this painting, and we can proceed if validated
                    final String canvasCode = PaintingItem.getPaintingCode(inputStack);

                    /**
                     * If item was not changed, there's no need
                     * to update offer and send packet
                     *
                     * Only need to update result
                     */
                    if (
                        this.currentOffer != null && this.currentOffer instanceof PaintingMerchantSaleOffer
                        && this.currentOffer.getRealCanvasCode().equals(canvasCode)
                    ) {
                        if (this.currentOffer.isReady()) {
                            this.setItem(OUTPUT_SLOT, currentOffer.getOfferResult());
                        }

                        return;
                    }

                    PaintingData paintingData = Helper.getWorldCanvasTracker(this.merchant.getTradingPlayer().level).getCanvasData(canvasCode);

                    PaintingMerchantSaleOffer saleOffer;
                    if (paintingData != null) {
                        saleOffer = PaintingMerchantSaleOffer.createOfferFromPlayersPainting(canvasCode, paintingData, 4);

                        Level level = this.merchant.getTradingPlayer().getLevel();
                        if (level.isClientSide()) {
                            saleOffer.register(this.merchant.getTradingPlayer().getLevel());
                        }
                    } else {
                        saleOffer = PaintingMerchantSaleOffer.createOfferWithoutPlayersPainting(canvasCode, this.merchant.getTradingPlayer(), 4);
                        CanvasRenderer.getInstance().queueCanvasTextureUpdate(canvasCode);
                    }

                    this.currentOffer = saleOffer;

                    if (saleOffer.validate(this.merchant)) {
                        // Ask Gallery if we can sell this painting
                        if (!this.merchant.isClientSide()) {
                            ConnectionManager.getInstance().validateSale(
                                (ServerPlayer) this.merchant.getTradingPlayer(),
                                saleOffer,
                                () -> {
                                    saleOffer.ready();

                                    SOfferStatePacket offerStatePacket = new SOfferStatePacket(saleOffer.getDummyCanvasCode(), PaintingMerchantPurchaseOffer.State.READY, "Ready");
                                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.merchant.getTradingPlayer()), offerStatePacket);
                                },
                                (error) -> {
                                    saleOffer.markError(error);

                                    SOfferStatePacket offerStatePacket = new SOfferStatePacket(saleOffer.getDummyCanvasCode(), PaintingMerchantPurchaseOffer.State.ERROR, error.getClientMessage());
                                    ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.merchant.getTradingPlayer()), offerStatePacket);
                                }
                            );
                        }
                    }
                } else if (inputStack.getItem() == Items.EMERALD) {
                    if (selectedPurchaseOffer != null && !selectedPurchaseOffer.equals(this.currentOffer)) {
                        this.currentOffer = selectedPurchaseOffer;
                    }

                    if (this.currentOffer != null) {
                        if (currentOffer.getPrice() <= inputStack.getCount()) {
                            this.currentOffer.ready();

                            this.setItem(OUTPUT_SLOT, currentOffer.getOfferResult());
                        } else {
                            this.currentOffer.unfulfilled();

                            this.setItem(OUTPUT_SLOT, ItemStack.EMPTY);
                        }
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

    /*
     * Zetter Networking
     */

    /**
     * Server-only, asks sales manager to load offers for that player/merchant pair
     */
    public void requestFeed() {
        SalesManager.getInstance().registerTrackingPlayer((ServerPlayer) this.player);
        SalesManager.getInstance().acquireMerchantOffers(
                (ServerPlayer) this.player,
                this,
                this::handleFeed,
                this::handleError
        );
    }

    /**
     * This is callback for offers request in FETCHING_SALES state.
     * Only on server, sends packet to client that is processed separately.
     *
     * @param offers
     */
    public void handleFeed(PaintingsResponse.CycleInfo cycleInfo, List<PaintingMerchantPurchaseOffer> offers) {
        if (this.state == OffersState.LOADING) {
            this.state = this.state.success();
        }

        this.currentCycle = cycleInfo;
        this.purchaseOffers = offers;
        this.updateCurrentOffer();
        this.registerOffersCanvases();

        if (!this.player.getLevel().isClientSide()) {
            SOffersPacket salesPacket = new SOffersPacket(cycleInfo, this.getPurchaseOffers());
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), salesPacket);
        }
    }

    public void registerOffersCanvases() {
        if (this.merchant.isClientSide() && this.getPurchaseOffers() != null) {
            // Maybe delegate that to some kind of ClientSalesManager?
            ICanvasTracker tracker = Helper.getWorldCanvasTracker(this.merchant.getTradingPlayer().getLevel());

            for (PaintingMerchantPurchaseOffer offer : this.getPurchaseOffers()) {
                DummyCanvasData paintingData = offer.getDummyPaintingData();
                tracker.registerCanvasData(offer.getDummyCanvasCode(), paintingData);
            }
        }
    }

    public void unregisterOffersCanvases() {
        if (this.merchant.isClientSide()) {
            Level level = this.merchant.getTradingPlayer().getLevel();;

            if (this.getPurchaseOffers() != null) {
                ICanvasTracker tracker = Helper.getWorldCanvasTracker(level);

                for (PaintingMerchantPurchaseOffer offer : this.getPurchaseOffers()) {
                    tracker.unregisterCanvasData(offer.getDummyCanvasCode());
                }
            }

            PaintingMerchantSaleOffer.unregister(level);
        }
    }

    /**
     * Start sell/purchase process - send message to the server that player
     * intents to buy a painting.
     *
     * We do not really care much for response: when purchasing,
     * Zetter Gallery just logs the data, and player should not be affected
     * When selling, validation part should be enough to make sure painting will
     * be saved successfully, except for the very rare cases that player should not
     * care about.
     */
    public void checkout(ItemStack purchaseStack) {
        IPaintingMerchantOffer offer = this.getCurrentOffer();

        if (offer instanceof PaintingMerchantSaleOffer saleOffer) {
            if (!offer.isReady()) {
                Zetter.LOG.error("Offer is not ready for checkout");
                return;
            }

            if (!this.merchant.getTradingPlayer().getLevel().isClientSide()) {
                ConnectionManager.getInstance().registerSale(
                    (ServerPlayer) this.player,
                    saleOffer,
                    this::finalizeCheckout,
                    offer::markError
                );
            }

            this.itemStacks.set(INPUT_SLOT, ItemStack.EMPTY);
        } else if (offer instanceof PaintingMerchantPurchaseOffer purchaseOffer) {
            if (!this.merchant.getTradingPlayer().getLevel().isClientSide()) {
                ConnectionManager.getInstance().registerPurchase(
                    (ServerPlayer) this.player,
                    purchaseOffer.getPaintingUuid(),
                    purchaseOffer.getPrice(),
                    this::finalizeCheckout,
                    offer::markError
                );
            }

            this.itemStacks.get(INPUT_SLOT).shrink(offer.getPrice());
            purchaseOffer.writeOfferResultData(this.merchant.getTradingPlayer().getLevel(), purchaseStack);
        }

        // Reset item if we were selling item
        this.updateCurrentOffer();
    }

    /**
     * Response from server after sell/purchase request: either
     * request was fulfilled and player may take the painting or something went wrong
     * and we do nothing
     */
    public void finalizeCheckout() {
        IPaintingMerchantOffer offer = this.getCurrentOffer();
        this.merchant.notifyTrade(this.getVanillaMerchantOffer(offer));
        this.playTradeSound();
    }

    public void handleError(GalleryError error) {
        this.error = error;
        this.state = this.state.error();
    }

    public void removed() {
        if (this.merchant.getTradingPlayer().getLevel().isClientSide()) {
            this.unregisterOffersCanvases();
        } else {
            SalesManager.getInstance().unregisterTrackingPlayer((ServerPlayer) this.player);
        }
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
    public List<PaintingMerchantPurchaseOffer> getPurchaseOffers() {
        return this.purchaseOffers;
    }

    public boolean hasOffers() {
        return this.purchaseOffers != null && this.purchaseOffers.size() > 0;
    }

    public int getOffersCount() {
        if (this.hasOffers()) {
            return this.getPurchaseOffers().size();
        }

        return 0;
    }

    /**
     * If painting was not loaded before player attempted to submit painting
     * @param canvasCode
     * @param paintingData
     */
    public void updateSaleOfferPaintingData(String canvasCode, PaintingData paintingData) {
        if (!this.merchant.getTradingPlayer().getLevel().isClientSide()) {
            throw new IllegalStateException("Should not update offer painting data on server");
        }

        // If offer has changed
        if (
            this.getCurrentOffer() == null
            || !this.getCurrentOffer().getRealCanvasCode().equals(canvasCode)
            || !(this.currentOffer instanceof PaintingMerchantSaleOffer saleOffer)
        ) {
            return;
        }

        DummyCanvasData paintingWrap = ZetterCanvasTypes.DUMMY.get().createWrap(
            paintingData.getResolution(), paintingData.getWidth(), paintingData.getHeight(),
            paintingData.getColorData()
        );

        saleOffer.updatePaintingData(paintingData.getPaintingName(), paintingWrap, paintingData.getAuthorUuid(), paintingData.getAuthorName());
        saleOffer.register(this.merchant.getTradingPlayer().getLevel());
    }

    private void playTradeSound() {
        if (!this.merchant.isClientSide()) {
            Entity entity = (Entity) this.merchant;
            entity.getLevel().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), this.merchant.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
        }
    }

    @Nullable
    public IPaintingMerchantOffer getCurrentOffer() {
        return this.currentOffer;
    }

    public int getSelectedPurchaseOfferIndex() {
        return this.selectedPurchaseOfferIndex;
    }

    public void updateCurrentOffer() {
        this.setSelectedPurchaseOfferIndex(this.getSelectedPurchaseOfferIndex());
    }

    private MerchantOffer getVanillaMerchantOffer(IPaintingMerchantOffer offer) {
        if (offer instanceof PaintingMerchantSaleOffer) {
            return this.merchant.getOffers().get(ZetterGalleryVillagerTrades.SELL_OFFER_ID);
        } else {
            return this.merchant.getOffers().get(ZetterGalleryVillagerTrades.BUY_OFFER_ID);
        }
    }

    public void setSelectedPurchaseOfferIndex(int index) {
        if (!this.hasOffers()) {
            ZetterGallery.LOG.error("No offers loaded yet");
            return;
        }

        if (index >= this.purchaseOffers.size()) {
            ZetterGallery.LOG.error("There's no offer with such index");
            return;
        }

        this.selectedPurchaseOfferIndex = index;

        // This will set current offer, because this code needs to update offer's state
        // Depending on the slots
        this.setChanged();

        if (this.merchant.getTradingPlayer().level.isClientSide()) {
            CSelectOfferPacket selectOfferPacket = new CSelectOfferPacket(index);
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
