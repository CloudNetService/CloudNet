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

package de.dytanic.cloudnet.launcher.version.update.jenkins;

import de.dytanic.cloudnet.launcher.LauncherUtils;
import de.dytanic.cloudnet.launcher.version.update.Updater;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarInputStream;

public class JenkinsUpdater implements Updater {

  private final String jenkinsJobUrl;

  private JenkinsBuild jenkinsBuild;

  private String appVersion;

  private String gitHubRepository;

  private long releaseTimestamp;

  private Path targetDirectory;

  public JenkinsUpdater(String jenkinsJobUrl) {
    this.jenkinsJobUrl = jenkinsJobUrl.endsWith("/") ? jenkinsJobUrl : jenkinsJobUrl + "/";
  }

  @Override
  public boolean init(Path versionDirectory, String githubRepository) {
    this.gitHubRepository = githubRepository;

    try (InputStream inputStream = LauncherUtils.readFromURL(this.jenkinsJobUrl + "lastSuccessfulBuild/api/json");
      Reader reader = new InputStreamReader(inputStream)) {

      this.jenkinsBuild = LauncherUtils.GSON.fromJson(reader, JenkinsBuild.class);

      String[] versionParts = this.readImplementationVersion(
        this.jenkinsBuild.getArtifacts().stream()
          .filter(artifact -> artifact.getFileName().equalsIgnoreCase("launcher.jar"))
          .findFirst()
          .orElse(null)
      ).split("-");

      this.appVersion = versionParts[0] + "-" + versionParts[1];
      this.releaseTimestamp = this.jenkinsBuild.getTimestamp();

      this.targetDirectory = versionDirectory.resolve(this.getFullVersion());

      return true;
    } catch (Exception exception) {
      exception.printStackTrace();
    }

    return false;
  }

  private String readImplementationVersion(JenkinsBuild.BuildArtifact artifact) throws Exception {
    try (JarInputStream jarInputStream = new JarInputStream(
      LauncherUtils.readFromURL(this.getArtifactDownloadURL(artifact)))) {
      return jarInputStream.getManifest().getMainAttributes().getValue("Implementation-Version");
    }
  }

  private String getArtifactDownloadURL(JenkinsBuild.BuildArtifact buildArtifact) {
    return this.jenkinsJobUrl + "lastSuccessfulBuild/artifact/" + buildArtifact.getRelativePath();
  }

  @Override
  public boolean installFile(String name, Path path) {
    try {
      if (!Files.exists(path)) {
        Files.createFile(path);
      }

      Files.createDirectories(path.getParent());

      JenkinsBuild.BuildArtifact buildArtifact = this.jenkinsBuild.getArtifacts().stream()
        .filter(artifact -> artifact.getFileName().equalsIgnoreCase(name))
        .findFirst()
        .orElseThrow(() -> new NullPointerException(String.format("Unable to find file %s on the jenkins!", name)));

      try (InputStream inputStream = LauncherUtils.readFromURL(this.getArtifactDownloadURL(buildArtifact))) {
        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
      }

      return true;

    } catch (Exception exception) {
      exception.printStackTrace();
    }

    return false;
  }

  @Override
  public String getRepositoryVersion() {
    return null;
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
