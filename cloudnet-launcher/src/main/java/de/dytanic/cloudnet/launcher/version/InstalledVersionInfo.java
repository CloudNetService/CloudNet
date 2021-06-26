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

package de.dytanic.cloudnet.launcher.version;

import java.nio.file.Path;

public class InstalledVersionInfo extends DefaultVersionInfo implements VersionInfo {

  public InstalledVersionInfo(Path targetDirectory, String gitHubRepository) {
    this.targetDirectory = targetDirectory;
    this.gitHubRepository = gitHubRepository;

    String versionSpecification = targetDirectory.getFileName().toString();

    String[] versionParts = versionSpecification.split("-");

    if (versionParts.length > 1) {
      this.appVersion = versionParts[0] + "-" + versionParts[1];

      try {
        this.releaseTimestamp = versionParts.length > 2 ? Long.parseLong(versionParts[2]) : -1;
      } catch (NumberFormatException ignored) {
        this.releaseTimestamp = -1;
      }
    }

  }

}
