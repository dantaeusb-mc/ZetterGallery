package me.dantaeusb.zettergallery.gallery;

import net.minecraft.entity.player.ServerPlayerEntity;

import javax.annotation.Nullable;
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

    public int getSize() {
        return this.playerTokenMap.size();
    }

    public void flush() {
        this.playerTokenMap.clear();
    }

    public void setPlayerToken(ServerPlayerEntity playerEntity, PlayerToken token) {
        this.playerTokenMap.put(playerEntity.getUUID(), token);
    }

    public boolean hasPlayerToken(ServerPlayerEntity playerEntity) {
        return this.playerTokenMap.containsKey(playerEntity.getUUID());
    }

    public void removePlayerToken(ServerPlayerEntity playerEntity) {
        this.playerTokenMap.remove(playerEntity.getUUID());
    }

    public @Nullable PlayerToken getPlayerToken(ServerPlayerEntity playerEntity) {
        UUID playerId = playerEntity.getUUID();

        if (!this.playerTokenMap.containsKey(playerId)) {
            return null;
        }

        return this.playerTokenMap.get(playerId);
    }

    public @Nullable String getPlayerTokenString(ServerPlayerEntity playerEntity) {
        UUID playerId = playerEntity.getUUID();

        if (!this.playerTokenMap.containsKey(playerId)) {
            return null;
        }

        return this.playerTokenMap.get(playerId).token;
    }
}
