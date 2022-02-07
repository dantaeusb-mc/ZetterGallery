package me.dantaeusb.zettergallery.gallery;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.UUID;

public class PlayerTokenStorage {
    private static PlayerTokenStorage instance;

    private final HashMap<UUID, PlayerToken> playerTokenMap = new HashMap<>();

    public PlayerTokenStorage() {
    }

    public static PlayerTokenStorage getInstance() {
        if (PlayerTokenStorage.instance == null) {
            PlayerTokenStorage.instance = new PlayerTokenStorage();
        }

        return instance;
    }

    public void setPlayerToken(ServerPlayer playerEntity, PlayerToken token) {
        this.playerTokenMap.put(playerEntity.getUUID(), token);
    }

    public boolean hasPlayerToken(ServerPlayer playerEntity) {
        return this.playerTokenMap.containsKey(playerEntity.getUUID());
    }

    public void removePlayerToken(ServerPlayer playerEntity) {
        this.playerTokenMap.remove(playerEntity.getUUID());
    }

    public @Nullable PlayerToken getPlayerToken(ServerPlayer playerEntity) {
        UUID playerId = playerEntity.getUUID();

        if (!this.playerTokenMap.containsKey(playerId)) {
            return null;
        }

        return this.playerTokenMap.get(playerId);
    }

    public @Nullable String getPlayerTokenString(ServerPlayer playerEntity) {
        UUID playerId = playerEntity.getUUID();

        if (!this.playerTokenMap.containsKey(playerId)) {
            return null;
        }

        return this.playerTokenMap.get(playerId).token;
    }
}
