package me.dantaeusb.zettergallery.network.http.dto;

import java.util.Date;

public class CrossAuthorizationRequestResponse {
    public String code;
    public Date issued;
    public Date notAfter;

    public CrossAuthorizationRequestResponse(String code, Date issued, Date notAfter) {
        this.code = code;
        this.issued = issued;
        this.notAfter = notAfter;
    }
}
