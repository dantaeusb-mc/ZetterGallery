package me.dantaeusb.zettergallery.gallery;

import java.util.Date;

public class PlayerToken extends Token {
    public CrossAuthCode crossAuthCode;

    PlayerToken(String token, Date issuedAt, Date notAfter) {
        super(token, issuedAt, notAfter);
    }

    public void setCrossAuthCode(String code, Date issued, Date notAfter) {
        this.crossAuthCode = new CrossAuthCode(code, issued, notAfter);
    }

    class CrossAuthCode {
        public String code;
        public Date issued;
        public Date notAfter;

        public CrossAuthCode(String code, Date issued, Date notAfter) {
            this.code = code;
            this.issued = issued;
            this.notAfter = notAfter;
        }
    }
}
