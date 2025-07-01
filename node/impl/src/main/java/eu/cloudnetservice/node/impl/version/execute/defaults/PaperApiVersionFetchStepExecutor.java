/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.version.execute.defaults;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.node.impl.version.execute.InstallStepExecutor;
import eu.cloudnetservice.node.impl.version.information.VersionInstaller;
import eu.cloudnetservice.utils.base.StringUtil;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import kong.unirest.core.Unirest;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaperApiVersionFetchStepExecutor implements InstallStepExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(PaperApiVersionFetchStepExecutor.class);
  private static final String LATEST_BUILD_URL = "https://fill.papermc.io/v3/projects/%s/versions/%s/builds/latest";

  @Override
  public @NonNull Set<Path> execute(
    @NonNull VersionInstaller installer,
    @NonNull Path workingDirectory,
    @NonNull Set<Path> inputPaths
  ) {
    // check if we need to fetch using the paper api
    var enabled = installer.serviceVersion().properties().getBoolean("fetchOverPaperApi");
    var versionGroup = installer.serviceVersion().properties().getString("versionGroup");
    if (enabled && versionGroup != null) {
      // resolve the download url to the latest available version for the version group
      var projectName = StringUtil.toLower(installer.serviceVersionType().name());
      var latestBuildUri = String.format(LATEST_BUILD_URL, projectName, versionGroup);
      var latestBuildInfo = this.makeRequest(latestBuildUri);
      if (latestBuildInfo.empty()) {
        throw new IllegalStateException(String.format(
          "Unable to load latest build info from papermc api [project=%s, versionGroup=%s]",
          projectName, versionGroup));
      }

      LOGGER.debug(
        "Latest build available in papermc api for [project={}, versionGroup={}] is {} ({})",
        projectName, versionGroup, latestBuildInfo.getInt("id"), latestBuildInfo.getString("time"));

      // get the available downloads (spigot and mojang mapped server jars)
      var downloads = latestBuildInfo.readDocument("downloads");
      var defaultMappedDownload = downloads.readDocument("server:default", null);
      var mojangMappedDownload = downloads.readDocument("server:mojang", null);
      if (defaultMappedDownload == null && mojangMappedDownload == null) {
        throw new IllegalStateException(String.format(
          "Unable to resolve download info from papermc api response for [project=%s, versionGroup=%s], got %s",
          projectName, versionGroup, latestBuildInfo.serializeToString()));
      }

      // resolve the url to use for downloading the service version
      var download = Objects.requireNonNullElse(defaultMappedDownload, mojangMappedDownload);
      var downloadUrl = download.getString("url");
      if (downloadUrl == null) {
        throw new IllegalStateException(String.format(
          "No download url provided by papermc api for [project=%s, versionGroup=%s, build=%s]",
          projectName, versionGroup, latestBuildInfo.getInt("id")));
      }

      // update the download url for the service version
      LOGGER.debug(
        "Resolved download url from papermc api for [project={}, versionGroup={}, build={}] to {}",
        projectName, versionGroup, latestBuildInfo.getInt("id"), downloadUrl);
      installer.serviceVersion().url(downloadUrl);
    }

    return Set.of();
  }

  /**
   * Makes a single api request to the given uri, returning the parsed response body on success. An empty document is
   * returned in case the request was not successful.
   *
   * @param uri the uri to send a get request to.
   * @return the parsed response body in the form of a document on success, an empty document on failure.
   * @throws NullPointerException if the given uri is null.
   */
  private @NonNull Document makeRequest(@NonNull String uri) {
    var response = Unirest.get(uri).accept("application/json").asString();
    return response.isSuccess() ? DocumentFactory.json().parse(response.getBody()) : Document.emptyDocument();
  }
}
