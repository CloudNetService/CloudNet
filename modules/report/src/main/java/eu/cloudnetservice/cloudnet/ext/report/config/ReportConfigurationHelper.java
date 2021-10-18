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

import com.google.gson.JsonParseException;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

public class ReportConfigurationHelper {

  private static final Logger LOGGER = LogManager.getLogger(ReportConfigurationHelper.class);

  public static @NotNull ReportConfiguration read(@NotNull Path location) {
    JsonDocument document;
    try {
      document = JsonDocument.newDocumentExceptionally(location);
    } catch (Exception exception) {
      throw new JsonParseException("Exception while parsing report configuration. Your configuration is invalid.");
    }

    if (document.contains("savingRecords")) {
      // we found an old config - convert the config
      LOGGER.warning("Detected old report configuration file, running conversation...");
      // save old configuration file
      document.write(location.getParent().resolve("config.json.old"));
      // convert config and rewrite the file
      ReportConfiguration configuration = convertConfiguration(document);
      write(configuration, location);

      return configuration;
    }
    // check if we need to create a new configuration
    if (document.isEmpty()) {
      write(ReportConfiguration.DEFAULT, location);
      return ReportConfiguration.DEFAULT;
    }
    // the document has a configuration
    return document.toInstanceOf(ReportConfiguration.class);
  }

  public static void write(@NotNull ReportConfiguration configuration, @NotNull Path location) {
    JsonDocument.newDocument(configuration).write(location);
  }

  private static ReportConfiguration convertConfiguration(@NotNull JsonDocument document) {
    boolean saveRecords = document.getBoolean("savingRecords", true);
    Path recordDestination = document.get("recordDestinationDirectory", Path.class, Paths.get("records"));
    Collection<PasteService> pasteServices = Collections.singletonList(
      new PasteService("default", document.getString("pasteServerUrl", "https://just-paste.it")));
    long serviceLifetime = document.getLong("serviceLifetimeLogPrint", 5000L);

    // since early 3.5 a custom date format is supported, migrate this too
    String dateFormat = document.getString("dateFormat", "yyyy-MM-dd");

    return new ReportConfiguration(saveRecords, recordDestination, serviceLifetime, dateFormat, pasteServices);
  }

}
