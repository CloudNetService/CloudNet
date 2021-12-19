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

package de.dytanic.cloudnet.template.install.execute.defaults;

import de.dytanic.cloudnet.console.animation.progressbar.ConsoleProgressWrappers;
import de.dytanic.cloudnet.template.install.InstallInformation;
import de.dytanic.cloudnet.template.install.execute.InstallStepExecutor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.NonNull;

public class DownloadStepExecutor implements InstallStepExecutor {

  @Override
  public @NonNull Set<Path> execute(
    @NonNull InstallInformation installInformation,
    @NonNull Path workingDirectory,
    @NonNull Set<Path> inputPaths
  ) throws IOException {
    var targetPath = workingDirectory.resolve(
      Path.of(installInformation.serviceVersionType().name() + ".jar"));

    ConsoleProgressWrappers.wrapDownload(
      installInformation.serviceVersion().url(),
      stream -> Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING));
    return new HashSet<>(Collections.singleton(targetPath));
  }
}
