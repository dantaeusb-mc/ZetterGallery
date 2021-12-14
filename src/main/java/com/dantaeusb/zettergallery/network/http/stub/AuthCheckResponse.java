package com.dantaeusb.zettergallery.network.http.stub;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.UUID;

public class AuthCheckResponse {
    public UUID playerUuid;
    public PlayerRights playerRights;
    public String role;
    public Date issued;
    public Date notAfter;
    public String type;

    @Nullable
    public CrossAuthorizationRequestResponse crossAuthorizationCode;

    public class PlayerRights {
        public boolean canBuy;
        public boolean canSell;
    }
}
