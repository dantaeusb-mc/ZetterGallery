package me.dantaeusb.zettergallery.gallery;

import java.util.Date;

public class Token {
    // Renew one day before expiration
    public static final long REFRESH_TIME = 24 * 60 * 60 * 1000L;

    public final String token;
    public final Date issuedAt;
    public final Date notAfter;

    public Token(String token, Date issuedAt, Date notAfter) {
        this.token = token;
        this.issuedAt = issuedAt;
        this.notAfter = notAfter;
    }

    /**
     * Assuming valid if not expired
     * @return
     */
    public boolean valid() {
        return this.notAfter.getTime() - System.currentTimeMillis() > 0;
    }

    public boolean needRefresh() {
        return this.notAfter.getTime() - System.currentTimeMillis() < REFRESH_TIME;
    }
}
