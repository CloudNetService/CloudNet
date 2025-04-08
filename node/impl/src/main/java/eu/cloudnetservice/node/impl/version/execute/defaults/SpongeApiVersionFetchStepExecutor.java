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

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.node.impl.version.execute.InstallStepExecutor;
import eu.cloudnetservice.node.impl.version.information.VersionInstaller;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import kong.unirest.core.HttpStatus;
import kong.unirest.core.Unirest;
import lombok.NonNull;

public class SpongeApiVersionFetchStepExecutor implements InstallStepExecutor {

  private static final String VERSION_DOWNLOAD_URL = "https://repo.spongepowered.org/repository/maven-releases/%s/%s/%s/%s-%s-universal.jar";
  private static final String VERSION_FETCH_URL = "https://dl-api-new.spongepowered.org/api/v2/groups/%s/artifacts/%s/versions?tags=minecraft:%s&offset=0&limit=1";

  @Override
  public @NonNull Set<Path> execute(
    @NonNull VersionInstaller installer,
    @NonNull Path workingDirectory,
    @NonNull Set<Path> files
  ) throws IOException {
    // check if we need to fetch using the sponge api
    var properties = installer.serviceVersion().properties();
    var enabled = properties.getBoolean("fetchOverSpongeApi");
    if (enabled) {
      // get all properties which we need to download the version
      var artifact = properties.getString("artifact");
      var mcVersion = properties.getString("mcVersion");
      var groupId = properties.getString("group", "org.spongepowered");

      // build the url and get the data from the url
      var fetchUrl = String.format(VERSION_FETCH_URL, groupId, artifact, mcVersion);
      var jsonResponse = Unirest.get(fetchUrl)
        .accept("application/json")
        .asObject(response -> {
          if (response.getStatus() == HttpStatus.OK) {
            return DocumentFactory.json().parse(response.getContentAsString());
          } else {
            return Document.newJsonDocument();
          }
        }).getBody();

      // check if the document contains any artifacts
      var artifacts = jsonResponse.readDocument("artifacts");
      if (!artifacts.empty()) {
        // get the first key - it is the version we need to download
        var versionKey = Iterables.getFirst(artifacts.keys(), null);
        if (versionKey != null) {
          // version if present - set the download url of the version
          var downloadUrl = String.format(
            VERSION_DOWNLOAD_URL,
            groupId.replace('.', '/'),
            artifact,
            versionKey,
            artifact,
            versionKey);
          installer.serviceVersion().url(downloadUrl);
        }
      }
    }

    // we generated no paths
    return Set.of();
  }
}
