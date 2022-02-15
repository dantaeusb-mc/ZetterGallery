package me.dantaeusb.zettergallery.network.http.stub;

import java.util.List;

public class RegisterRequest {
    public boolean singleplayer;

    public String title;
    public String motd;
    public String galleryVersion;

    public List<String> rating;

    public RegisterRequest(boolean singleplayer, String title, String motd, String galleryVersion) {
        this.singleplayer = singleplayer;
        this.title = title;
        this.motd = motd;
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
