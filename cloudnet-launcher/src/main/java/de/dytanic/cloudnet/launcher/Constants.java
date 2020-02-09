package de.dytanic.cloudnet.launcher;

import de.dytanic.cloudnet.launcher.module.CloudNetModule;

import java.util.Arrays;
import java.util.List;

public final class Constants {

    public static final List<CloudNetModule> DEFAULT_MODULES = Arrays.asList(
            new CloudNetModule("cloudnet-bridge", "cloudnet-bridge.jar"),
            new CloudNetModule("cloudnet-labymod", "cloudnet-labymod.jar"),
            new CloudNetModule("cloudnet-signs", "cloudnet-signs.jar"),
            new CloudNetModule("cloudnet-syncproxy", "cloudnet-syncproxy.jar"),
            new CloudNetModule("cloudnet-cloudflare", "cloudnet-cloudflare.jar"),
            new CloudNetModule("cloudnet-report", "cloudnet-report.jar"),
            new CloudNetModule("cloudnet-rest", "cloudnet-rest.jar"),
            new CloudNetModule("cloudnet-smart", "cloudnet-smart.jar"),
            new CloudNetModule("cloudnet-cloudperms", "cloudnet-cloudperms.jar"),
            new CloudNetModule("cloudnet-storage-ftp", "cloudnet-storage-ftp.jar"),
            new CloudNetModule("cloudnet-database-mysql", "cloudnet-database-mysql.jar")
    );

    public static final String[] VERSION_FILE_NAMES = new String[]{
            "cloudnet.jar",
            "cloudnet.cnl",
            "driver.jar",
            "driver.cnl"
    };

    public static final String
            CLOUDNET_SELECTED_VERSION = "cloudnet.launcher.select.version",
            LAUNCHER_DEV_MODE = "cloudnet.launcher.devmode",
            CLOUDNET_REPOSITORY = "cloudnet.repository",
            CLOUDNET_REPOSITORY_GITHUB = "cloudnet.repository.github",
            CLOUDNET_REPOSITORY_AUTO_UPDATE = "cloudnet.auto-update",
            FALLBACK_VERSION = Constants.class.getPackage().getSpecificationVersion();

    private Constants() {
        throw new UnsupportedOperationException();
    }

}