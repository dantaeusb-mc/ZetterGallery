package me.dantaeusb.zettergallery.trading;

import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zettergallery.core.ZetterGalleryCanvasTypes;
import me.dantaeusb.zettergallery.network.http.dto.PaintingsResponse;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.UUID;

public class PaintingMerchantPurchaseOffer extends PaintingMerchantAbstractOffer {
    private final String realCanvasCode;

    private final String dummyCanvasCode;
    private final DummyCanvasData dummyPaintingData;

    private final UUID paintingUuid;
    private final String paintingName;
    private final UUID paintingAuthorUuid;
    private final String paintingAuthorName;

    private int cycleIncrementId;
    private String feedName;

    /**
     * Offers sometimes might be created without painting data, but they need to know which canvas code to
     * use to request it. Typically, happens for sale offers.
     *
     * @param canvasCode
     * @param paintingData
     * @param paintingUuid
     * @param paintingTitle
     * @param paintingAuthorUuid
     * @param paintingAuthorName
     * @param price
     */
    private PaintingMerchantPurchaseOffer(
        String canvasCode, DummyCanvasData paintingData,
        UUID paintingUuid, String paintingTitle, UUID paintingAuthorUuid, String paintingAuthorName, int price
    ) {
        super(price, State.UNFULFILLED);

        this.realCanvasCode = GalleryPaintingData.getCanvasCode(paintingUuid);

        this.dummyCanvasCode = canvasCode;
        this.dummyPaintingData = paintingData;

        this.paintingUuid = paintingUuid;
        this.paintingName = paintingTitle;
        this.paintingAuthorUuid = paintingAuthorUuid;
        this.paintingAuthorName = paintingAuthorName;
    }

    /**
     * Creates painting merchant offer from Zetter Gallery HTTP response
     * @param paintingItem
     * @return
     */
    public static PaintingMerchantPurchaseOffer createOfferFromGalleryResponse(PaintingsResponse.PaintingItem paintingItem) {
        return new PaintingMerchantPurchaseOffer(
            GalleryPaintingData.getDummyOfferCanvasCode(paintingItem.uuid),
            PaintingMerchantPurchaseOffer.createDummyCanvasDataFromItem(paintingItem),
            paintingItem.uuid,
            paintingItem.name,
            paintingItem.author.uuid,
            paintingItem.author.nickname,
            paintingItem.price
        );
    }

    /**
     * Creates painting merchant offer from
     * @return
     */
    public static PaintingMerchantPurchaseOffer createOfferFromNetwork(
        DummyCanvasData canvasData, UUID paintingUuid, String paintingTitle,
        UUID paintingAuthorUuid, String paintingAuthorName, int price
    ) {
        return new PaintingMerchantPurchaseOffer(
            GalleryPaintingData.getDummyOfferCanvasCode(paintingUuid),
            canvasData,
            paintingUuid,
            paintingTitle,
            paintingAuthorUuid,
            paintingAuthorName,
            price
        );
    }

    public UUID getPaintingUuid() {
        return this.paintingUuid;
    }

    @Override
    public String getRealCanvasCode() {
        return this.realCanvasCode;
    }

    @Override
    public String getDummyCanvasCode() {
        return this.dummyCanvasCode;
    }

    @Override
    public DummyCanvasData getDummyPaintingData() {
        return this.dummyPaintingData;
    }

    @Override
    public String getPaintingName() {
        return this.paintingName;
    }

    @Override
    public UUID getAuthorUuid() {
        return this.paintingAuthorUuid;
    }

    @Override
    public String getAuthorName() {
        return this.paintingAuthorName;
    }

    public void setCycleInfo(int cycleIncrementId, String feedName) {
        this.cycleIncrementId = cycleIncrementId;
        this.feedName = feedName;
    }

    public String getFeedName() {
        return this.feedName;
    }

    public int getCycleIncrementId() {
        return this.cycleIncrementId;
    }

    public Optional<String> getMessage() {
        return this.message == null ? Optional.empty() : Optional.of(this.message);
    }

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
    public ItemStack getOfferResult() {
        if (this.isReady()) {
            return new ItemStack(ZetterItems.PAINTING.get(), 1);
        } else {
            return ItemStack.EMPTY;
        }
    }

    public void writeOfferResultData(Level level, ItemStack painting) {
        if (!painting.is(ZetterItems.PAINTING.get())) {
            throw new IllegalStateException("Can only write data to painting");
        }

        GalleryPaintingData galleryPaintingData = ZetterGalleryCanvasTypes.GALLERY_PAINTING.get().createWrap(
            this.dummyPaintingData.getResolution(),
            this.dummyPaintingData.getWidth(),
            this.dummyPaintingData.getHeight(),
            this.dummyPaintingData.getColorData()
        );

        galleryPaintingData.setMetaProperties(
            this.paintingUuid,
            this.paintingAuthorUuid,
            this.paintingAuthorName,
            this.paintingName
        );

        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(level);
        canvasTracker.registerCanvasData(this.realCanvasCode, galleryPaintingData);

        PaintingItem.storePaintingData(painting, this.realCanvasCode, galleryPaintingData, 1);
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

        return ZetterCanvasTypes.DUMMY.get().createWrap(
            resolution, paintingItem.sizeW * resolution.getNumeric(), paintingItem.sizeH * resolution.getNumeric(), canvasData
        );
    }
}
