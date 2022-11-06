package me.dantaeusb.zettergallery.network.http.stub;

import me.dantaeusb.zettergallery.gallery.Token;

import javax.annotation.Nullable;
import java.util.Date;

public class AuthTokenResponse extends Token {
    public String type;

    @Nullable
    public RefreshToken refreshToken;

    public AuthTokenResponse(String token, Date issued, Date notAfter, String type) {
        super(token, issued, notAfter);

        this.type = type;
    }

    public static class RefreshToken {
        public String token;
        public Date issuedAt;
        public Date notAfter;

        public RefreshToken(String token, Date issuedAt, Date notAfter) {
            this.token = token;
            this.issuedAt = issuedAt;
            this.notAfter = notAfter;
        }
    }
}
