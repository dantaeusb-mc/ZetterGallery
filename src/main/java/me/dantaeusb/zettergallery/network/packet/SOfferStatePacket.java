package me.dantaeusb.zettergallery.network.packet;

import me.dantaeusb.zettergallery.ZetterGallery;
import me.dantaeusb.zettergallery.network.ClientHandler;
import me.dantaeusb.zettergallery.trading.PaintingMerchantPurchaseOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SOfferStatePacket {
    private final String canvasCode;
    private final PaintingMerchantPurchaseOffer.State state;
    private final String message;

    public SOfferStatePacket(String canvasCode, PaintingMerchantPurchaseOffer.State state, String message) {
        this.canvasCode = canvasCode;
        this.state = state;
        this.message = message;
    }

    public String getCanvasCode() {
        return this.canvasCode;
    }

    public PaintingMerchantPurchaseOffer.State getState() {
        return this.state;
    }

    public String getMessage() {
        return this.message;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SOfferStatePacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String canvasCode = networkBuffer.readUtf(32767);
            String state = networkBuffer.readUtf(32767);
            String message = networkBuffer.readUtf(32767);

            return new SOfferStatePacket(canvasCode, PaintingMerchantPurchaseOffer.State.fromValue(state), message);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ZetterGallery.LOG.warn("Exception while reading SGalleryOfferStatePacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.canvasCode, 32767);
        networkBuffer.writeUtf(this.state.toValue(), 32767);
        networkBuffer.writeUtf(this.message, 32767);
    }

    public static void handle(final SOfferStatePacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ZetterGallery.LOG.warn("SGalleryOfferStatePacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processPaintingOfferState(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SGalleryErrorPacket[message=" + this.message + "]";
    }
}