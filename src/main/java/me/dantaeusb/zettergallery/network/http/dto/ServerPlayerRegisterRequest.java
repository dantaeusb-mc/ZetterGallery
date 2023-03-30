package me.dantaeusb.zettergallery.network.http.dto;

import java.util.List;
import java.util.UUID;

public class ServerPlayerRegisterRequest {
    public UUID uuid;
    public String name;

    public List<String> rating;

    public ServerPlayerRegisterRequest(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;

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
