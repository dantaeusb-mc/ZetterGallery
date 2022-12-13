package me.dantaeusb.zettergallery.gallery;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class ServerInfo {
    public final boolean singleplayer;
    public final String motd;
    public final String gameVersion;
    public final String galleryVersion;

    private ServerInfo(boolean singleplayer, String motd, String gameVersion, String galleryVersion) {
        this.singleplayer = singleplayer;
        this.motd = motd;
        this.gameVersion = gameVersion;
        this.galleryVersion = galleryVersion;
    }

    public static ServerInfo createSingleplayerServer(String gameVersion) {
        return new ServerInfo(true, "Singleplayer world", gameVersion, ServerInfo.getGalleryVersion());
    }

    public static ServerInfo createMultiplayerServer(String motd, String gameVersion) {
        return new ServerInfo(false, motd, gameVersion, ServerInfo.getGalleryVersion());
    }

    private static String getGalleryVersion() {
        if (FMLEnvironment.production) {
            IModInfo zetterGalleryModInfo = ModList.get().getMods().stream()
                    .filter(mod -> mod.getModId().equals("zettergallery"))
                    .findAny()
                    .orElse(null);

            ArtifactVersion modVersion = zetterGalleryModInfo.getVersion();

            return modVersion.getMajorVersion() + "." + modVersion.getMinorVersion() + "." + modVersion.getIncrementalVersion();
        }

        return "1.0.0";
    }
}
