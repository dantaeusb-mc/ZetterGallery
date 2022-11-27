package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.core.ZetterGalleryCanvasTypes;
import me.dantaeusb.zettergallery.network.ClientHandler;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import me.dantaeusb.zettergallery.trading.PaintingMerchantOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;
import java.util.function.Supplier;

/**
 * @todo: Is that okay that we don't have classic handler here?
 */
public class SGalleryOffersPacket {
    static final int MAX_NAME_LENGTH = 128;
    static final int MAX_AUTHOR_LENGTH = 64;

    private final List<PaintingMerchantOffer<GalleryPaintingData>> offers;

    public SGalleryOffersPacket(List<PaintingMerchantOffer<GalleryPaintingData>> offers) {
        this.offers = offers;
    }

    public List<PaintingMerchantOffer<GalleryPaintingData>> getOffers() {
        return this.offers;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SGalleryOffersPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            final int size = networkBuffer.readInt();
            int i = 0;

            Vector<PaintingMerchantOffer<GalleryPaintingData>> offers = new Vector<>();

            while (i < size) {
                final UUID uuid = networkBuffer.readUUID();
                final String title = networkBuffer.readUtf(MAX_NAME_LENGTH);
                final String authorName = networkBuffer.readUtf(MAX_AUTHOR_LENGTH);
                final AbstractCanvasData.Resolution resolution = AbstractCanvasData.Resolution.get(networkBuffer.readInt());
                final int sizeH = networkBuffer.readInt();
                final int sizeW = networkBuffer.readInt();
                final byte[] color = networkBuffer.readByteArray();
                final int price = networkBuffer.readInt();
                final String feedName = networkBuffer.readUtf(64);

                GalleryPaintingData paintingData = ZetterGalleryCanvasTypes.GALLERY_PAINTING.get().createWrap(resolution, sizeW * resolution.getNumeric(), sizeH * resolution.getNumeric(), color);
                paintingData.setMetaProperties(uuid, authorName, title);

                PaintingMerchantOffer<GalleryPaintingData> offer = PaintingMerchantOffer.createOfferFromPaintingData(paintingData, price);
                offer.setFeedName(feedName);

                offers.add(offer);

                i++;
            }

            return new SGalleryOffersPacket(offers);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGallerySalesPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.offers.size());

        for (PaintingMerchantOffer merchantOffer : this.offers) {
            if (
                merchantOffer.isSaleOffer()
                || merchantOffer.getPaintingData().isEmpty()
                || !(merchantOffer.getPaintingData().get() instanceof GalleryPaintingData)
            ) {
                ZetterGallery.LOG.error("Trying to send sell offer over the net");
                return;
            }

            GalleryPaintingData paintingData = (GalleryPaintingData) merchantOffer.getPaintingData().get();

            int resolution = paintingData.getResolution().getNumeric();
            byte[] color = new byte[paintingData.getColorDataBuffer().remaining()];
            paintingData.getColorDataBuffer().get(color);

            networkBuffer.writeUUID(paintingData.getUUID());
            networkBuffer.writeUtf(paintingData.getPaintingTitle(), MAX_NAME_LENGTH);
            networkBuffer.writeUtf(paintingData.getAuthorName(), MAX_AUTHOR_LENGTH);
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