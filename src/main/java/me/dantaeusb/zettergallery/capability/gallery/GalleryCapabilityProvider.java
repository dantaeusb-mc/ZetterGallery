package me.dantaeusb.zettergallery.capability.gallery;

import me.dantaeusb.zetter.capability.canvastracker.CanvasTrackerCapability;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GalleryCapabilityProvider implements ICapabilitySerializable<INBT> {
    private final Gallery gallery;

    private final String TAG_NAME_GALLERY_CAPABILITY = "ZetterGallery";

    public GalleryCapabilityProvider(World world) {
        if (world.isClientSide()) {
            throw new IllegalArgumentException("Gallery capability exists only in server's overworld");
        }

        this.gallery = new GalleryServer();
        this.gallery.setWorld(world);
    }

    /**
     * Asks the Provider if it has the given capability
     * @param capability<T> capability to be checked for
     * @param facing the side of the provider being checked (null = no particular side)
     * @param <T> The interface instance that is used
     * @return a lazy-initialisation supplier of the interface instance that is used to access this capability
     *         In this case, we don't actually use lazy initialisation because the instance is very quick to create.
     *         See CapabilityProviderFlowerBag for an example of lazy initialisation
     */
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (GalleryCapability.CAPABILITY_GALLERY == capability) {
            return (LazyOptional<T>)LazyOptional.of(()-> this.gallery);
        }

        return LazyOptional.empty();
    }

    /**
     * Write all the capability state information to NBT
     * We need to save data only for Server Implementation of the capability
     */
    public INBT serializeNBT() {
        return GalleryCapability.CAPABILITY_GALLERY.getStorage().writeNBT(
            GalleryCapability.CAPABILITY_GALLERY,
            this.gallery,
            null
        );
    }

    /**
     * Read the capability state information out of NBT
     * We need to get the data only for Server Implementation of the capability
     */
    public void deserializeNBT(INBT tag) {
        GalleryCapability.CAPABILITY_GALLERY.getStorage().readNBT(
            GalleryCapability.CAPABILITY_GALLERY,
            this.gallery,
            null,
            tag
        );
    }
}
