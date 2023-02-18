package me.dantaeusb.zettergallery.trading;

import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.entity.merchant.IMerchant;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.util.UUID;

public class PaintingMerchantSaleOffer extends PaintingMerchantAbstractOffer {
    public static final ITextComponent ANOTHER_PLAYERS_PAINTING_ERROR = new TranslationTextComponent("container.zettergallery.merchant.another_players_painting");

    private final String realCanvasCode;

    private DummyCanvasData paintingDummyData;
    private String paintingName;
    private UUID paintingAuthorUuid;
    private String paintingAuthorName;

    /**
     * Sale offers often created on client when painting data is not yet accessible,
     * and should request that data from the server. However, sometimes they
     * can proceed without extra request.
     *
     * @param paintingData
     * @param paintingTitle
     * @param paintingAuthorUuid
     * @param paintingAuthorName
     * @param price
     */
    private PaintingMerchantSaleOffer(
        String canvasCode, DummyCanvasData paintingData, String paintingTitle,
        UUID paintingAuthorUuid, String paintingAuthorName, int price
    ) {
        super(price, State.WAITING);

        this.realCanvasCode = canvasCode;

        this.paintingDummyData = paintingData;

        this.paintingName = paintingTitle;
        this.paintingAuthorUuid = paintingAuthorUuid;
        this.paintingAuthorName = paintingAuthorName;
    }

    /**
     * Sale offers often created on client when painting data is not yet accessible,
     * and should request that data from the server. However, sometimes they
     * can proceed without extra request.
     *
     * @param paintingAuthorUuid
     * @param paintingAuthorName
     * @param price
     */
    private PaintingMerchantSaleOffer(
        String canvasCode, UUID paintingAuthorUuid, String paintingAuthorName, int price
    ) {
        super(price, State.WAITING);

        this.realCanvasCode = canvasCode;

        this.paintingAuthorUuid = paintingAuthorUuid;
        this.paintingAuthorName = paintingAuthorName;
    }

    /**
     * Creates painting merchant offer from player's painting
     * @param paintingData
     * @param price
     * @return
     */
    public static PaintingMerchantSaleOffer createOfferFromPlayersPainting(String canvasCode, PaintingData paintingData, int price) {
        DummyCanvasData paintingWrap = ZetterCanvasTypes.DUMMY.get().createWrap(
            canvasCode, paintingData.getResolution(), paintingData.getWidth(), paintingData.getHeight(),
            paintingData.getColorData()
        );

        return new PaintingMerchantSaleOffer(
            canvasCode,
            paintingWrap,
            paintingData.getPaintingName(),
            paintingData.getAuthorUuid(),
            paintingData.getAuthorName(),
            price
        );
    }

    public static PaintingMerchantSaleOffer createOfferWithoutPlayersPainting(String canvasCode, PlayerEntity player, int price) {
        return new PaintingMerchantSaleOffer(
            canvasCode,
            player.getUUID(),
            player.getName().getString(),
            price
        );
    }

    public void register(World level) {
        Helper.getLevelCanvasTracker(level).registerCanvasData(
            this.getDummyCanvasCode(),
            this.getDummyPaintingData()
        );
    }

    public static void unregister(World level) {
        Helper.getLevelCanvasTracker(level).unregisterCanvasData(
            PaintingMerchantSaleOffer.getStaticCanvasCode()
        );
    }

    /**
     * Locally validates the painting: i.e.
     * forbids to sell paintings from other players
     * @return
     */
    public boolean validate(IMerchant merchant) {
        if (!merchant.getTradingPlayer().getUUID().equals(this.paintingAuthorUuid)) {
            this.markError(new GalleryError(GalleryError.CLIENT_INVALID_OFFER, ANOTHER_PLAYERS_PAINTING_ERROR.getString()));
            return false;
        }

        return true;
    }

    public void updatePaintingData(String paintingName, DummyCanvasData paintingData, UUID paintingAuthorUuid, String paintingAuthorName) {
        this.paintingName = paintingName;
        this.paintingDummyData = paintingData;

        this.paintingAuthorUuid = paintingAuthorUuid;
        this.paintingAuthorName = paintingAuthorName;

        this.state = State.UNFULFILLED;
    }

    @Override
    public String getRealCanvasCode() {
        return this.realCanvasCode;
    }

    @Override
    public String getDummyCanvasCode() {
        return PaintingMerchantSaleOffer.getStaticCanvasCode();
    }

    public static String getStaticCanvasCode() {
        return GalleryPaintingData.getDummySaleOfferCanvasCode();
    }

    @Override
    public DummyCanvasData getDummyPaintingData() {
        if (this.isLoading() || this.paintingDummyData == null) {
            throw new IllegalStateException("Sale offer is still loading");
        }

        return this.paintingDummyData;
    }

    @Override
    public String getPaintingName() {
        if (this.isLoading() || this.paintingName == null) {
            throw new IllegalStateException("Sale offer is still loading");
        }

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

    @Override
    public boolean isLoading() {
        return this.paintingDummyData == null;
    }

    public ItemStack getOfferResult() {
        if (this.isReady()) {
            return new ItemStack(Items.EMERALD, this.price);
        } else {
            return ItemStack.EMPTY;
        }
    }
}
