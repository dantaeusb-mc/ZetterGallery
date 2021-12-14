package com.dantaeusb.zettergallery.menu;

import com.dantaeusb.zetter.core.ZetterItems;
import com.dantaeusb.zetter.item.PaintingItem;
import com.dantaeusb.zetter.storage.PaintingData;
import com.dantaeusb.zettergallery.container.PaintingMerchantContainer;
import com.dantaeusb.zettergallery.core.Helper;
import com.dantaeusb.zettergallery.core.ModContainers;
import com.dantaeusb.zettergallery.core.ModItems;
import com.dantaeusb.zettergallery.core.ModNetwork;
import com.dantaeusb.zettergallery.gallery.SalesManager;
import com.dantaeusb.zettergallery.network.http.GalleryConnection;
import com.dantaeusb.zettergallery.network.packet.CGalleryAuthorizationCheckPacket;
import com.dantaeusb.zettergallery.network.packet.CGalleryOffersRequestPacket;
import com.dantaeusb.zettergallery.storage.OfferPaintingData;
import com.dantaeusb.zettergallery.tileentity.PaintingMerchantTileEntity;
import com.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class PaintingMerchantMenu extends AbstractContainerMenu {
    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    public static final int PLAYER_INVENTORY_XPOS = 8;
    public static final int PLAYER_INVENTORY_YPOS = 154;

    private final ContainerLevelAccess worldPosCallable;

    private final Player player;
    private final Level world;

    private final PaintingMerchantContainer container;

    private State state = State.SERVER_AUTHENTICATION;
    @Nullable
    private String crossAuthorizationCode;

    @Nullable
    private String error;

    public PaintingMerchantMenu(int windowID, Inventory invPlayer,
                                PaintingMerchantContainer container,
                                final ContainerLevelAccess worldPosCallable) {
        super(ModContainers.PAINTING_MERCHANT, windowID);

        this.container = container;
        this.worldPosCallable = worldPosCallable;

        this.player = invPlayer.player;
        this.world = invPlayer.player.level;

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
                this.addSlot(new Slot(invPlayer, slotNumber,  xpos, ypos));
            }
        }
    }

    public static PaintingMerchantMenu createContainerServerSide(int windowID, Inventory playerInventory,
                                                                 PaintingMerchantTileEntity tileEntity,
                                                                 final ContainerLevelAccess worldPosCallable) {
        // @todo: this should happen only if the storage is not ready (all caches after release)
        // request token from Zetter Gallery
        GalleryConnection.getInstance().authorizeServerPlayer((ServerPlayer) playerInventory.player);

        PaintingMerchantContainer storage = SalesManager.getInstance().getStorage((ServerPlayer) playerInventory.player, tileEntity);

        return new PaintingMerchantMenu(windowID, playerInventory, storage, worldPosCallable);
    }

    public static PaintingMerchantMenu createContainerClientSide(int windowID, Inventory playerInventory,
                                                                 FriendlyByteBuf networkBuffer) {
        PaintingMerchantContainer canvasStorage = PaintingMerchantStorage.createForClientSideContainer(playerInventory.player);

        return new PaintingMerchantMenu(windowID, playerInventory, canvasStorage, ContainerLevelAccess.DUMMY);
    }

    /**
     * Callback for when the crafting matrix is changed.
     * @todo: no info set on server
     */
    @Override
    public void slotsChanged(Container container) {
        //this.storage.resetRecipeAndSlots(this.ready(), this.getCurrentOffer());
        super.slotsChanged(container);
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean stillValid(Player player) {
        //return this.merchant.getCustomer() == player;
        return this.container.isUsableByPlayer(player);
    }

    /**
     *
     * @param playerIn
     * @param sourceSlotIndex
     * @return
     */
    @Override
    public ItemStack quickMoveStack(Player playerIn, int sourceSlotIndex)
    {
        ItemStack outStack = ItemStack.EMPTY;
        Slot sourceSlot = this.inventorySlots.get(sourceSlotIndex);

        if (sourceSlot != null && sourceSlot.hasItem()) {
            ItemStack sourceStack = sourceSlot.getStack();
            outStack = sourceStack.copy();

            // Palette
            if (sourceSlotIndex == 0) {
                if (!this.mergeItemStack(sourceStack, 2, 10, true)) {
                    return ItemStack.EMPTY;
                }

                sourceSlot.onSlotChange(sourceStack, outStack);

            // Inventory
            } else {
                if (sourceStack.getItem() == ZetterItems.PALETTE) {
                    if (!this.mergeItemStack(sourceStack, 0, 1, false)) {
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

    @Nullable
    public List<PaintingMerchantOffer> getOffers() {
        return this.container.getOffers();
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

    public boolean ready() {
        return this.getOffers() != null;
    }

    public boolean isSaleAllowed() {
        return this.container.isSaleAllowed();
    }


    public State getState() {
        return this.state;
    }

    public @Nullable String getError() {
        return this.error;
    }

    /**
     * Say to server that player is back in game after they left
     * for authorization link. This should make server to check
     * authorization for player token again, and will send
     * player's rights back if everything's alright
     */
    public void updateAuthentication() {
        CGalleryAuthorizationCheckPacket authenticationCheckPacket = new CGalleryAuthorizationCheckPacket();
        ModNetwork.simpleChannel.sendToServer(authenticationCheckPacket);

        this.state = this.state.success();
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

    public void updateLoadingState() {
        if (this.state == State.FETCHING_SALES) {
            CGalleryOffersRequestPacket offersRequestPacket = new CGalleryOffersRequestPacket();
            ModNetwork.simpleChannel.sendToServer(offersRequestPacket);
        }
    }

    public void handleAuthorization(boolean canBuy, boolean canSell) {
        if (this.state == State.SERVER_AUTHENTICATION) {


            this.state = this.state.success();
        }

        this.updateLoadingState();
    }

    public void handleAuthorizationRequest(String token) {
        if (this.state == State.SERVER_AUTHENTICATION) {
            this.crossAuthorizationCode = token;
            this.state = this.state.fail();
        }

        // @todo: or throw wrong state error
        this.updateLoadingState();
    }

    public void handleOffers(boolean sellAllowed, List<PaintingMerchantOffer> offers) {
        this.container.handleOffers(sellAllowed, offers);
    }

    public void handleStorageUpdate() {
        if (this.state == State.FETCHING_SALES) {
            this.state = this.state.success();
        }

        this.updateLoadingState();
    }

    public void handleError(String error) {
        this.state = this.state.error();
        this.error = error;
    }

    /*private void playMerchantYesSound() {
        if (!this.merchant.getWorld().isRemote) {
            Entity entity = (Entity)this.merchant;
            this.merchant.getWorld().playSound(entity.getPosX(), entity.getPosY(), entity.getPosZ(), this.merchant.getYesSound(), SoundCategory.NEUTRAL, 1.0F, 1.0F, false);
        }
    }*/

    /**
     * Called when the container is closed.
     */
    public void removed(Player player) {
        super.removed(player);

        this.container.stopOpen();

        if (!this.world.isClientSide()) {
            if (!player.isAlive() || player instanceof ServerPlayer && ((ServerPlayer)player).hasDisconnected()) {
                ItemStack itemstack = this.container.removeStackFromSlot(0);
                if (!itemstack.isEmpty()) {
                    player.drop(itemstack, false);
                }

                itemstack = this.container.removeStackFromSlot(1);
                if (!itemstack.isEmpty()) {
                    player.drop(itemstack, false);
                }
            } else {
                player.getInventory().placeItemBackInInventory(this.container.removeStackFromSlot(0));
            }
        }
    }

    protected ItemStack takeOutput(Player player, ItemStack outStack) {
        if (outStack.getItem() == ZetterItems.PAINTING) {
            OfferPaintingData offerPaintingData = this.getCurrentOffer().getPaintingData();
            PaintingData paintingData = Helper.createPaintingFromOffer(world, offerPaintingData);

            PaintingItem.setPaintingData(outStack, paintingData);
            PaintingItem.setBlockSize(
                    outStack,
                    new int[]{
                            paintingData.getWidth() / paintingData.getResolution().getNumeric(),
                            paintingData.getHeight() / paintingData.getResolution().getNumeric()
                    }
            );

            if (!player.isCreative()) {
                //this.inventorySlots.get(0);
            }

            if (!this.world.isClientSide()) {
                GalleryConnection.getInstance().purchase((ServerPlayer) player, offerPaintingData.getUniqueId());
            }
        } else if (outStack.getItem() == Items.EMERALD) {
            if (!this.world.isClientSide()) {
                final String paintingCode = PaintingItem.getPaintingCode(this.container.getInputSlot());
                PaintingData paintingData = Helper.getWorldCanvasTracker(this.world).getCanvasData(paintingCode, PaintingData.class);

                GalleryConnection.getInstance().sell((ServerPlayer) player, paintingData);
            }
        }

        return outStack;
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
            if (!PaintingMerchantMenu.this.ready()) {
                return false;
            }

            if (stack.getItem() == Items.EMERALD && PaintingMerchantMenu.this.ready()) {
                return true;
            }

            if (stack.getItem() == ZetterItems.PAINTING && PaintingMerchantMenu.this.isSaleAllowed()) {
                return true;
            }

            return false;
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
         * @param player
         * @param stack
         */
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            PaintingMerchantMenu.this.takeOutput(player, stack);
        }
    }

    public enum State {
        SERVER_AUTHENTICATION {
            @Override
            public State success() {
                return FETCHING_SALES;
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
        FETCHING_SALES {
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
        };
    }
}
