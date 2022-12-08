package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.ClientHandler;
import me.dantaeusb.zettergallery.network.http.stub.PaintingsResponse;
import me.dantaeusb.zettergallery.trading.PaintingMerchantPurchaseOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

/**
 * @todo: Is that okay that we don't have classic handler here?
 */
public class SGalleryOffersPacket {
    static final int MAX_NAME_LENGTH = 128;
    static final int MAX_AUTHOR_LENGTH = 64;

    private final PaintingsResponse.CycleInfo cycleInfo;
    private final List<PaintingMerchantPurchaseOffer> offers;

    public SGalleryOffersPacket(PaintingsResponse.CycleInfo cycleInfo, List<PaintingMerchantPurchaseOffer> offers) {
        this.cycleInfo = cycleInfo;
        this.offers = offers;
    }

    public PaintingsResponse.CycleInfo getCycleInfo() {
        return this.cycleInfo;
    }

    public List<PaintingMerchantPurchaseOffer> getOffers() {
        return this.offers;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SGalleryOffersPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            final int cycleIncrementId = networkBuffer.readInt();
            final Date cycleStartsAt = networkBuffer.readDate();
            final Date cycleEndsAt = networkBuffer.readDate();
            final String cycleSeed = networkBuffer.readUtf(16);

            final PaintingsResponse.CycleInfo cycleInfo = new PaintingsResponse.CycleInfo(
                cycleIncrementId, cycleSeed, cycleStartsAt, cycleEndsAt
            );

            final int size = networkBuffer.readInt();
            int i = 0;

            Vector<PaintingMerchantPurchaseOffer> offers = new Vector<>();

            while (i < size) {
                final UUID paintingGalleryUuid = networkBuffer.readUUID();
                final String paintingTitle = networkBuffer.readUtf(MAX_NAME_LENGTH);
                final UUID paintingAuthorUuid = networkBuffer.readUUID();
                final String paintingAuthorName = networkBuffer.readUtf(MAX_AUTHOR_LENGTH);
                final AbstractCanvasData.Resolution resolution = AbstractCanvasData.Resolution.get(networkBuffer.readInt());
                final int sizeH = networkBuffer.readInt();
                final int sizeW = networkBuffer.readInt();
                final byte[] color = networkBuffer.readByteArray();
                final int price = networkBuffer.readInt();
                final String feedName = networkBuffer.readUtf(64);

                DummyCanvasData paintingData = ZetterCanvasTypes.DUMMY.get().createWrap(resolution, sizeW * resolution.getNumeric(), sizeH * resolution.getNumeric(), color);

                PaintingMerchantPurchaseOffer offer = PaintingMerchantPurchaseOffer.createOfferFromNetwork(
                    paintingData, paintingGalleryUuid, paintingTitle,
                    paintingAuthorUuid, paintingAuthorName, price
                );
                offer.setFeedName(feedName);

                offers.add(offer);

                i++;
            }

            return new SGalleryOffersPacket(cycleInfo, offers);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGallerySalesPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.cycleInfo.incrementId);
        networkBuffer.writeDate(this.cycleInfo.startsAt);
        networkBuffer.writeDate(this.cycleInfo.endsAt);
        networkBuffer.writeUtf(this.cycleInfo.seed, 16);

        networkBuffer.writeInt(this.offers.size());

        for (PaintingMerchantPurchaseOffer merchantOffer : this.offers) {
            DummyCanvasData paintingData = merchantOffer.getDummyPaintingData();

            int resolution = paintingData.getResolution().getNumeric();
            byte[] color = new byte[paintingData.getColorDataBuffer().remaining()];
            paintingData.getColorDataBuffer().get(color);

            networkBuffer.writeUUID(merchantOffer.getPaintingUuid());
            networkBuffer.writeUtf(merchantOffer.getPaintingName(), MAX_NAME_LENGTH);
            networkBuffer.writeUUID(merchantOffer.getAuthorUuid());
            networkBuffer.writeUtf(merchantOffer.getAuthorName(), MAX_AUTHOR_LENGTH);
            networkBuffer.writeInt(paintingData.getResolution().getNumeric());
            networkBuffer.writeInt(paintingData.getHeight() / resolution);
            networkBuffer.writeInt(paintingData.getWidth() / resolution);
            networkBuffer.writeByteArray(color);
            networkBuffer.writeInt(merchantOffer.getPrice());
            networkBuffer.writeUtf(merchantOffer.getFeedName(), 64);
        }
    }

    public static void handle(final SGalleryOffersPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryOffersPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processPaintingMerchantOffers(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryOffersPacket[]";
    }
}