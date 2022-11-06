package me.dantaeusb.zettergallery.gallery;

import me.dantaeusb.zetter.canvastracker.CanvasClientTracker;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import me.dantaeusb.zettergallery.core.ZetterGalleryCapabilities;
import me.dantaeusb.zettergallery.gallery.GalleryServerCapability;
import me.dantaeusb.zettergallery.gallery.IGalleryCapability;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GalleryCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    private final IGalleryCapability galleryCapability;

    private final String TAG_NAME_GALLERY_CAPABILITY = "ZetterGallery";

    public GalleryCapabilityProvider(Level world) {
        if (world.isClientSide()) {
            this.galleryCapability = new GalleryClientCapability(world);
        } else {
            this.galleryCapability = new GalleryServerCapability(world);
        }
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
        if (ZetterGalleryCapabilities.GALLERY == capability) {
            return (LazyOptional<T>)LazyOptional.of(()-> this.galleryCapability);
        }

        return LazyOptional.empty();
    }

    /**
     * Write all the capability state information to NBT
     * We need to save data only for Server Implementation of the capability
     */
    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();

        if (this.galleryCapability.getWorld() == null || this.galleryCapability.getWorld().isClientSide()) {
            return compoundTag;
        }

        Tag canvasTrackerTag = ((GalleryServerCapability) this.galleryCapability).serializeNBT();
        compoundTag.put(TAG_NAME_GALLERY_CAPABILITY, canvasTrackerTag);

        return compoundTag;
    }

    /**
     * Read the capability state information out of NBT
     * We need to get the data only for Server Implementation of the capability
     */
    public void deserializeNBT(CompoundTag compoundTag) {
        if (this.galleryCapability.getWorld() == null || this.galleryCapability.getWorld().isClientSide()) {
            return;
        }

        Tag galleryTag = compoundTag.get(TAG_NAME_GALLERY_CAPABILITY);
        ((GalleryServerCapability) this.galleryCapability).deserializeNBT(galleryTag);
    }
}
