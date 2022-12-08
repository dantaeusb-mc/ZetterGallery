package me.dantaeusb.zettergallery.trading;

import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import me.dantaeusb.zettergallery.network.http.GalleryError;
import me.dantaeusb.zettergallery.storage.GalleryPaintingData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public abstract class PaintingMerchantAbstractOffer implements IPaintingMerchantOffer {
    protected final int price;

    /**
     * If we're ready to make a transaction
     */
    protected State state;

    /**
     * Describe error or action
     */
    protected String message;

    protected PaintingMerchantAbstractOffer(int price, State state) {
        this.price = price;
        this.state = state;
    }

    @Override
    public int getPrice() {
        return this.price;
    }

    @Override
    public boolean isReady() {
        return this.state == State.READY;
    }

    @Override
    public boolean isError() {
        return this.state == State.ERROR;
    }

    @Override
    public void unfulfilled() {
        this.state = State.UNFULFILLED;
    }

    @Override
    public void ready() {
        this.state = State.READY;
    }

    @Override
    public void markError(GalleryError error) {
        this.state = State.ERROR;
        this.message = error.getClientMessage();
    }

    @Override
    public String getErrorMessage() {
        return this.message;
    }
}
