package me.dantaeusb.zettergallery.gallery;

public class ServerInfo {
    public final boolean singleplayer;
    public final String title;
    public final String motd;

    private ServerInfo(boolean singleplayer, String title, String motd) {
        this.singleplayer = singleplayer;
        this.title = title;
        this.motd = motd;
    }

    public static ServerInfo createSingleplayerServer() {
        return new ServerInfo(true, "Singleplayer world", "Singleplayer world");
    }

    public static ServerInfo createMultiplayerServer(String title, String motd) {
        return new ServerInfo(false, title, motd);
    }
}
