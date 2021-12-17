package me.dantaeusb.zettergallery.container;

import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import com.google.common.collect.Lists;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.ZetterGalleryNetwork;
import me.dantaeusb.zettergallery.core.ZetterGalleryVillagerTrades;
import me.dantaeusb.zettergallery.network.packet.SGallerySalesPacket;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ContainerListener;
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
    private final Merchant merchant;

    @Nullable
    private List<ContainerListener> listeners;
    private PaintingMerchantOffer currentOffer;

    private boolean locked = false;
    private boolean saleAllowed = false;
    private boolean canProceed = false;
    private int currentOfferIndex;

    @Nullable
    private List<PaintingMerchantOffer> offers;

    public PaintingMerchantContainer(Merchant merchant) {
        this.merchant = merchant;
    }

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
     * @link {SalesManager}
     * @param offers
     */
    public void handleOffers(boolean saleAllowed, List<PaintingMerchantOffer> offers) {
        this.saleAllowed = saleAllowed;
        this.offers = offers;
        this.updateCurrentOffer();

        if (this.merchant.isClientSide()) {
            // Maybe delegate that to some kind of ClientSalesManager?
            ICanvasTracker tracker = Helper.getWorldCanvasTracker(this.merchant.getTradingPlayer().getLevel());

            // @todo: unregister on GUI close
            for (PaintingMerchantOffer offer : offers) {
                tracker.registerCanvasData(offer.getCanvasCode(), offer.getPaintingData());
            }
        } else {
            // sync with client when updating, calls the same method on client side
            SGallerySalesPacket salesPacket = new SGallerySalesPacket(saleAllowed, offers);
            ZetterGalleryNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.merchant.getTradingPlayer()), salesPacket);
        }
    }

    public boolean isSaleAllowed() {
        return true;
        //return this.saleAllowed;
    }

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

    public boolean canProceed() {
        return this.canProceed;
    }

    public void finishSale() {
        if (this.canProceed && this.getCurrentOffer() != null) {
            PaintingMerchantOffer offer = this.getCurrentOffer();

            this.setItem(OUTPUT_SLOT, offer.getOfferResult());

            this.merchant.notifyTrade(this.getMerchantOffer(offer));
        }
    }

    public int getOffersCount() {
        if (this.hasOffers()) {
            return this.getOffers().size();
        }

        return 0;
    }

    /**
     * @todo: Use offers from villager trades
     */
    public void setChanged() {
        ItemStack inputStack = this.getInputSlot();

        if (this.hasOffers()) {
            if (inputStack.isEmpty()) {
                // Reset item if we were selling item
                this.updateCurrentOffer();

                this.canProceed = false;
            }  else {
                if (inputStack.getItem() == ZetterItems.PAINTING) {
                    // Current offer is to sell this painting, and we can proceed immediately
                    final String canvasCode = PaintingItem.getPaintingCode(inputStack);
                    PaintingData paintingData = Helper.getWorldCanvasTracker(this.merchant.getTradingPlayer().level).getCanvasData(canvasCode, PaintingData.class);
                    this.currentOffer = new PaintingMerchantOffer(canvasCode, paintingData, 4);
                    this.canProceed = true;
                } else {
                    this.updateCurrentOffer();

                    this.canProceed = inputStack.getItem() == Items.EMERALD
                            && this.currentOffer != null
                            && inputStack.getCount() >= this.currentOffer.getPrice();
                }
            }

            if (this.listeners != null) {
                for(ContainerListener containerlistener : this.listeners) {
                    containerlistener.containerChanged(this);
                }
            }
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
        }

        this.currentOfferIndex = index;
        this.currentOffer = this.offers.get(index);
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
