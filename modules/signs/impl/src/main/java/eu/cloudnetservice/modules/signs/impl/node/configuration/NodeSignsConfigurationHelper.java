/*
 * Copyright 2019-present CloudNetService team & contributors
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
import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
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
      // convert the old v3 configuration
      var configuration = convertOldConfiguration(configurationDocument, path);
      write(configuration, path);
      LOGGER.info("Successfully converted the old signs configuration file");
      return configuration;
    }

    if (configurationDocument.empty()) {
      // initial config load: create a new, blank config entry
      var configuration = SignsConfiguration.builder()
        .modifyEntries(entries -> entries.add(SignConfigurationType.JAVA.createEntry("Lobby")))
        .build();
      write(configuration, path);
      return configuration;
    }

    // document contains a modern configuration, load that - migrate if necessary
    convertGlowingColor(configurationDocument);
    var configuration = configurationDocument.toInstanceOf(SignsConfiguration.class);
    write(configuration, path);
    return configuration;
  }

  /**
   * Converts the old {@code glowingColor} setting for all config entries in the given config document.
   *
   * @param configDocument the config document to convert.
   * @throws NullPointerException if the given config document is null.
   */
  private static void convertGlowingColor(@NonNull Document.Mutable configDocument) {
    var listDocumentType = TypeFactory.parameterizedClass(List.class, Document.class);
    var layoutNames = List.of("searchingLayout", "startingLayout", "emptyLayout", "onlineLayout", "fullLayout");

    List<Document> configEntries = configDocument.readObject("entries", listDocumentType);
    for (var entryIndex = 0; entryIndex < configEntries.size(); entryIndex++) {
      // convert top-level layouts
      var configEntry = configEntries.get(entryIndex).mutableCopy();
      convertGlowingInConfig(configEntry, listDocumentType, layoutNames);

      // convert group-level layouts
      List<Document> groupConfigurations = configEntry.readObject("groupConfigurations", listDocumentType);
      for (var groupIndex = 0; groupIndex < groupConfigurations.size(); groupIndex++) {
        var groupConfig = groupConfigurations.get(groupIndex).mutableCopy();
        convertGlowingInConfig(groupConfig, listDocumentType, layoutNames);
        groupConfigurations.set(groupIndex, groupConfig);
      }
      configEntry.append("groupConfigurations", groupConfigurations);

      // update the config entry
      configEntries.set(entryIndex, configEntry);
    }

    // copy the modified config entries into the source document
    configDocument.append("entries", configEntries);
  }

  /**
   * Converts the old {@code glowingColor} setting for all layout holders in the given config document.
   *
   * @param config           the config document to convert.
   * @param listDocumentType type representing a list of documents.
   * @param layoutNames      the layout property names to convert.
   * @throws NullPointerException if the given config document, list type or layout names is null.
   */
  private static void convertGlowingInConfig(
    @NonNull Document.Mutable config,
    @NonNull Type listDocumentType,
    @NonNull List<String> layoutNames
  ) {
    for (var layoutName : layoutNames) {
      var holder = config.readMutableDocument(layoutName, null);
      if (holder != null) {
        List<Document> layouts = holder.readObject("signLayouts", listDocumentType);
        for (var index = 0; index < layouts.size(); index++) {
          var layout = layouts.get(index).mutableCopy();
          if (layout.contains("glowingColor")) {
            var glowingColor = layout.getString("glowingColor");
            if (glowingColor != null) {
              // glowing color was set, enable text color and glowing
              layout.append("textColor", glowingColor);
              layout.append("textGlowing", true);
            } else {
              // glowing color was not set, no need to enable text color or glowing
              layout.appendNull("textColor");
              layout.append("textGlowing", false);
            }

            // remove old glowing color property, update document
            layout.remove("glowingColor");
            layouts.set(index, layout);
          }
        }

        // update the sign layouts with the modified variant in the holder document and then in the original config
        holder.append("signLayouts", layouts);
        config.append(layoutName, holder);
      }
    }
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
