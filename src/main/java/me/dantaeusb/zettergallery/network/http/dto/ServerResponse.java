package me.dantaeusb.zettergallery.network.http.dto;

import me.dantaeusb.zettergallery.gallery.GalleryServerCapability;

import java.util.UUID;

public class ServerResponse {
    public UUID uuid;
    public String ip;
    public String title;
    public String motd;
    public GalleryServerCapability.ClientInfo client;
}
