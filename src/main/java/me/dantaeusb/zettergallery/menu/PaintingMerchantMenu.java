package me.dantaeusb.zettergallery.menu;

import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.container.PaintingMerchantContainer;
import me.dantaeusb.zettergallery.core.ZetterGalleryContainerMenus;
import me.dantaeusb.zettergallery.gallery.ConnectionManager;
import me.dantaeusb.zettergallery.menu.paintingmerchant.MerchantAuthorizationController;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import me.dantaeusb.zettergallery.trading.PaintingMerchantPurchaseOffer;
import net.minecraft.entity.NPCMerchant;
import net.minecraft.entity.merchant.IMerchant;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public class PaintingMerchantMenu extends Container implements ItemStackHandlerListener {
    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    public static final int PLAYER_INVENTORY_XPOS = 24;
    public static final int PLAYER_INVENTORY_YPOS = 154;

    private final PlayerEntity player;
    private final IMerchant merchant;

    private final MerchantAuthorizationController authorizationController;
    private final PaintingMerchantContainer container;

    private UUID merchantId;
    private int merchantLevel;

    private PaintingMerchantMenu(int windowID, PlayerInventory invPlayer, IMerchant merchant) {
        super(ZetterGalleryContainerMenus.PAINTING_MERCHANT.get(), windowID);

        this.player = invPlayer.player;
        this.merchant = merchant;
        this.container = new PaintingMerchantContainer(invPlayer.player, merchant, this);
        this.container.addListener(this);

        this.authorizationController = new MerchantAuthorizationController(invPlayer.player, this);

        // gui position of the player material slots
        final int INPUT_XPOS = 119;
        final int INPUT_YPOS = 119;

        // gui position of the player material slots
        final int OUTPUT_XPOS = 180;
        final int OUTPUT_YPOS = 119;

        this.addSlot(new PaintingMerchantMenu.SlotInput(this.container, 0, INPUT_XPOS, INPUT_YPOS));
        this.addSlot(new PaintingMerchantMenu.SlotOutput(this.container, 1, OUTPUT_XPOS, OUTPUT_YPOS));

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 24;
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

    public static PaintingMerchantMenu createMenuServerSide(int windowID, PlayerInventory playerInventory, IMerchant merchant) {
        return new PaintingMerchantMenu(windowID, playerInventory, merchant);
    }

    public static PaintingMerchantMenu createMenuClientSide(int windowID, PlayerInventory playerInventory, net.minecraft.network.PacketBuffer networkBuffer) {
        IMerchant merchant = new NPCMerchant(playerInventory.player);

        return new PaintingMerchantMenu(windowID, playerInventory, merchant);
    }

    public PlayerEntity getPlayer() {
        return this.player;
    }

    public IMerchant getMerchant() {
        return this.merchant;
    }

    public PaintingMerchantContainer getContainer() {
        return this.container;
    }

    public MerchantAuthorizationController getAuthController() {
        return this.authorizationController;
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

    @Override
    public void containerChanged(ItemStackHandler container, int slot) {

    }

    public void purchase(PlayerEntity player, ItemStack stack) {
        this.container.checkout(stack);
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean stillValid(PlayerEntity player) {
        //return this.merchant.getCustomer() == player;
        return this.container.stillValid(player);
    }

    /**
     * @param playerIn
     * @param sourceSlotIndex
     * @return
     */
    @Override
    public ItemStack quickMoveStack(PlayerEntity playerIn, int sourceSlotIndex) {
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
                if (sourceStack.getItem() == ZetterItems.PALETTE.get()) {
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

    /**
     * Pass calls to storage
     */

    public boolean hasOffers() {
        return this.container.hasOffers();
    }

    public int getOffersCount() {
        return this.container.getOffersCount();
    }

    @Nullable
    public PaintingMerchantOffer getCurrentOffer() {
        return this.container.getCurrentOffer();
    }

    public void updateCurrentOfferIndex(int index) {
        this.container.setSelectedPurchaseOfferIndex(index);

        if (!this.merchant.getLevel().isClientSide()) {
            PaintingMerchantPurchaseOffer currentPurchaseOffer = this.container.getCurrentPurchaseOffer();
            assert currentPurchaseOffer != null;

            ConnectionManager.getInstance().registerImpression((ServerPlayerEntity) this.player, currentPurchaseOffer.getPaintingUuid(),
                currentPurchaseOffer.getCycleIncrementId(), () -> {
                }, () -> {
                    ZetterGallery.LOG.error("Unable to register impression, maybe outdated mod version?");
                });
        }
    }

    public int getCurrentOfferIndex() {
        return this.container.getSelectedPurchaseOfferIndex();
    }

    /*
     * Event handling
     */

    /**
     * Handle the change of the current offers status,
     * used to sync status when server asks to validate
     * painting for sale
     *
     * @param canvasCode
     * @param state
     * @param message
     */
    public void handleOfferState(String canvasCode, PaintingMerchantPurchaseOffer.State state, String message) {
        // If canvas code of the offer has changed since packet was formed, disregard it
        if (this.getCurrentOffer() == null || !this.getCurrentOffer().getDummyCanvasCode().equals(canvasCode)) {
            return;
        }

        if (state == PaintingMerchantPurchaseOffer.State.ERROR) {
            this.getCurrentOffer().markError(new GalleryError(GalleryError.CLIENT_INVALID_OFFER, message));
        } else if (state == PaintingMerchantPurchaseOffer.State.READY) {
            this.getCurrentOffer().ready();
        }

        this.container.setChanged();
    }

    /*
     * Helpers
     */

    /**
     * Called when the container is closed.
     */
    public void removed(PlayerEntity player) {
        super.removed(player);

        if (!this.merchant.getLevel().isClientSide()) {
            if (!player.isAlive() || player instanceof ServerPlayerEntity && ((ServerPlayerEntity) player).hasDisconnected()) {
                ItemStack itemstack = this.container.removeItemNoUpdate(PaintingMerchantContainer.INPUT_SLOT);
                if (!itemstack.isEmpty()) {
                    player.drop(itemstack, false);
                }

                itemstack = this.container.removeItemNoUpdate(PaintingMerchantContainer.OUTPUT_SLOT);
                if (!itemstack.isEmpty()) {
                    player.drop(itemstack, false);
                }
            } else {
                player.inventory.placeItemBackInInventory(this.getMerchant().getLevel(), this.container.removeItemNoUpdate(0));
            }
        }

        this.container.removed();
        this.merchant.setTradingPlayer(null);
    }

    private boolean areItemStacksEqual(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem() && ItemStack.isSame(stack1, stack2);
    }

    public class SlotInput extends Slot {
        public SlotInput(IInventory inventoryIn, int index, int xPosition, int yPosition) {
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

            if (stack.getItem() == ZetterItems.PAINTING.get() && PaintingMerchantMenu.this.getAuthController().canSell(stack)) {
                return true;
            }

            return false;
        }

        @Override
        public boolean mayPickup(PlayerEntity player) {
            return PaintingMerchantMenu.this.container.canTakeItem(PaintingMerchantContainer.INPUT_SLOT);
        }
    }

    public class SlotOutput extends Slot {
        public SlotOutput(IInventory inventoryIn, int index, int xPosition, int yPosition) {
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
        public ItemStack onTake(PlayerEntity player, ItemStack stack) {
            super.onTake(player, stack);
            PaintingMerchantMenu.this.purchase(player, stack);
            return stack;
        }
    }
}
