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

package eu.cloudnetservice.modules.smart.impl;

import dev.derklaro.aerogel.auto.annotation.Provides;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.modules.smart.SmartServiceManagement;
import eu.cloudnetservice.modules.smart.SmartServiceTaskConfig;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.impl.cluster.sync.DefaultDataSyncHandler;
import eu.cloudnetservice.utils.base.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@Provides(SmartServiceManagement.class)
public final class NodeSmartServiceManagement implements SmartServiceManagement {

  public static final String SMART_CONFIG_ADD = "add_smart_config";
  public static final String SMART_CONFIG_REMOVE = "remove_smart_config";
  public static final String SMART_CHANNEL_NAME = "internal_smart_channel";

  private final Path dataDirectory;
  private final Map<String, SmartServiceTaskConfig> smartConfigs = new ConcurrentHashMap<>();

  @Inject
  public NodeSmartServiceManagement(
    @NonNull @Named("dataDirectory") Path dataDirectory,
    @NonNull DataSyncRegistry syncRegistry
  ) {
    this.dataDirectory = dataDirectory;

    syncRegistry.registerHandler(DefaultDataSyncHandler.<SmartServiceTaskConfig>builder()
        .key("smart-configs")
        .nameExtractor(SmartServiceTaskConfig::targetTask)
        .dataCollector(this.smartConfigs::values)
        .convertObject(SmartServiceTaskConfig.class)
        .currentGetter(config -> this.smartServiceTaskConfig(config.targetTask()))
        .writer(this::addSmartServiceTaskConfigSilently)
      .build());
    this.loadSmartConfigurations();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, SmartServiceTaskConfig> configurations() {
    return Collections.unmodifiableMap(this.smartConfigs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable SmartServiceTaskConfig smartServiceTaskConfig(@NonNull String taskName) {
    return this.smartConfigs.get(taskName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addSmartServiceTaskConfig(@NonNull SmartServiceTaskConfig config) {
    ChannelMessage.builder()
      .targetNodes()
      .channel(SMART_CHANNEL_NAME)
      .message(SMART_CONFIG_ADD)
      .build(buffer -> buffer.writeObject(config))
      .send();
    this.addSmartServiceTaskConfigSilently(config);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeSmartServiceTaskConfig(@NonNull String taskName) {
    ChannelMessage.builder()
      .targetNodes()
      .channel(SMART_CHANNEL_NAME)
      .message(SMART_CONFIG_REMOVE)
      .build(buffer -> buffer.writeString(taskName))
      .send();
    this.removeSmartServiceTaskConfigSilently(taskName);
  }

  public void addSmartServiceTaskConfigSilently(@NonNull SmartServiceTaskConfig config) {
    this.smartConfigs.put(config.targetTask(), config);
    DocumentFactory.json().newDocument(config).writeTo(this.smartConfigFile(config.targetTask()));
  }

  public void removeSmartServiceTaskConfigSilently(@NonNull String taskName) {
    this.smartConfigs.remove(taskName);
    FileUtil.delete(this.smartConfigFile(taskName));
  }

  void loadSmartConfigurations() {
    this.smartConfigs.clear();

    FileUtil.walkFileTree(this.dataDirectory, (_, file) -> {
      var config = DocumentFactory.json().parse(file).toInstanceOf(SmartServiceTaskConfig.class);
      var taskName = file.getFileName().toString().replace(".json", "");
      if (!taskName.equals(config.targetTask())) {
        FileUtil.move(file, this.smartConfigFile(config.targetTask()), StandardCopyOption.REPLACE_EXISTING);
      }

      this.addSmartServiceTaskConfig(config);
    }, false, "*.json");
  }

  private @NonNull Path smartConfigFile(@NonNull String taskName) {
    return this.dataDirectory.resolve(taskName + ".json");
  }
}
