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

import de.dytanic.cloudnet.launcher.version.InstalledVersionInfo;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class FallbackUpdater extends InstalledVersionInfo implements Updater {

  public FallbackUpdater(Path targetDirectory, String gitHubRepository) {
    super(targetDirectory, gitHubRepository);
  }

  @Override
  public boolean init(Path versionDirectory, String githubRepository) {
    // do nothing, we already have all information necessary
    return true;
  }

  @Override
  public boolean installFile(String name, Path path) {
    try {
      if (!Files.exists(path)) {
        Files.createFile(path);
      }

      Files.createDirectories(path.getParent());

      try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(name)) {
        Files.copy(
          Objects
            .requireNonNull(inputStream, String.format("Fallback file %s not found, is the launcher corrupted?", name)),
          path,
          StandardCopyOption.REPLACE_EXISTING
        );
      }

      return true;

    } catch (Exception exception) {
      exception.printStackTrace();
    }

    return false;
  }


}
