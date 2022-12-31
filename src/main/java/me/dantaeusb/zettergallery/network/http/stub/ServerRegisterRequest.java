package me.dantaeusb.zettergallery.network.http.stub;

import java.util.List;

public class ServerRegisterRequest {
    public boolean singleplayer;

    public String motd;
    public String gameVersion;
    public String galleryVersion;

    public List<String> rating;

    public ServerRegisterRequest(boolean singleplayer, String motd, String gameVersion, String galleryVersion) {
        this.singleplayer = singleplayer;
        this.motd = motd;
        this.gameVersion = gameVersion;
        this.galleryVersion = galleryVersion;

        /*
         * Defaults: Low effort, fantasy violence, fear
         * Low effort is internal rating (could be used for filtering too)
         * Whereas fantasy violence and fear
         * Used by PEGI to describe minecraft, so as long as Minecraft is ok,
         * These tags are too.
         */
        this.rating = List.of("L", "FW", "F");
    }
}
