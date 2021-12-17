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

package eu.cloudnetservice.cloudnet.ext.report.config;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

public final class ReportConfigurationHelper {

  private static final Logger LOGGER = LogManager.logger(ReportConfigurationHelper.class);

  /**
   * Reads a {@link ReportConfiguration} from the file at the given location. If an old version of the config is
   * detected a conversion is done and the file is rewritten. In the case, that the file is empty, the default {@link
   * ReportConfiguration#builder()} configuration is written to the file.
   *
   * @param location the location of the file to read from.
   * @return the read {@link ReportConfiguration} configuration.
   */
  public static @NotNull ReportConfiguration read(@NotNull Path location) {
    var document = JsonDocument.newDocument(location);

    if (document.contains("savingRecords")) {
      // we found an old config - convert the config
      LOGGER.warning("Detected old report configuration file, running conversation...");
      // save old configuration file
      document.write(location.getParent().resolve("config.json.old"));
      // convert config and rewrite the file
      var configuration = convertConfiguration(document);

      return write(configuration, location);
    }
    // check if we need to create a new configuration
    if (document.isEmpty()) {
      return write(ReportConfiguration.builder().build(), location);
    }
    // the document has a configuration
    return document.toInstanceOf(ReportConfiguration.class);
  }

  /**
   * Wraps the {@link ReportConfiguration} configuration into a {@link JsonDocument} and writes it to the file at the
   * given path.
   *
   * @param configuration the configuration to write.
   * @param location      the location to save the configuration to.
   */
  public static @NotNull ReportConfiguration write(@NotNull ReportConfiguration configuration, @NotNull Path location) {
    JsonDocument.newDocument(configuration).write(location);
    return configuration;
  }

  /**
   * Converts the old configuration (<= 3.4.0) to the new format.
   *
   * @param document the document containing the old configuration.
   * @return the new converted configuration.
   */
  private static ReportConfiguration convertConfiguration(@NotNull JsonDocument document) {
    var saveRecords = document.getBoolean("savingRecords", true);
    var recordDestination = document.get("recordDestinationDirectory", Path.class, Paths.get("records"));
    var pasteServices = Collections.singletonList(
      new PasteService("default", document.getString("pasteServerUrl", "https://just-paste.it")));
    var serviceLifetime = document.getLong("serviceLifetimeLogPrint", 5000L);

    // since early 3.5 a custom date format is supported, migrate this too
    var dateFormat = document.getString("dateFormat", "yyyy-MM-dd");

    return new ReportConfiguration(saveRecords,
      false,
      recordDestination,
      serviceLifetime,
      new SimpleDateFormat(dateFormat),
      pasteServices);
  }

}
