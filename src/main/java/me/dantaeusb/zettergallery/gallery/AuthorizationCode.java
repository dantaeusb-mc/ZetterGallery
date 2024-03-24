package me.dantaeusb.zettergallery.gallery;

import java.util.Date;

public class AuthorizationCode {
    public String code;
    public Date issuedAt;
    public Date notAfter;

    public AuthorizationCode(String code, Date issuedAt, Date notAfter) {
        this.code = code;
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
}
