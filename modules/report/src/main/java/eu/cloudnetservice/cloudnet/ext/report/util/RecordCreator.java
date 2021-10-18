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

package eu.cloudnetservice.cloudnet.ext.report.util;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.service.ICloudService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecordCreator {

  private static final Logger LOGGER = LogManager.getLogger(RecordCreator.class);

  private final Path directory;
  private final ICloudService service;

  private RecordCreator(Path directory, ICloudService service) {
    this.directory = directory;
    this.service = service;
  }

  public static @Nullable RecordCreator forService(@NotNull Path baseDirectory, @NotNull ICloudService service) {
    Path directory = baseDirectory.resolve(
      service.getServiceId().getName() + "-" + service.getServiceId().getUniqueId()).normalize().toAbsolutePath();

    if (Files.exists(directory)) {
      return null;
    }

    FileUtils.createDirectoryReported(directory);
    return of(directory, service);
  }

  public static RecordCreator of(@NotNull Path directory, @NotNull ICloudService service) {
    return new RecordCreator(directory, service);
  }

  public void copyLogFiles() {
    try {
      Path targetDirectory = this.directory.resolve("logs");
      FileUtils.createDirectoryReported(targetDirectory);

      if (this.service.getServiceId().getEnvironment() == ServiceEnvironmentType.BUNGEECORD) {
        FileUtils.walkFileTree(this.service.getDirectory(), (root, current) -> {
          try {
            FileUtils.copy(current, targetDirectory.resolve(root.relativize(current)));
          } catch (IOException exception) {
            LOGGER.severe("Exception while copying directories", exception);
          }
        }, false, "proxy.log*");
      } else {
        FileUtils.copyFilesToDirectory(this.service.getDirectory().resolve("logs"), targetDirectory);
      }
    } catch (Exception exception) {
      LOGGER.severe("Exception while creating directory", exception);
    }
  }

  public void writeCachedConsoleLog() {
    try {
      Files.write(directory.resolve("cachedConsoleLog.txt"), this.service.getCachedLogMessages());
    } catch (IOException exception) {
      LOGGER.severe("Unable to write cached console logs", exception);
    }
  }

  public void writeServiceInfoSnapshot() {
    JsonDocument.newDocument("serviceInfoSnapshot", this.service.getServiceInfoSnapshot())
      .append("lastServiceInfoSnapshot", this.service.getLastServiceInfoSnapshot())
      .write(directory.resolve("serviceInfos.json"));
  }

  public void notifySuccess() {
    LOGGER.info(LanguageManager.getMessage("module-report-create-record-success")
      .replace("%service%", this.service.getServiceId().getName())
      .replace("%file%", this.directory.toString()));
  }

}
