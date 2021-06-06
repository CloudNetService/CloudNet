/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
