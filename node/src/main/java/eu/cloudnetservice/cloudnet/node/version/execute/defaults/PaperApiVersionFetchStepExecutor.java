/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.version.execute.defaults;

import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.node.version.ServiceVersionType;
import eu.cloudnetservice.cloudnet.node.version.execute.InstallStepExecutor;
import eu.cloudnetservice.cloudnet.node.version.information.VersionInstaller;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import kong.unirest.Unirest;
import lombok.NonNull;

public class PaperApiVersionFetchStepExecutor implements InstallStepExecutor {

  private static final String VERSION_LIST_URL = "https://papermc.io/api/v2/projects/%s/versions/%s";
  private static final String DOWNLOAD_URL = "https://papermc.io/api/v2/projects/%s/versions/%s/builds/%d/downloads/%s-%s-%d.jar";
  private static final Type INT_SET_TYPE = TypeToken.getParameterized(Set.class, Integer.class).getType();

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
      // resolve the project name we should use for the api request
      var project = this.decideApiProjectName(installer.serviceVersionType());
      var versionInformation = this.makeRequest(String.format(VERSION_LIST_URL, project, versionGroup));
      // check if there are any builds for the version
      if (versionInformation.contains("builds")) {
        // extract the build numbers from the response
        Set<Integer> builds = versionInformation.get("builds", INT_SET_TYPE);
        // find the highest build number (the newest build)
        var newestBuild = builds.stream().reduce(Math::max);
        // check if there is a build
        if (newestBuild.isPresent()) {
          // set the download url of the service version required in the download step
          int build = newestBuild.get();
          installer.serviceVersion()
            .url(String.format(DOWNLOAD_URL, project, versionGroup, build, project, versionGroup, build));
        } else {
          throw new IllegalStateException(
            "Unable to retrieve latest build for papermc project " + project + " version-group " + versionGroup);
        }
      } else {
        throw new IllegalStateException(
          "Unable to load build information for papermc project " + project + " version-group " + versionGroup);
      }
    }
    // we generated no paths
    return Collections.emptySet();
  }

  private JsonDocument makeRequest(@NonNull String apiUrl) {
    var response = Unirest.get(apiUrl)
      .accept("application/json")
      .asString();

    if (response.isSuccess()) {
      return JsonDocument.fromJsonString(response.getBody());
    }
    return JsonDocument.emptyDocument();
  }

  @NonNull
  private String decideApiProjectName(@NonNull ServiceVersionType type) {
    return type.name().toLowerCase();
  }
}
