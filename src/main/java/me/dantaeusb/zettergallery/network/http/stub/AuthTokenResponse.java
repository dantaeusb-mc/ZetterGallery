package me.dantaeusb.zettergallery.network.http.stub;

import javax.annotation.Nullable;
import java.util.Date;

public class AuthTokenResponse {
    public String token;
    public Date issued;
    public Date notAfter;
    public String type;

    @Nullable
    public CrossAuthorization crossAuthorizationCode;

    public AuthTokenResponse(String token, Date issued, Date notAfter, String type) {
        this.token = token;
        this.issued = issued;
        this.notAfter = notAfter;
        this.type = type;
    }

    public static class CrossAuthorization {
        public String code;
        public Date issued;
        public Date notAfter;

        public CrossAuthorization(String code, Date issued, Date notAfter) {
            this.code = code;
            this.issued = issued;
            this.notAfter = notAfter;
        }
    }
}
