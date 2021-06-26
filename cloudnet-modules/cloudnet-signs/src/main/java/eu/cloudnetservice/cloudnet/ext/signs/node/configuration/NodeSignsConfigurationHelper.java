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

package eu.cloudnetservice.cloudnet.ext.signs.node.configuration;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationReaderAndWriter;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignLayoutConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignGroupConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayoutsHolder;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public final class NodeSignsConfigurationHelper {

  private NodeSignsConfigurationHelper() {
    throw new UnsupportedOperationException();
  }

  public static void write(@NotNull SignsConfiguration configuration, @NotNull Path path) {
    JsonDocument.newDocument(configuration).write(path);
  }

  public static SignsConfiguration read(@NotNull Path path) {
    JsonDocument configurationDocument = JsonDocument.newDocument(path);
    if (configurationDocument.contains("config")) {
      // old document - run conversation now
      CloudNetDriver.getInstance().getLogger()
        .warning("Detected old signs configuration file, running conversation...");
      // save old configuration as a backup before backup
      configurationDocument.write(path.getParent().resolve("config.json.old"));
      // write the new configuration file
      SignsConfiguration configuration = convertOldConfiguration(configurationDocument, path);
      write(configuration, path);
      // notify that the convert was successful
      CloudNetDriver.getInstance().getLogger().info("Successfully converted the old signs configuration file");
      // no need to load the configuration from the file again
      return configuration;
    }
    // check if the configuration file already exists
    if (configurationDocument.isEmpty()) {
      // create a new configuration entry
      SignsConfiguration configuration = SignsConfiguration.createDefaultJava("Lobby");
      write(configuration, path);
      return configuration;
    }
    // the document contains a configuration
    return configurationDocument.toInstanceOf(SignsConfiguration.class);
  }

  // convert of old configuration file
  private static SignsConfiguration convertOldConfiguration(@NotNull JsonDocument document, @NotNull Path path) {
    // read as old configuration file
    SignConfiguration oldConfiguration = SignConfigurationReaderAndWriter.read(document, path);
    // create new configuration from it
    return new SignsConfiguration(
      convertMessages(oldConfiguration.getMessages()),
      oldConfiguration.getConfigurations().stream().map(oldEntry -> new SignConfigurationEntry(
        oldEntry.getTargetGroup(),
        oldEntry.isSwitchToSearchingWhenServiceIsFull(),
        new SignConfigurationEntry.KnockbackConfiguration(
          oldEntry.getKnockbackDistance() > 0 && oldEntry.getKnockbackStrength() > 0,
          oldEntry.getKnockbackDistance(),
          oldEntry.getKnockbackStrength()
        ),
        oldEntry.getTaskLayouts().stream().map(oldTaskEntry -> new SignGroupConfiguration(
          oldTaskEntry.getTask(),
          convertSingleToMany(oldTaskEntry.getEmptyLayout()),
          convertSingleToMany(oldTaskEntry.getOnlineLayout()),
          convertSingleToMany(oldTaskEntry.getFullLayout())
        )).collect(Collectors.toList()),
        convertOldAnimation(oldEntry.getSearchLayouts()),
        convertOldAnimation(oldEntry.getStartingLayouts()),
        convertSingleToMany(oldEntry.getDefaultEmptyLayout()),
        convertSingleToMany(oldEntry.getDefaultOnlineLayout()),
        convertSingleToMany(oldEntry.getDefaultFullLayout())
      )).collect(Collectors.toList())
    );
  }

  protected static SignLayout convertSignLayout(de.dytanic.cloudnet.ext.signs.SignLayout oldLayout) {
    return new SignLayout(oldLayout.getLines(), oldLayout.getBlockType(), oldLayout.getSubId());
  }

  protected static SignLayoutsHolder convertOldAnimation(SignLayoutConfiguration configuration) {
    return new SignLayoutsHolder(configuration.getAnimationsPerSecond(), configuration.getSignLayouts().stream()
      .map(NodeSignsConfigurationHelper::convertSignLayout)
      .collect(Collectors.toList()));
  }

  protected static SignLayoutsHolder convertSingleToMany(de.dytanic.cloudnet.ext.signs.SignLayout oldLayout) {
    return new SignLayoutsHolder(1, new ArrayList<>(Collections.singleton(convertSignLayout(oldLayout))));
  }

  protected static Map<String, String> convertMessages(Map<String, String> oldMessages) {
    Map<String, String> messages = new HashMap<>(SignsConfiguration.DEFAULT_MESSAGES);
    for (Map.Entry<String, String> entry : oldMessages.entrySet()) {
      messages.put(entry.getKey(), entry.getValue().replace('&', '§'));
    }
    return messages;
  }
}
