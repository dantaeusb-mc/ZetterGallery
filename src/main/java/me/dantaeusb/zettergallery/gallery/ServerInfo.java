package me.dantaeusb.zettergallery.gallery;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class ServerInfo {
    public final boolean singleplayer;
    public final String title;
    public final String motd;
    public final String galleryVersion;

    private ServerInfo(boolean singleplayer, String title, String motd, String galleryVersion) {
        this.singleplayer = singleplayer;
        this.title = title;
        this.motd = motd;
        this.galleryVersion = galleryVersion;
    }

    public static ServerInfo createSingleplayerServer() {
        return new ServerInfo(true, "Singleplayer world", "Singleplayer world", ServerInfo.getGalleryVersion());
    }

    public static ServerInfo createMultiplayerServer(String title, String motd) {
        return new ServerInfo(false, title, motd, ServerInfo.getGalleryVersion());
    }

    private static String getGalleryVersion() {
        IModInfo zetterGalleryModInfo = ModList.get().getMods().stream()
                .filter(mod -> mod.getModId().equals("zettergallery"))
                .findAny()
                .orElse(null);

        ArtifactVersion modVersion = zetterGalleryModInfo.getVersion();

        return modVersion.getMajorVersion() + "." + modVersion.getIncrementalVersion() + "." + modVersion.getMinorVersion();
    }
}
