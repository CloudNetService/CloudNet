package de.dytanic.cloudnet.launcher.version;

import java.nio.file.Path;

public interface VersionInfo {

  String getRepositoryVersion();

  String getCurrentVersion();

  String getGitHubRepository();

  long getReleaseTimestamp();

  Path getTargetDirectory();

  default String getFullVersion() {
    return this.getCurrentVersion() + "-" + this.getReleaseTimestamp();
  }

}
