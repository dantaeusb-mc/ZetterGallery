package me.dantaeusb.zettergallery.capability.gallery;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

import javax.annotation.Nullable;

import static me.dantaeusb.zettergallery.capability.gallery.GalleryStorage.*;

public class GalleryServer implements Gallery {
    private World overworld;

    @Nullable
    private ClientInfo clientInfo;

    public GalleryServer() {

    }

    @Override
    public void setWorld(World world) {
        this.overworld = world;
    }

    @Override
    public World getWorld() {
        return this.overworld;
    }

    @Override
    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public void removeClientInfo() {
        this.clientInfo = null;
    }

    @Override
    public @Nullable ClientInfo getClientInfo() {
        return this.clientInfo;
    }

    /*
     * Saving data
     */

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
}
