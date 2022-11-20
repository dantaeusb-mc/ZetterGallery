package me.dantaeusb.zettergallery.gallery;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

public class GalleryServerCapability implements IGalleryCapability {
    private static final String NBT_TAG_PAINTINGS_TRACKER = "PaintingsTracker";
    private static final String NBT_TAG_CLIENT_ID = "ClientID";
    private static final String NBT_TAG_CLIENT_NAME = "ClientName";
    private static final String NBT_TAG_CLIENT_SECRET = "ClientSecret";

    private Level overworld;

    @Nullable
    private ClientInfo clientInfo;

    public GalleryServerCapability(Level overworld) {
        this.overworld = overworld;
    }

    public Level getWorld() {
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

    public Tag serializeNBT() {
        CompoundTag compound = new CompoundTag();

        //compound.putByteArray(NBT_TAG_PAINTINGS_METADATA, this.paintingsMetadata.toByteArray());

        if (this.clientInfo != null) {
            this.clientInfo.serialize(compound);
        }

        return compound;
    }

    public void deserializeNBT(Tag tag) {
        if (tag.getType() == CompoundTag.TYPE) {
            CompoundTag compound = (CompoundTag) tag;

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

        public void serialize(CompoundTag compound) {
            compound.putString(NBT_TAG_CLIENT_ID, this.id);
            compound.putString(NBT_TAG_CLIENT_NAME, this.name);
            compound.putString(NBT_TAG_CLIENT_SECRET, this.secret);
        }

        public static ClientInfo deserialize(CompoundTag compound) {
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

        public void serialize(CompoundTag compoundTag) {
            //compoundTag.putLong(this.updatedAt);
        }
    }
}
