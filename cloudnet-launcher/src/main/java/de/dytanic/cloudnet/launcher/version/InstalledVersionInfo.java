package de.dytanic.cloudnet.launcher.version;

import java.nio.file.Path;

public class InstalledVersionInfo extends DefaultVersionInfo implements VersionInfo {

  public InstalledVersionInfo(Path targetDirectory, String gitHubRepository) {
    this.targetDirectory = targetDirectory;
    this.gitHubRepository = gitHubRepository;

    String versionSpecification = targetDirectory.getFileName().toString();

    String[] versionParts = versionSpecification.split("-");

    if (versionParts.length > 1) {
      this.appVersion = versionParts[0] + "-" + versionParts[1];

      try {
        this.releaseTimestamp = versionParts.length > 2 ? Long.parseLong(versionParts[2]) : -1;
      } catch (NumberFormatException ignored) {
        this.releaseTimestamp = -1;
      }
    }

  }

}
