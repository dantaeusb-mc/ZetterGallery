package me.dantaeusb.zettergallery.network.http.stub;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.UUID;

public class ServerResponse {
    public UUID uuid;
    public String ip;
    public String title;
    public String motd;
    public ServerToken token;

    public static class ServerToken {
        public String token;
        public Date issued;
        public Date notAfter;
        public String type;
    }
}
