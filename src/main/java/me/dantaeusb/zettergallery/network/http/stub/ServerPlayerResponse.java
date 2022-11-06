package me.dantaeusb.zettergallery.network.http.stub;

import me.dantaeusb.zettergallery.gallery.AuthorizationCode;
import me.dantaeusb.zettergallery.gallery.GalleryServerCapability;

import java.util.Date;
import java.util.UUID;

public class ServerPlayerResponse {
    public UUID uuid;
    public String name;

    public AuthTokenResponse token;
    public AuthorizationCode poolingAuthorizationCode;
}
