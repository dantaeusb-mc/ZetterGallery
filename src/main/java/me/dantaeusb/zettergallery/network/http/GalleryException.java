package me.dantaeusb.zettergallery.network.http;

public class GalleryException extends Exception {
    private final int code;

    public GalleryException(int code, String message) {
        super(message);

        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
