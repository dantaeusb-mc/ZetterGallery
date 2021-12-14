package com.dantaeusb.zettergallery.container;

import com.dantaeusb.zetter.core.ZetterItems;
import com.dantaeusb.zettergallery.core.ModNetwork;
import com.dantaeusb.zettergallery.network.packet.SGallerySalesPacket;
import com.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

public class PaintingMerchantContainer implements Container {
    public static final int STORAGE_SIZE = 2;

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);
    private final Player player;

    private boolean saleAllowed = false;
    private int currentOfferIndex;

    @Nullable
    private List<PaintingMerchantOffer> offers;

    public PaintingMerchantContainer(Player player) {
        this.player = player;
    }

    @Override
    public int getContainerSize() {
        return STORAGE_SIZE;
    }

    /**
     * Handle offers only on client, on server it's SalesManager work
     * @link {SalesManager}
     * @param offers
     */
    public void handleOffers(boolean saleAllowed, List<PaintingMerchantOffer> offers) {
        if (this.player.level.isClientSide()) {
            ICanvasTracker tracker = Helper.getWorldCanvasTracker(this.player.world);

            // @todo: unregister on GUI close
            for (PaintingMerchantOffer offer : offers) {
                tracker.registerCanvasData(offer.getPaintingData());
            }
        } else {
            // sync with client when updating, calls the same method on client side
            SGallerySalesPacket salesPacket = new SGallerySalesPacket(saleAllowed, offers);
            ModNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), salesPacket);
        }

        this.saleAllowed = saleAllowed;
        this.offers = offers;
    }

    public boolean isSaleAllowed() {
        return true;
        //return this.saleAllowed;
    }

    @Nullable
    public List<PaintingMerchantOffer> getOffers() {
        return this.offers;
    }

    public int getOffersCount() {
        if (this.getOffers() != null) {
            return this.getOffers().size();
        }

        return 0;
    }

    public void updateOfferOutput() {
        PaintingMerchantOffer currentOffer = this.getCurrentOffer();
        ItemStack inputStack;

        if (this.getInputSlot().isEmpty()) {
            this.setItem(OUTPUT_SLOT, ItemStack.EMPTY);
        }  else {
            inputStack = this.getInputSlot();

            if (inputStack.getItem() == Items.EMERALD && currentOffer != null && inputStack.getCount() >= currentOffer.getPrice()) {
                this.setItem(OUTPUT_SLOT, new ItemStack(ZetterItems.PAINTING));
            } else if (inputStack.getItem() == ZetterItems.PAINTING) {
                this.setItem(OUTPUT_SLOT, new ItemStack(Items.EMERALD, 4));
            } else {
                this.setItem(OUTPUT_SLOT, ItemStack.EMPTY);
            }
        }
    }

    @Nullable
    public PaintingMerchantOffer getCurrentOffer() {
        if (this.offers == null || this.offers.size() == 0) {
            return null;
        }

        return this.offers.get(this.currentOfferIndex);
    }

    public int getCurrentOfferIndex() {
        return this.currentOfferIndex;
    }

    public void setCurrentOfferIndex(int index) {
        this.currentOfferIndex = index;
    }

    /**
     * @todo: move all interactions to TE
     * @return
     */
    public ItemStack getInputSlot() {
        return this.getStackInSlot(INPUT_SLOT);
    }

    public ItemStack getOutputSlot() {
        return this.getStackInSlot(OUTPUT_SLOT);
    }

    @Override
    public boolean isUsableByPlayer(Player player) {
        return this.player.equals(player);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return true;
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
        if (this.player.level.isClientSide() && this.offers != null) {
            ICanvasTracker tracker = Helper.getWorldCanvasTracker(this.player.level);

            for (PaintingMerchantOffer offer : this.offers) {
                tracker.unregisterCanvasData(offer.getPaintingData());
            }
        }
    }

    @Override
    public ItemStack getItem(int index) {
        return this.itemStacks.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return this.itemStacks.get(index, count, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        int maxPossibleItemStackSize = paintingMerchantContents.getSlotLimit(index);
        return paintingMerchantContents.extractItem(index, maxPossibleItemStackSize, false);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.itemStacks.set(index, stack);
    }

    @Override
    public void clear() {
        for (int i = 0; i < paintingMerchantContents.getSlots(); ++i) {
            paintingMerchantContents.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
