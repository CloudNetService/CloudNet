package de.dytanic.cloudnet.launcher.version.update;

import de.dytanic.cloudnet.launcher.LauncherUtils;
import de.dytanic.cloudnet.launcher.version.DefaultVersionInfo;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class RepositoryUpdater extends DefaultVersionInfo implements Updater {

  private final String url;

  public RepositoryUpdater(String url) {
    this.url = url.endsWith("/") ? url : url + "/";
  }

  @Override
  public boolean init(Path versionDirectory, String githubRepository) {
    this.gitHubRepository = githubRepository;

    try (InputStream inputStream = LauncherUtils.readFromURL(this.url + "repository")) {
      Properties properties = new Properties();
      properties.load(inputStream);

      if (properties.containsKey("app-version")) {
        this.repositoryVersion = properties.getProperty("repository-version");
        this.appVersion = properties.getProperty("app-version");
        this.releaseTimestamp = Long.parseLong(properties.getProperty("release-timestamp", "-1"));

        this.targetDirectory = versionDirectory.resolve(this.getFullVersion());

        return true;
      }

    } catch (Exception exception) {
      exception.printStackTrace();
    }

    return false;
  }

  @Override
  public boolean installFile(String name, Path path) {
    try {
      if (!Files.exists(path)) {
        Files.createFile(path);
      }

      Files.createDirectories(path.getParent());

      try (InputStream inputStream = LauncherUtils.readFromURL(this.url + "versions/" + this.appVersion + "/" + name)) {
        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
      }

      return true;

    } catch (Exception exception) {
      exception.printStackTrace();
    }

    return false;
  }

}
