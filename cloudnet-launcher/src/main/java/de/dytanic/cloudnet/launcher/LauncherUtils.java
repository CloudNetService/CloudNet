package de.dytanic.cloudnet.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.dytanic.cloudnet.launcher.module.CloudNetModule;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class LauncherUtils {

    public static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();

    public static final List<CloudNetModule> DEFAULT_MODULES = Arrays.asList(
            new CloudNetModule("cloudnet-bridge", "cloudnet-bridge.jar"),
            new CloudNetModule("cloudnet-signs", "cloudnet-signs.jar"),
            new CloudNetModule("cloudnet-npcs", "cloudnet-npcs.jar"),
            new CloudNetModule("cloudnet-syncproxy", "cloudnet-syncproxy.jar"),
            new CloudNetModule("cloudnet-cloudflare", "cloudnet-cloudflare.jar"),
            new CloudNetModule("cloudnet-report", "cloudnet-report.jar"),
            new CloudNetModule("cloudnet-rest", "cloudnet-rest.jar"),
            new CloudNetModule("cloudnet-smart", "cloudnet-smart.jar"),
            new CloudNetModule("cloudnet-cloudperms", "cloudnet-cloudperms.jar"),
            new CloudNetModule("cloudnet-storage-ftp", "cloudnet-storage-ftp.jar"),
            new CloudNetModule("cloudnet-database-mysql", "cloudnet-database-mysql.jar"),
            new CloudNetModule("cloudnet-labymod", "cloudnet-labymod.jar")
    );

    public static final String[] VERSION_FILE_NAMES = new String[]{
            "cloudnet.jar",
            "cloudnet.cnl",
            "driver.jar",
            "driver.cnl"
    };

    public static final String
            CLOUDNET_SELECTED_VERSION = "cloudnet.launcher.select.version",
            LAUNCHER_DEV_MODE = "cloudnet.launcher.dev-mode",
            CLOUDNET_REPOSITORY = "cloudnet.repository",
            CLOUDNET_REPOSITORY_GITHUB = "cloudnet.repository.github",
            CLOUDNET_REPOSITORY_AUTO_UPDATE = "cloudnet.auto-update",
            FALLBACK_VERSION = LauncherUtils.class.getPackage().getSpecificationVersion(),
            CLOUDNET_SNAPSHOTS = "cloudnet.snapshots.use-snapshots",
            CLOUDNET_SNAPSHOTS_JOB_URL = "cloudnet.snapshots.job.url";

    private LauncherUtils() {
        throw new UnsupportedOperationException();
    }

    public static InputStream readFromURL(String url) throws IOException {
        URLConnection urlConnection = new URL(url).openConnection();

        urlConnection.setUseCaches(false);
        urlConnection.setDoOutput(false);

        urlConnection.setConnectTimeout(5000);
        urlConnection.setReadTimeout(5000);

        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        urlConnection.connect();

        return urlConnection.getInputStream();
    }

    public static void deleteFile(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                });
    }

}