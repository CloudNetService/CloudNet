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
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.service.CloudService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Constructs a new record with the already resolved directory for the record.
 *
 * @param directory the directory to store the record files in
 * @param service   the service that this record is used for
 */
public record RecordMaker(@NonNull Path directory, @NonNull CloudService service) {

  private static final Logger LOGGER = LogManager.logger(RecordMaker.class);

  /**
   * Constructs a new Record with the given baseDirectory, the resulting directory is resolved with the name and the
   * uniqueId of the given service e.g. Lobby-1-857679dc-a12e-459e-95a7-5577dd98313e.
   *
   * @param baseDirectory the baseDirectory to create the sub folder of the service in
   * @param service       the service that this record is used for
   * @return the new Record for the service, null if the directory for the services already exists
   */
  public static @Nullable RecordMaker forService(@NonNull Path baseDirectory, @NonNull CloudService service) {
    var directory = baseDirectory.resolve(
      service.serviceId().name() + "-" + service.serviceId().uniqueId()).normalize().toAbsolutePath();

    if (Files.exists(directory)) {
      return null;
    }

    FileUtils.createDirectory(directory);
    return of(directory, service);
  }

  /**
   * Constructs a new record with the already resolved directory for the record.
   *
   * @param directory the directory to store the record files in
   * @param service   the service that this record is used for
   * @return the new Record for the service
   */
  public static RecordMaker of(@NonNull Path directory, @NonNull CloudService service) {
    return new RecordMaker(directory, service);
  }

  /**
   * Copies the "logs" folder from the service into the directory of this record. When running this for a BungeeCord
   * service "proxy.log.*" is copied.
   */
  public void copyLogFiles() {
    try {
      var targetDirectory = this.directory.resolve("logs");
      FileUtils.createDirectory(targetDirectory);

      if (this.service.serviceId().environment().equals(ServiceEnvironmentType.BUNGEECORD)) {
        FileUtils.walkFileTree(this.service.directory(),
          (root, current) -> FileUtils.copy(current, targetDirectory.resolve(root.relativize(current))), false,
          "proxy.log*");
      } else {
        FileUtils.copyDirectory(this.service.directory().resolve("logs"), targetDirectory);
      }
    } catch (Exception exception) {
      LOGGER.severe("Exception while creating directory", exception);
    }
  }

  /**
   * Creates a new file ("cachedConsoleLog.txt") with the last cached log lines of the service.
   */
  public void writeCachedConsoleLog() {
    try {
      Files.write(this.directory.resolve("cachedConsoleLog.txt"), this.service.cachedLogMessages());
    } catch (IOException exception) {
      LOGGER.severe("Unable to write cached console logs", exception);
    }
  }

  /**
   * Creates a new file ("ServiceInfoSnapshots.json") with the previous and current {@link
   * de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot} ServiceInfoSnapshot of the service.
   */
  public void writeServiceInfoSnapshot() {
    JsonDocument.newDocument("serviceInfoSnapshot", this.service.serviceInfo())
      .append("lastServiceInfoSnapshot", this.service.lastServiceInfoSnapshot())
      .write(this.directory.resolve("ServiceInfoSnapshots.json"));
  }

  /**
   * Sends a message to the node, that the record was created successfully.
   */
  public void notifySuccess() {
    LOGGER.info(I18n.trans("module-report-create-record-success")
      .replace("%service%", this.service.serviceId().name())
      .replace("%file%", this.directory.toString()));
  }
}
