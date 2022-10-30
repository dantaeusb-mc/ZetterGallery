package me.dantaeusb.zettergallery.gallery;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.UUID;

public class PlayerToken extends Token {
    public @Nullable PlayerInfo authorizedAs;
    public CrossAuthCode crossAuthCode;

    PlayerToken(String token, Date issuedAt, Date notAfter) {
        super(token, issuedAt, notAfter);
    }

    public void setCrossAuthCode(String code, Date issued, Date notAfter) {
        this.crossAuthCode = new CrossAuthCode(code, issued, notAfter);
    }

    @Nullable
    public PlayerInfo getAuthorizedAs() {
        return this.authorizedAs;
    }

    public boolean isAuthorized() {
        return this.authorizedAs != null;
    }

    public CrossAuthCode getCrossAuthCode() {
        return this.crossAuthCode;
    }

    public record PlayerInfo(UUID uuid, String nickname) {
    }

    public record CrossAuthCode(String code, Date issued, Date notAfter) {
    }
}
