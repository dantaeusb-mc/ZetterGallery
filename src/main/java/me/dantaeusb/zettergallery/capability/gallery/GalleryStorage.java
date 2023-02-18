package me.dantaeusb.zettergallery.capability.gallery;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class GalleryStorage  implements Capability.IStorage<Gallery> {
    public static final String NBT_TAG_CLIENT_ID = "ClientID";
    public static final String NBT_TAG_CLIENT_NAME = "ClientName";
    public static final String NBT_TAG_CLIENT_SECRET = "ClientSecret";

    @Override
    public INBT writeNBT(Capability<Gallery> capability, Gallery instance, @Nullable Direction side) {
        CompoundNBT compound = new CompoundNBT();

        //compound.putByteArray(NBT_TAG_PAINTINGS_METADATA, this.paintingsMetadata.toByteArray());

        if (instance.getClientInfo() != null) {
            instance.getClientInfo().serialize(compound);
        }

        return compound;
    }

    @Override
    public void readNBT(Capability<Gallery> capability, Gallery instance, Direction side, @Nullable INBT tag) {
        if (tag.getType() == CompoundNBT.TYPE) {
            CompoundNBT compound = (CompoundNBT) tag;

            if (compound.contains(NBT_TAG_CLIENT_ID) && compound.contains(NBT_TAG_CLIENT_SECRET)) {
                instance.setClientInfo(GalleryServer.ClientInfo.deserialize(compound));
            }
        }
    }
}
