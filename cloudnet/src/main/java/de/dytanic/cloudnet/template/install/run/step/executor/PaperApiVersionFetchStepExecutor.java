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

package de.dytanic.cloudnet.template.install.run.step.executor;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import de.dytanic.cloudnet.template.install.run.InstallInformation;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class PaperApiVersionFetchStepExecutor implements InstallStepExecutor {

  private static final String VERSION_LIST_URL = "https://papermc.io/api/v2/projects/%s/versions/%s";
  private static final String DOWNLOAD_URL = "https://papermc.io/api/v2/projects/%s/versions/%s/builds/%d/downloads/%s-%s-%d.jar";
  private static final Type INT_SET_TYPE = TypeToken.getParameterized(Set.class, Integer.class).getType();

  @Override
  public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory,
    @NotNull Set<Path> inputPaths) throws IOException {
    boolean enabled = installInformation.getServiceVersion().getProperties().getBoolean("fetchOverPaperApi");
    String versionGroup = installInformation.getServiceVersion().getProperties().getString("versionGroup");
    if (enabled && versionGroup != null) {
      String project = this.decideApiProjectName(installInformation.getServiceVersionType());
      JsonDocument versionInformation = this.makeRequest(String.format(VERSION_LIST_URL, project, versionGroup));
      if (versionInformation.contains("builds")) {
        Set<Integer> builds = versionInformation.get("builds", INT_SET_TYPE);
        Optional<Integer> newestBuild = builds.stream().reduce(Math::max);
        if (newestBuild.isPresent()) {
          int build = newestBuild.get();
          installInformation.getServiceVersion()
            .setUrl(String.format(DOWNLOAD_URL, project, versionGroup, build, project, versionGroup, build));
        } else {
          throw new IllegalStateException(
            "Unable to retrieve latest build for papermc project " + project + " version-group " + versionGroup);
        }
      } else {
        throw new IllegalStateException(
          "Unable to load build information for papermc project " + project + " version-group " + versionGroup);
      }
    }

    return Collections.emptySet();
  }

  private JsonDocument makeRequest(@NotNull String apiUrl) {
    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
      connection.setRequestProperty("User-Agent",
        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
      connection.setRequestProperty("accepts", "application/json");
      connection.connect();
      if (connection.getResponseCode() == 200) {
        try (InputStream stream = connection.getInputStream()) {
          return JsonDocument.newDocument(stream);
        }
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    return JsonDocument.EMPTY;
  }

  @NotNull
  private String decideApiProjectName(@NotNull ServiceVersionType type) {
    switch (type.getName().toLowerCase()) {
      case "paperspigot":
        return "paper";
      case "waterfall":
        return "waterfall";
      case "travertine":
        return "travertine";
      case "velocity":
        return "velocity";
      default:
        throw new IllegalStateException("Unsupported paper api project type " + type.getName());
    }
  }
}
