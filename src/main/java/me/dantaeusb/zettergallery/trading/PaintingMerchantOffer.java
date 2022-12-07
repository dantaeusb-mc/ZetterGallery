package me.dantaeusb.zettergallery.trading;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.core.ZetterGalleryCanvasTypes;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.UUID;

public class PaintingMerchantOffer {
    public final String canvasCode;
    private DummyCanvasData paintingDataCopy;

    public final UUID paintingUuid;
    public final String paintingName;
    public final UUID paintingAuthorUuid;
    public final String paintingAuthorName;

    public final int price;
    public final boolean saleOffer;

    /**
     * If we're ready to make a transaction
     */
    private State state;

    /**
     * Describe error or action
     */
    private String message;

    private String feedName;

    private PaintingMerchantOffer(
        String canvasCode, @Nullable DummyCanvasData paintingData,
        UUID paintingUuid, String paintingTitle, UUID paintingAuthorUuid, String paintingAuthorName,
        int price, boolean sale
    ) {
        this.canvasCode = canvasCode;
        this.paintingDataCopy = paintingData;

        this.paintingUuid = paintingUuid;
        this.paintingName = paintingTitle;
        this.paintingAuthorUuid = paintingAuthorUuid;
        this.paintingAuthorName = paintingAuthorName;

        this.price = price;
        this.saleOffer = sale;

        if (paintingData == null) {
            this.state = State.PENDING;
        } else {
            this.state = sale ? State.WAITING : State.UNFULFILLED;
        }
    }

    /**
     * Creates painting merchant offer from Zetter Gallery HTTP response
     * @param paintingItem
     * @return
     */
    public static PaintingMerchantOffer createOfferFromGalleryResponse(PaintingsResponse.PaintingItem paintingItem) {
        return new PaintingMerchantOffer(
            GalleryPaintingData.getDummyOfferCanvasCode(paintingItem.uuid),
            PaintingMerchantOffer.createDummyCanvasDataFromItem(paintingItem),
            paintingItem.uuid,
            paintingItem.name,
            paintingItem.author.uuid,
            paintingItem.author.nickname,
            paintingItem.price,
            false
        );
    }

    /**
     * Creates painting merchant offer from player's painting
     * @param canvasCode
     * @param paintingData
     * @param price
     * @return
     */
    public static PaintingMerchantOffer createOfferFromPlayersPainting(String canvasCode, PaintingData paintingData, int price) {
        DummyCanvasData paintingWrap = ZetterCanvasTypes.DUMMY.get().createWrap(
            paintingData.getResolution(), paintingData.getWidth(), paintingData.getHeight(),
            paintingData.getColorData()
        );

        return new PaintingMerchantOffer(
            canvasCode,
            paintingWrap,
            new UUID(0L, 0L),
            paintingData.getPaintingName(),
            paintingData.getAuthorUuid(),
            paintingData.getAuthorName(),
            price,
            true
        );
    }

    /**
     * Creates painting merchant offer from
     * @return
     */
    public static PaintingMerchantOffer createOfferFromNetwork(
        DummyCanvasData canvasData, UUID paintingUuid, String paintingTitle,
        UUID paintingAuthorUuid, String paintingAuthorName, int price
    ) {

        return new PaintingMerchantOffer(
            GalleryPaintingData.getDummyOfferCanvasCode(paintingUuid),
            canvasData,
            paintingUuid,
            paintingTitle,
            paintingAuthorUuid,
            paintingAuthorName,
            price,
            false
        );
    }

    public void updatePaintingData(DummyCanvasData paintingData) {
        if (this.paintingDataCopy != null) {
            Zetter.LOG.error("Trying to update offer data which already has data");
            return;
        }

        this.paintingDataCopy = paintingData;
    }

    public Optional<DummyCanvasData> getPaintingData() {
        return this.paintingDataCopy == null ? Optional.empty() : Optional.of(this.paintingDataCopy);
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

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getFeedName() {
        return this.feedName;
    }

    public Optional<String> getMessage() {
        return this.message == null ? Optional.empty() : Optional.of(this.message);
    }

    public ItemStack getOfferResult() {
        if (this.saleOffer) {
            return new ItemStack(Items.EMERALD, this.price);
        } else {
            ItemStack painting = new ItemStack(ZetterItems.PAINTING.get());

            return painting;
        }
    }

    public void writeOfferResultData(Level level, ItemStack painting) {
        if (!painting.is(ZetterItems.PAINTING.get())) {
            throw new IllegalStateException("Can only write data to painting");
        }

        String galleryPaintingCanvasCode = GalleryPaintingData.getCanvasCode(this.paintingUuid);
        GalleryPaintingData galleryPaintingData = ZetterGalleryCanvasTypes.GALLERY_PAINTING.get().createWrap(
            this.paintingDataCopy.getResolution(),
            this.paintingDataCopy.getWidth(),
            this.paintingDataCopy.getHeight(),
            this.paintingDataCopy.getColorData()
        );

        galleryPaintingData.setMetaProperties(
            this.paintingUuid,
            this.paintingAuthorUuid,
            this.paintingAuthorName,
            this.paintingName
        );

        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(level);
        canvasTracker.registerCanvasData(galleryPaintingCanvasCode, galleryPaintingData);

        PaintingItem.storePaintingData(painting, galleryPaintingCanvasCode, galleryPaintingData, 1);
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
    private static DummyCanvasData createDummyCanvasDataFromItem(PaintingsResponse.PaintingItem paintingItem) {
        final int numericResolution = paintingItem.resolution * Helper.getBasicResolution().getNumeric();
        final AbstractCanvasData.Resolution resolution = AbstractCanvasData.Resolution.get(numericResolution);

        if (resolution == null) {
            throw new InvalidParameterException("Invalid resolution returned by Gallery: " + numericResolution);
        }

        final int paintingSize = (paintingItem.sizeH * resolution.getNumeric()) * (paintingItem.sizeW * resolution.getNumeric());
        byte[] canvasData = new byte[paintingSize * 4];

        for (int i = 0; i < paintingSize; i++) {
            canvasData[i * 4] = (byte) 0xFF;
            canvasData[i * 4 + 1] = paintingItem.color[i * 4];
            canvasData[i * 4 + 2] = paintingItem.color[i * 4 + 1];
            canvasData[i * 4 + 3] = paintingItem.color[i * 4 + 2];
            // Skip alpha, it should not be used anyway
        }

        DummyCanvasData paintingData = ZetterCanvasTypes.DUMMY.get().createWrap(
            resolution, paintingItem.sizeW * resolution.getNumeric(), paintingItem.sizeH * resolution.getNumeric(), canvasData
        );

        return paintingData;
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
