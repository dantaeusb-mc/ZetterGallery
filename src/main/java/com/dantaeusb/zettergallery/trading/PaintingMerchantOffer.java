package com.dantaeusb.zettergallery.trading;

import com.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import com.dantaeusb.zettergallery.storage.OfferPaintingData;

import java.util.UUID;

public class PaintingMerchantOffer {
    private final UUID paintingId;
    private final int price;

    private final OfferPaintingData paintingData;

    public PaintingMerchantOffer(PaintingsResponse.PaintingItem paintingItem) {
        this.paintingId = paintingItem.uuid;
        this.price = paintingItem.price;
        this.paintingData = this.createOfferDataFromItem(paintingItem);
    }

    public PaintingMerchantOffer(OfferPaintingData paintingData, int price) {
        this.paintingId = paintingData.getUniqueId();
        this.price = price;
        this.paintingData = paintingData;
    }

    public OfferPaintingData getPaintingData() {
        return this.paintingData;
    }

    public int getPrice() {
        return this.price;
    }

    /**
     * N.B. Data sent with RGBA format and stored in ARGB
     * @param paintingItem
     * @return
     */
    private OfferPaintingData createOfferDataFromItem(PaintingsResponse.PaintingItem paintingItem) {
        final OfferPaintingData offer = new OfferPaintingData(paintingItem.uuid, paintingItem.author, paintingItem.name);
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

        // @todo: resolution calculation is completely wrong for x4
        offer.initData(AbstractCanvasData.Resolution.x16, paintingItem.sizeH * resolution.getNumeric(), paintingItem.sizeW * resolution.getNumeric(), canvasData);

        return offer;
    }
}
