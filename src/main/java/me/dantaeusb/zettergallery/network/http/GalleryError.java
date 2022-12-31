package me.dantaeusb.zettergallery.network.http;

import javax.annotation.Nullable;

public class GalleryError {
    public static final int UNKNOWN = 0;
    public static final int SERVER_INVALID_VERSION = 1001;
    public static final int PLAYER_FEED_UNAVAILABLE = 1002;
    public static final int SERVER_SALE_DISALLOWED = 1003;
    public static final int SERVER_RECEIVED_INVALID_PAINTING_DATA = 1004;
    public static final int CLIENT_INVALID_OFFER = 1005;
    public static final int SERVER_UNAVAILABLE = 1999;

    public static final int UNKNOWN_FSM_ERROR = 2000;

    private final int code;
    private final String message;
    private @Nullable String clientMessage;

    public GalleryError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setClientMessage(String message) {
        this.clientMessage = message;
    }

    public String getClientMessage() {
        // @todo: HTTP errors

        if (this.clientMessage != null) {
            return this.clientMessage;
        }

        return this.message;
    }

    public String getShortMessage() {

        return "Unknown error";
    }
}
