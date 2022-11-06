package me.dantaeusb.zettergallery.network.http.stub;

import me.dantaeusb.zettergallery.gallery.GalleryServerCapability;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.UUID;

public class ServerResponse {
    public UUID uuid;
    public String ip;
    public String title;
    public String motd;
    public GalleryServerCapability.ClientInfo client;
}
