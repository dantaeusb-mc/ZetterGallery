package com.dantaeusb.zettergallery.network.http.stub;

import javax.annotation.Nullable;
import java.util.Date;

public class AuthTokenResponse {
    public String token;
    public Date issued;
    public Date notAfter;
    public String type;

    @Nullable
    public CrossAuthorizationRequestResponse crossAuthorizationCode;
}
