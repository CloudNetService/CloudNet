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

import eu.cloudnetservice.node.impl.console.animation.progressbar.ConsoleProgressWrappers;
import eu.cloudnetservice.node.impl.version.execute.InstallStepExecutor;
import eu.cloudnetservice.node.impl.version.information.VersionInstaller;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.NonNull;

public class DownloadStepExecutor implements InstallStepExecutor {

  private final ConsoleProgressWrappers consoleProgressWrappers;

  @Inject
  public DownloadStepExecutor(@NonNull ConsoleProgressWrappers consoleProgressWrappers) {
    this.consoleProgressWrappers = consoleProgressWrappers;
  }

  @Override
  public @NonNull Set<Path> execute(
    @NonNull VersionInstaller installer,
    @NonNull Path workingDirectory,
    @NonNull Set<Path> inputPaths
  ) throws IOException {
    var targetPath = workingDirectory.resolve(installer.serviceVersionType().name() + ".jar");

    this.consoleProgressWrappers.wrapDownload(
      installer.serviceVersion().url(),
      stream -> Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING));
    return new HashSet<>(Collections.singleton(targetPath));
  }
}
