package me.dantaeusb.zettergallery.trading;

import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.FrameItem;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PaintingMerchantOffer {
    private final String canvasCode;
    private final int price;
    private final boolean saleOffer;

    private final PaintingData paintingData;

    /**
     * If we're ready to make a transaction
     */
    private State state;

    /**
     * Describe error or action
     */
    private String message;

    private PaintingMerchantOffer(String canvasCode, PaintingData paintingData, int price, boolean sale) {
        this.canvasCode = canvasCode;
        this.paintingData = paintingData;
        this.price = price;
        this.saleOffer = sale;
        this.state = sale ? State.WAITING : State.READY;
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

    public PaintingData getPaintingData() {
        return this.paintingData;
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

    public ItemStack getOfferResult() {
        if (this.saleOffer) {
            return new ItemStack(Items.EMERALD, this.price);
        } else {
            ItemStack painting = new ItemStack(ZetterItems.PAINTING);

            // @todo: use PaintingItem
            FrameItem.setPaintingData(painting, this.canvasCode, this.paintingData, 1);
            FrameItem.setBlockSize(
                    painting,
                    new int[]{
                            paintingData.getWidth() / paintingData.getResolution().getNumeric(),
                            paintingData.getHeight() / paintingData.getResolution().getNumeric()
                    }
            );

            return painting;
        }
    }

    public boolean isReady() {
        return this.state == State.READY;
    }

    public boolean isError() {
        return this.state == State.ERROR;
    }

    public void markReady() {
        this.state = State.READY;
    }

    public void markError(String error) {
        this.state = State.ERROR;
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

        return GalleryPaintingData.create(paintingItem.uuid, paintingItem.author.nickname, paintingItem.name, resolution, paintingItem.sizeH * resolution.getNumeric(), paintingItem.sizeW * resolution.getNumeric(), canvasData);
    }

    public enum State {
        WAITING,
        READY,
        ERROR,
    }
}
