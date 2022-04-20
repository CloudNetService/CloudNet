/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.report.util;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.node.service.CloudService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Constructs a new record with the already resolved directory for the record.
 *
 * @param directory the directory to store the record files in.
 * @param service   the service that this record is used for.
 */
public record RecordMaker(@NonNull Path directory, @NonNull CloudService service) {

  private static final Logger LOGGER = LogManager.logger(RecordMaker.class);

  /**
   * Constructs a new Record with the given baseDirectory. The resulting directory is resolved with the name and the
   * uniqueId of the given service e.g. Lobby-1-857679dc-a12e-459e-95a7-5577dd98313e.
   *
   * @param baseDirectory the baseDirectory to create the sub folder of the service in.
   * @param service       the service that this record is used for.
   * @return the new Record for the service, null if the directory for the services already exists.
   * @throws NullPointerException if the given directory or service is null.
   */
  public static @Nullable RecordMaker forService(@NonNull Path baseDirectory, @NonNull CloudService service) {
    var directory = baseDirectory.resolve(service.serviceId().name() + "-" + service.serviceId().uniqueId())
      .normalize()
      .toAbsolutePath();

    if (Files.exists(directory)) {
      return null;
    }

    FileUtil.createDirectory(directory);
    return of(directory, service);
  }

  /**
   * Constructs a new record with the already resolved directory for the record.
   *
   * @param directory the directory to store the record files in.
   * @param service   the service that this record is used for.
   * @return the new Record for the service.
   * @throws NullPointerException if the given directory or service is null.
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
      FileUtil.createDirectory(targetDirectory);

      if (this.service.serviceId().environment().equals(ServiceEnvironmentType.BUNGEECORD)) {
        FileUtil.walkFileTree(this.service.directory(),
          (root, current) -> FileUtil.copy(current, targetDirectory.resolve(root.relativize(current))), false,
          "proxy.log*");
      } else {
        FileUtil.copyDirectory(this.service.directory().resolve("logs"), targetDirectory);
      }
    } catch (Exception exception) {
      LOGGER.severe("Exception while creating directory", exception);
    }
  }

  /**
   * Creates a new log file containing the cached console log of the service.
   */
  public void writeCachedConsoleLog() {
    try {
      Files.write(this.directory.resolve("cachedConsoleLog.txt"), this.service.cachedLogMessages());
    } catch (IOException exception) {
      LOGGER.severe("Unable to write cached console logs", exception);
    }
  }

  /**
   * Creates a new file in the record directory containing the current and last
   * {@link eu.cloudnetservice.driver.service.ServiceInfoSnapshot} of the service.
   */
  public void writeServiceInfoSnapshot() {
    JsonDocument.newDocument("serviceInfoSnapshot", this.service.serviceInfo())
      .append("lastServiceInfoSnapshot", this.service.lastServiceInfoSnapshot())
      .write(this.directory.resolve("ServiceInfoSnapshots.json"));
  }

  /**
   * Sends a message to the cloudnet node about the newly created record.
   */
  public void notifySuccess() {
    LOGGER.info(I18n.trans("module-report-create-record-success",
      this.service.serviceId().name(),
      this.directory.toString()));
  }
}
