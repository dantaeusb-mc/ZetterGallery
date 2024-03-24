package me.dantaeusb.zettergallery.gallery;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

    /**
     * Check that the Player tokens are still relevant:
     * 1. Remove if expired
     * 2. Refresh if can be refreshed
     * 3. Remove authorization code if expired
     */
    public void validateTokens() {
        final Date now = new Date();

        if (this.playerTokenMap.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, PlayerToken> entry : this.playerTokenMap.entrySet()) {
            if (entry.getValue().notAfter.before(now)) {
                this.playerTokenMap.remove(entry.getKey());
            }

            if (entry.getValue().needRefresh()) {
                // @todo: [MED] Refresh token
            }

            if (entry.getValue().authorizationCode != null && entry.getValue().authorizationCode.notAfter.before(now)) {
                entry.getValue().dropAuthorizationCode();
            }
        }

        this.playerTokenMap.entrySet().removeIf(entry -> entry.getValue().notAfter.before(now));
    }

    public int getSize() {
        return this.playerTokenMap.size();
    }

    public void flush() {
        this.playerTokenMap.clear();
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
