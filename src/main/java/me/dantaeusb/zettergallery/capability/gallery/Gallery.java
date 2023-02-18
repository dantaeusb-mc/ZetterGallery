package me.dantaeusb.zettergallery.capability.gallery;

import net.minecraft.world.World;

import javax.annotation.Nullable;

public interface Gallery {
    void setWorld(World world);
    World getWorld();

    void setClientInfo(GalleryServer.ClientInfo clientInfo);
    @Nullable GalleryServer.ClientInfo getClientInfo();
}
