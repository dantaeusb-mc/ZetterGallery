package me.dantaeusb.zettergallery.network.http.stub;

import javax.annotation.Nullable;

public class TokenRequest {
    @Nullable
    public final CrossAuthorizationRole crossAuthorizationRole;

    public TokenRequest(CrossAuthorizationRole crossAuthorizationRole)
    {
        this.crossAuthorizationRole = crossAuthorizationRole;
    }

    public enum CrossAuthorizationRole
    {
        PLAYER("player"),
        PLAYER_SERVER("player_server");

        private final String name;

        CrossAuthorizationRole(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return this.name;
        }

        public String getString()
        {
            return this.name;
        }
    }
}
