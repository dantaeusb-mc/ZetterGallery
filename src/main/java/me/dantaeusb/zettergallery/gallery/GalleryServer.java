package me.dantaeusb.zettergallery.gallery;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GalleryServer implements Gallery {
    private static final String NBT_TAG_PAINTINGS_TRACKER = "PaintingsTracker";
    private static final String NBT_TAG_CLIENT_ID = "ClientID";
    private static final String NBT_TAG_CLIENT_NAME = "ClientName";
    private static final String NBT_TAG_CLIENT_SECRET = "ClientSecret";

    private World overworld;

    @Nullable
    private ClientInfo clientInfo;

    public GalleryServer() {

    }

    public void setWorld(World world) {
        this.overworld = world;
    }

    public World getWorld() {
        return this.overworld;
    }

    public void saveClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public void removeClientInfo() {
        this.clientInfo = null;
    }

    public @Nullable ClientInfo getClientInfo() {
        return this.clientInfo;
    }

    /*
     * Saving data
     */

    public INBT serializeNBT() {
        CompoundNBT compound = new CompoundNBT();

        //compound.putByteArray(NBT_TAG_PAINTINGS_METADATA, this.paintingsMetadata.toByteArray());

        if (this.clientInfo != null) {
            this.clientInfo.serialize(compound);
        }

        return compound;
    }

    public void deserializeNBT(INBT tag) {
        if (tag.getType() == CompoundNBT.TYPE) {
            CompoundNBT compound = (CompoundNBT) tag;

            if (compound.contains(NBT_TAG_CLIENT_ID) && compound.contains(NBT_TAG_CLIENT_SECRET)) {
                this.clientInfo = ClientInfo.deserialize(compound);
            }
        }
    }

    public static class ClientInfo {
        public String id;
        public String name;
        public String secret;

        public ClientInfo(String id, String name, String secret) {
            this.id = id;
            this.name = name;
            this.secret = secret;
        }

        public void serialize(CompoundNBT compound) {
            compound.putString(NBT_TAG_CLIENT_ID, this.id);
            compound.putString(NBT_TAG_CLIENT_NAME, this.name);
            compound.putString(NBT_TAG_CLIENT_SECRET, this.secret);
        }

        public static ClientInfo deserialize(CompoundNBT compound) {
            final String clientId = compound.getString(NBT_TAG_CLIENT_ID);
            final String clientName = compound.getString(NBT_TAG_CLIENT_NAME);
            final String clientSecret = compound.getString(NBT_TAG_CLIENT_SECRET);

            return new ClientInfo(clientId, clientName, clientSecret);
        }
    }

    public static class PaintingTracker {
        public Long updatedAt;
        public List<UUID> connectedEntities = new ArrayList<>();

        public PaintingTracker(UUID entityId, Long updatedAt) {
            this.updatedAt = updatedAt;
            this.connectedEntities.add(entityId);
        }

        public void serialize(CompoundNBT compoundTag) {
            //compoundTag.putLong(this.updatedAt);
        }
    }

    // Convert to/from NBT
    static class GalleryStorage implements Capability.IStorage<GalleryServer> {
        @Override
        public INBT writeNBT(Capability<GalleryServer> capability, GalleryServer instance, @Nullable Direction side) {
            return instance.serializeNBT();
        }

        @Override
        public void readNBT(Capability<GalleryServer> capability, GalleryServer instance, Direction side, @Nullable INBT nbt) {
            instance.deserializeNBT(nbt);
        }
    }
}
