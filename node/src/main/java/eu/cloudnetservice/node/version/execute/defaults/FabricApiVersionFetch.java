/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.version.execute.defaults;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import eu.cloudnetservice.node.version.execute.InstallStepExecutor;
import eu.cloudnetservice.node.version.information.VersionInstaller;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import kong.unirest.Unirest;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class FabricApiVersionFetch implements InstallStepExecutor {

  private static final String FABRIC_INSTALLER_URL = "https://meta.fabricmc.net/v2/versions/installer";

  @Override
  public @NonNull Set<Path> execute(
    @NonNull VersionInstaller installer,
    @NonNull Path workingDirectory,
    @NonNull Set<Path> files
  ) throws IOException {
    // check if we need the fabric api to fetch the version
    var enabled = installer.serviceVersion().properties().getBoolean("fetchOverFabricApi");
    if (enabled) {
      var element = this.makeRequest();
      if (element == null) {
        // response is invalid, fail
        throw new IllegalStateException("Unable to retrieve latest installer for fabric");
      }

      for (var jsonElement : element.getAsJsonArray()) {
        var jsonObject = jsonElement.getAsJsonObject();
        // only allow stable fabric versions
        if (jsonObject.get("stable").getAsBoolean()) {
          // set the fabric loader download url
          installer.serviceVersion().url(jsonObject.get("url").getAsString());
          // we don't have any paths
          return Collections.emptySet();
        }
      }
      // could not find any stable fabric version
      throw new IllegalStateException("Unable to retrieve latest installer for fabric (no stable version found)");
    }
    // we don't have any paths
    return Collections.emptySet();
  }

  private @Nullable JsonElement makeRequest() {
    var response = Unirest.get(FabricApiVersionFetch.FABRIC_INSTALLER_URL)
      .accept("application/json")
      .asString();
    if (response.isSuccess()) {
      return JsonParser.parseString(response.getBody());
    }
    return null;
  }
}
