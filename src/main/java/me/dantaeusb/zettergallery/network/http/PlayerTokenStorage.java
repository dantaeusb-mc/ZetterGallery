package me.dantaeusb.zettergallery.network.http;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.UUID;

public class PlayerTokenStorage {
    private static PlayerTokenStorage instance;

    private final HashMap<UUID, PlayerTokenEntry> playerTokenMap = new HashMap<>();

    public PlayerTokenStorage() {
    }

    public static PlayerTokenStorage getInstance() {
        if (PlayerTokenStorage.instance == null) {
            PlayerTokenStorage.instance = new PlayerTokenStorage();
        }

        return instance;
    }

    public void setPlayerToken(ServerPlayer playerEntity, String token) {
        this.playerTokenMap.put(playerEntity.getUUID(), new PlayerTokenEntry(token));
    }

    public boolean hasPlayerToken(ServerPlayer playerEntity) {
        return this.playerTokenMap.containsKey(playerEntity.getUUID());
    }

    public void removePlayerToken(ServerPlayer playerEntity) {
        this.playerTokenMap.remove(playerEntity.getUUID());
    }

    public @Nullable String getPlayerToken(ServerPlayer playerEntity) {
        UUID playerId = playerEntity.getUUID();

        if (!this.playerTokenMap.containsKey(playerId)) {
            return null;
        }

        return this.playerTokenMap.get(playerId).token;
    }

    private static class PlayerTokenEntry {
        public final String token;
        public Timestamp lastUsed;

        PlayerTokenEntry(String token) {
            this.token = token;
            this.lastUsed = new Timestamp(System.currentTimeMillis());
        }

        public boolean validate() {
            return System.currentTimeMillis() - this.lastUsed.getTime() > 1000 * 60 * 60;
        }
    }
}
