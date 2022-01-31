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
    private boolean loading;

    /**
     * Describe error or action
     */
    private String message;

    public PaintingMerchantOffer(PaintingsResponse.PaintingItem paintingItem) {
        this.canvasCode = GalleryPaintingData.getCanvasCode(paintingItem.uuid);
        this.price = paintingItem.price;
        this.saleOffer = false;

        this.paintingData = this.createOfferDataFromItem(paintingItem);
    }

    public PaintingMerchantOffer(GalleryPaintingData paintingData, int price) {
        this.canvasCode = GalleryPaintingData.getCanvasCode(paintingData.getUUID());
        this.price = price;
        this.saleOffer = false;

        this.paintingData = paintingData;
    }

    public PaintingMerchantOffer(String canvasCode, PaintingData paintingData, int price) {
        this.canvasCode = canvasCode;
        this.price = price;
        this.saleOffer = true;

        this.paintingData = paintingData;
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

    /**
     * N.B. Data sent with RGBA format and stored in ARGB
     * @param paintingItem
     * @return
     */
    private GalleryPaintingData createOfferDataFromItem(PaintingsResponse.PaintingItem paintingItem) {
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
}
