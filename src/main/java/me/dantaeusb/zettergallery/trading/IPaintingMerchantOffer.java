package me.dantaeusb.zettergallery.trading;

import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public interface IPaintingMerchantOffer {
    /**
     * Used to make a painting, real code of the painting behind the offer
     * @return
     */
    String getRealCanvasCode();

    /**
     * Used to render an offer, a copy of painting data (to avoid
     * clash with existing painting data and manage separately)
     * @return
     */
    String getDummyCanvasCode();

    String getPaintingName();

    DummyCanvasData getDummyPaintingData();

    int getPrice();

    UUID getAuthorUuid();

    String getAuthorName();

    boolean isLoading();

    boolean isReady();

    boolean isError();

    ItemStack getOfferResult();

    void unfulfilled();

    void ready();

    void markError(GalleryError error);

    String getErrorMessage();

    enum State {
        WAITING("waiting"), // Painting waiting for validation / no data received
        UNFULFILLED("unfulfilled"), // Price is not offered (not enough emeralds in slot)
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

            throw new IllegalArgumentException(value + " is not an offer state");
        }

        public String toValue() {
            return value;
        }
    }
}
