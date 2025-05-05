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

package eu.cloudnetservice.modules.signs.impl.node.configuration;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.modules.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.modules.signs.configuration.SignGroupConfiguration;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.configuration.SignLayoutsHolder;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.impl._deprecated.configuration.SignConfigurationReaderAndWriter;
import eu.cloudnetservice.modules.signs.impl._deprecated.configuration.entry.SignLayoutConfiguration;
import java.nio.file.Path;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NodeSignsConfigurationHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(NodeSignsConfigurationHelper.class);

  private NodeSignsConfigurationHelper() {
    throw new UnsupportedOperationException();
  }

  public static void write(@NonNull SignsConfiguration configuration, @NonNull Path path) {
    Document.newJsonDocument().appendTree(configuration).writeTo(path);
  }

  public static SignsConfiguration read(@NonNull Path path) {
    var configurationDocument = DocumentFactory.json().parse(path);
    if (configurationDocument.contains("config")) {
      // write the new configuration file
      var configuration = convertOldConfiguration(configurationDocument, path);
      write(configuration, path);
      // notify that the convert was successful
      LOGGER.info("Successfully converted the old signs configuration file");
      // no need to load the configuration from the file again
      return configuration;
    }
    // check if the configuration file already exists
    if (configurationDocument.empty()) {
      // create a new configuration entry
      var configuration = SignsConfiguration.builder()
        .modifyEntries(entries -> entries.add(SignConfigurationType.JAVA.createEntry("Lobby")))
        .build();
      write(configuration, path);
      return configuration;
    }
    // the document contains a configuration
    return configurationDocument.toInstanceOf(SignsConfiguration.class);
  }

  // convert of old configuration file
  private static SignsConfiguration convertOldConfiguration(@NonNull Document.Mutable document, @NonNull Path path) {
    // read as old configuration file
    var oldConfiguration = SignConfigurationReaderAndWriter.read(document, path);
    // create new configuration from it
    return new SignsConfiguration(
      oldConfiguration.getConfigurations().stream().map(oldEntry -> new SignConfigurationEntry(
        oldEntry.getTargetGroup(),
        oldEntry.isSwitchToSearchingWhenServiceIsFull(),
        new SignConfigurationEntry.KnockbackConfiguration(
          oldEntry.getKnockbackDistance() > 0 && oldEntry.getKnockbackStrength() > 0,
          oldEntry.getKnockbackDistance(),
          oldEntry.getKnockbackStrength(),
          null
        ),
        oldEntry.getTaskLayouts().stream().map(oldTaskEntry -> new SignGroupConfiguration(
          oldTaskEntry.getTask(),
          oldEntry.isSwitchToSearchingWhenServiceIsFull(),
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

  @Contract("_ -> new")
  private static @NonNull SignLayout convertSignLayout(
    @NonNull eu.cloudnetservice.modules.signs.impl._deprecated.SignLayout oldLayout
  ) {
    return SignLayout.builder()
      .lines(oldLayout.getLines())
      .blockMaterial(oldLayout.getBlockType())
      .blockSubId(oldLayout.getSubId())
      .build();
  }

  @Contract("_ -> new")
  private static @NonNull SignLayoutsHolder convertOldAnimation(@NonNull SignLayoutConfiguration configuration) {
    return new SignLayoutsHolder(configuration.getAnimationsPerSecond(), configuration.getSignLayouts().stream()
      .map(NodeSignsConfigurationHelper::convertSignLayout)
      .collect(Collectors.toList()));
  }

  @Contract("_ -> new")
  private static @NonNull SignLayoutsHolder convertSingleToMany(
    @NonNull eu.cloudnetservice.modules.signs.impl._deprecated.SignLayout oldLayout
  ) {
    return SignLayoutsHolder.singleLayout(convertSignLayout(oldLayout));
  }
}
