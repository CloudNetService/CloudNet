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

import de.dytanic.cloudnet.template.install.run.InstallInformation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class DeployStepExecutor implements InstallStepExecutor {

  @Override
  public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory,
    @NotNull Set<Path> inputPaths) throws IOException {
    for (Path path : inputPaths) {
      if (Files.isDirectory(path)) {
        continue;
      }

      String relativePath = workingDirectory.relativize(path).toString().replace("\\", "/");

      try (OutputStream outputStream = installInformation.getTemplateStorage()
        .newOutputStream(installInformation.getServiceTemplate(), relativePath)) {
        Files.copy(path, Objects.requireNonNull(outputStream));
      }
    }
    return inputPaths;
  }

}
