package me.dantaeusb.zettergallery.network.http.stub;

import me.dantaeusb.zettergallery.capability.gallery.GalleryServer;

import java.util.UUID;

public class ServerResponse {
    public UUID uuid;
    public String ip;
    public String title;
    public String motd;
    public GalleryServer.ClientInfo client;
}
