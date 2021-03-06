package me.dantaeusb.zettergallery.core;

public class Helper {
    public static int CANVAS_COLOR = 0xFFE0DACE;

    private static Helper instance;

    public static final String GALLERY_SCHEME = "https";
    public static final String GALLERY_HOST = "zetter.gallery";
    public static final String GALLERY_AUTH_SERVER_ENDPOINT = "auth/cross";
    public static final int GALLERY_PORT = 443;

    public static final int GALLERY_CROSS_AUTH_CODE_LENGTH = 12;

    public static Helper getInstance() {
        if (Helper.instance == null) {
            Helper.instance = new Helper();
        }

        return Helper.instance;
    }
}
