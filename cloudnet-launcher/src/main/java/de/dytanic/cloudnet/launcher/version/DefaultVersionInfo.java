package de.dytanic.cloudnet.launcher.version;

import java.nio.file.Path;

public abstract class DefaultVersionInfo implements VersionInfo {

  protected String repositoryVersion;
  protected String appVersion;

  protected String gitHubRepository;

  protected long releaseTimestamp;

  protected Path targetDirectory;

  @Override
  public String getRepositoryVersion() {
    return this.repositoryVersion;
  }

  @Override
  public String getCurrentVersion() {
    return this.appVersion;
  }

  @Override
  public String getGitHubRepository() {
    return this.gitHubRepository;
  }

  @Override
  public long getReleaseTimestamp() {
    return this.releaseTimestamp;
  }

  @Override
  public Path getTargetDirectory() {
    return this.targetDirectory;
  }
}
