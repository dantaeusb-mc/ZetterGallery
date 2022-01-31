package me.dantaeusb.zettergallery.network.http.stub;

import java.util.List;

public class RegisterRequest {
    public String hostname;
    public String ip;
    public String motd;
    public List<String> rating;
    public boolean isLocal;

    public RegisterRequest(boolean isLocal, String hostname, String ip, String motd) {
        this.isLocal = isLocal;

        if (isLocal) {
            this.hostname = "Localhost";
        } else {
            this.hostname = hostname;
            this.ip = ip;
        }

        this.motd = motd;
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
