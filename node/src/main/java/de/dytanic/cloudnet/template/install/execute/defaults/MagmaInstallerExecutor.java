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

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;

public class MagmaInstallerExecutor extends BuildStepExecutor {

  private static final Logger LOGGER = LogManager.logger(MagmaInstallerExecutor.class);

  @Override
  protected int buildProcessAndWait(@NonNull List<String> arguments, @NonNull Path workingDir) {
    return super.buildProcessAndWait(
      arguments,
      workingDir,
      (line, process) -> {
        // check if the line starts with "Would you like to accept the EULA?", kill the process then (only for 1.12 install)
        if (line.startsWith("Would you like to accept the EULA?")) {
          process.destroyForcibly();
        } else {
          LOGGER.info(String.format("[Template Installer]: %s", line));
        }
      },
      (line, $) -> LOGGER.warning(String.format("[Template Installer]: %s", line))
    );
  }
}
