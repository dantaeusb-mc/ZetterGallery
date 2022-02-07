package me.dantaeusb.zettergallery.gallery;

import java.util.Date;

public class Token {
    public final String token;
    public final Date issuedAt;
    public final Date notAfter;

    Token(String token, Date issuedAt, Date notAfter) {
        this.token = token;
        this.issuedAt = issuedAt;
        this.notAfter = notAfter;
    }

    public boolean valid() {
        return true;
    }
}
