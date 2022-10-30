package me.dantaeusb.zettergallery.trading;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PaintingMerchantOffer {
    private final String canvasCode;
    private final int price;
    private final boolean saleOffer;

    private PaintingData paintingData;

    /**
     * If we're ready to make a transaction
     */
    private State state;

    /**
     * Describe error or action
     */
    private String message;

    private PaintingMerchantOffer(String canvasCode, @Nullable PaintingData paintingData, int price, boolean sale) {
        this.canvasCode = canvasCode;
        this.paintingData = paintingData;
        this.price = price;
        this.saleOffer = sale;

        if (paintingData == null) {
            this.state = State.PENDING;
        } else {
            this.state = sale ? State.WAITING : State.UNFULFILLED;
        }
    }

    public static PaintingMerchantOffer createOfferFromResponse(PaintingsResponse.PaintingItem paintingItem) {
        return new PaintingMerchantOffer(
                GalleryPaintingData.getCanvasCode(paintingItem.uuid),
                PaintingMerchantOffer.createOfferDataFromItem(paintingItem),
                paintingItem.price,
                false
        );
    }

    public static PaintingMerchantOffer createOfferFromPlayersPainting(String canvasCode, PaintingData paintingData, int price) {
        return new PaintingMerchantOffer(
                canvasCode,
                paintingData,
                price,
                true
        );
    }

    public static PaintingMerchantOffer createOfferFromPaintingData(GalleryPaintingData paintingData, int price) {
        return new PaintingMerchantOffer(
                GalleryPaintingData.getCanvasCode(paintingData.getUUID()),
                paintingData,
                price,
                false
        );
    }

    public void updatePaintingData(PaintingData paintingData) {
        if (this.paintingData != null) {
            Zetter.LOG.error("Trying to update offer data which already has data");
            return;
        }

        this.paintingData = paintingData;
    }

    public Optional<PaintingData> getPaintingData() {
        return this.paintingData == null ? Optional.empty() : Optional.of(this.paintingData);
    }

    public String getCanvasCode() {
        return this.canvasCode;
    }

    public boolean isSaleOffer() {
        return this.saleOffer;
    }

    public int getPrice() {
        return this.price;
    }

    public Optional<String> getMessage() {
        return this.message == null ? Optional.empty() : Optional.of(this.message);
    }

    public ItemStack getOfferResult(Level level) {
        if (this.saleOffer) {
            return new ItemStack(Items.EMERALD, this.price);
        } else {
            ItemStack painting = new ItemStack(ZetterItems.PAINTING.get());

            ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(level);
            canvasTracker.registerCanvasData(this.canvasCode, this.paintingData);
            // @todo: this spawns event that will replace offer

            PaintingItem.storePaintingData(painting, this.canvasCode, this.paintingData, 1);

            return painting;
        }
    }

    public boolean isLoading() {
        return this.state == State.PENDING || this.state == State.WAITING;
    }

    public boolean isReady() {
        return this.state == State.READY;
    }

    public boolean isError() {
        return this.state == State.ERROR;
    }

    public void unfulfilled() {
        this.state = State.UNFULFILLED;
    }

    public void ready() {
        this.state = State.READY;
    }

    public void markError(GalleryError error) {
        this.state = State.ERROR;
        this.message = error.getClientMessage();
    }


    /**
     * N.B. Data sent with RGBA format and stored in ARGB
     * @param paintingItem
     * @return
     */
    private static GalleryPaintingData createOfferDataFromItem(PaintingsResponse.PaintingItem paintingItem) {
        final AbstractCanvasData.Resolution resolution = AbstractCanvasData.Resolution.x16;
        final int paintingSize = (paintingItem.sizeH * resolution.getNumeric()) * (paintingItem.sizeW * resolution.getNumeric());
        byte[] canvasData = new byte[paintingSize * 4];

        for (int i = 0; i < paintingSize; i++) {
            canvasData[i * 4] = (byte) 0xFF;
            canvasData[i * 4 + 1] = paintingItem.color[i * 4];
            canvasData[i * 4 + 2] = paintingItem.color[i * 4 + 1];
            canvasData[i * 4 + 3] = paintingItem.color[i * 4 + 2];
            // Skip alpha, it should not be used anyway
        }

        return GalleryPaintingData.create(paintingItem.uuid, paintingItem.author.nickname, paintingItem.name, resolution, paintingItem.sizeW * resolution.getNumeric(), paintingItem.sizeH * resolution.getNumeric(), canvasData);
    }

    public enum State {
        PENDING("pending"), // Painting data not ready
        WAITING("waiting"), // Painting waiting for validation / no data received
        UNFULFILLED("unfulfilled"), //
        READY("ready"), // Ready for checkout
        ERROR("error"); // Error occurred, checkout not possible

        private final String value;

        State(String value) {
            this.value = value;
        }

        public static State fromValue(String value) {
            if (value != null) {
                for (State state : values()) {
                    if (state.value.equals(value)) {
                        return state;
                    }
                }
            }

            return getDefault();
        }

        public String toValue() {
            return value;
        }

        public static State getDefault() {
            return PENDING;
        }
    }
}
