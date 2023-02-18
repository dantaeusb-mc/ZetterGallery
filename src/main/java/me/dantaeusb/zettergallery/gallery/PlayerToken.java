package me.dantaeusb.zettergallery.gallery;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.UUID;

public class PlayerToken extends Token {
    public @Nullable PlayerInfo authorizedAs;
    public @Nullable AuthorizationCode authorizationCode;

    PlayerToken(String token, Date issuedAt, Date notAfter) {
        super(token, issuedAt, notAfter);
    }

    public void setAuthorizedAs(PlayerInfo authorizedAs) {
        this.authorizedAs = authorizedAs;
    }

    @Nullable
    public PlayerInfo getAuthorizedAs() {
        return this.authorizedAs;
    }

    public boolean isAuthorized() {
        return this.authorizedAs != null;
    }

    public void setAuthorizationCode(AuthorizationCode authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public void dropAuthorizationCode() {
        this.authorizationCode = null;
    }

    @Nullable
    public AuthorizationCode getAuthorizationCode() {
        return this.authorizationCode;
    }

    public static class PlayerInfo {
        public final UUID uuid;
        public final String nickname;

        public PlayerInfo(UUID uuid, String nickname) {
            this.uuid = uuid;
            this.nickname = nickname;
        }
    }
}
