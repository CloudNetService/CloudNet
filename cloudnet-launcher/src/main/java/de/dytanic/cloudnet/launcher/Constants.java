package de.dytanic.cloudnet.launcher;

import de.dytanic.cloudnet.launcher.util.CloudNetModule;
import java.util.Arrays;
import java.util.List;

public final class Constants {

  private Constants() {
    throw new UnsupportedOperationException();
  }

  public static final List<CloudNetModule> DEFAULT_MODULES = Arrays.asList(
    new CloudNetModule("cloudnet-bridge", "cloudnet-bridge.jar"),
    new CloudNetModule("cloudnet-signs", "cloudnet-signs.jar"),
    new CloudNetModule("cloudnet-syncproxy", "cloudnet-syncproxy.jar"),
    new CloudNetModule("cloudnet-cloudflare", "cloudnet-cloudflare.jar"),
    new CloudNetModule("cloudnet-report", "cloudnet-report.jar"),
    new CloudNetModule("cloudnet-rest", "cloudnet-rest.jar"),
    new CloudNetModule("cloudnet-smart", "cloudnet-smart.jar"),
    new CloudNetModule("cloudnet-cloudperms", "cloudnet-cloudperms.jar"),
    new CloudNetModule("cloudnet-storage-ftp", "cloudnet-storage-ftp.jar"),
    new CloudNetModule("cloudnet-database-mysql",
      "cloudnet-database-mysql.jar")
  );

  public static final String
    CLOUDNET_SELECTED_VERSION = "cloudnet.launcher.select.version",
    CLOUDNET_REPOSITORY = "cloudnet.repository",
    CLOUDNET_REPOSITORY_TYPE = "cloudnet.repository.type",
    CLOUDNET_REPOSITORY_AUTO_UPDATE = "cloudnet.auto-update",
    CLOUDNET_MODULES_AUTO_UPDATE_WITH_EMBEDDED = "cloudnet.auto-update.with-embedded",
    LAUNHCER_CONFIG = System
      .getProperty("cloudnet.launcher.config", "launcher.cnl"),
    LAUNHCER_DIR = System.getProperty("cloudnet.launcher.dir", "launcher"),
    FALLBACK_VERSION = Constants.class.getPackage().getSpecificationVersion(),
    INTERNAL_CLOUDNET_JAR_FILE_NAME = "cloudnet.jar", INTERNAL_DRIVER_JAR_FILE_NAME = "driver.jar",
    INTERNAL_CLOUDNET_CNL_FILE_NAME = "cloudnet.cnl", INTERNAL_DRIVER_CNL_FILE_NAME = "driver.cnl";
}