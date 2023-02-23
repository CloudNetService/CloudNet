/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.provider;

import dev.derklaro.aerogel.PostConstruct;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.event.group.LocalGroupConfigurationAddEvent;
import eu.cloudnetservice.node.event.group.LocalGroupConfigurationRemoveEvent;
import eu.cloudnetservice.node.network.listener.message.GroupChannelMessageListener;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

@Singleton
@Provides(GroupConfigurationProvider.class)
public class NodeGroupConfigurationProvider implements GroupConfigurationProvider {

  private static final Path OLD_GROUPS_FILE = Path.of(
    System.getProperty("cloudnet.config.groups.path", "local/groups.json"));
  private static final Path GROUP_DIRECTORY_PATH = Path.of(
    System.getProperty("cloudnet.config.groups.directory.path", "local/groups"));

  private static final Type TYPE = TypeFactory.parameterizedClass(Collection.class, GroupConfiguration.class);

  private final EventManager eventManager;
  private final Map<String, GroupConfiguration> groupConfigurations = new ConcurrentHashMap<>();

  @Inject
  public NodeGroupConfigurationProvider(
    @NonNull EventManager eventManager,
    @NonNull RPCFactory rpcFactory,
    @NonNull DataSyncRegistry syncRegistry,
    @NonNull RPCHandlerRegistry handlerRegistry
  ) {
    this.eventManager = eventManager;

    // rpc
    rpcFactory.newHandler(GroupConfigurationProvider.class, this).registerTo(handlerRegistry);

    // cluster data sync
    syncRegistry.registerHandler(
      DataSyncHandler.<GroupConfiguration>builder()
        .key("group_configuration")
        .nameExtractor(Nameable::name)
        .convertObject(GroupConfiguration.class)
        .writer(this::addGroupConfigurationSilently)
        .dataCollector(this::groupConfigurations)
        .currentGetter(group -> this.groupConfiguration(group.name()))
        .build());

    // run the conversion of the old file
    this.upgrade();
  }

  @PostConstruct
  private void loadGroups() {
    // load the groups
    if (Files.exists(GROUP_DIRECTORY_PATH)) {
      this.loadGroupConfigurations();
    } else {
      FileUtil.createDirectory(GROUP_DIRECTORY_PATH);
    }
  }

  @PostConstruct
  private void registerChannelMessageListener() {
    this.eventManager.registerListener(GroupChannelMessageListener.class);
  }

  @Override
  public void reload() {
    // clear the local cache
    this.groupConfigurations.clear();
    // load the group files
    this.loadGroupConfigurations();
  }

  @Override
  public @NonNull @UnmodifiableView Collection<GroupConfiguration> groupConfigurations() {
    return Collections.unmodifiableCollection(this.groupConfigurations.values());
  }

  @Override
  public @Nullable GroupConfiguration groupConfiguration(@NonNull String name) {
    return this.groupConfigurations.get(name);
  }

  @Override
  public boolean addGroupConfiguration(@NonNull GroupConfiguration groupConfiguration) {
    if (!this.eventManager.callEvent(new LocalGroupConfigurationAddEvent(groupConfiguration)).cancelled()) {
      this.addGroupConfigurationSilently(groupConfiguration);
      // publish the change to the cluster
      ChannelMessage.builder()
        .targetAll()
        .message("add_group_configuration")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .buffer(DataBuf.empty().writeObject(groupConfiguration))
        .build()
        .send();
      return true;
    }
    return false;
  }

  @Override
  public void removeGroupConfigurationByName(@NonNull String name) {
    var configuration = this.groupConfiguration(name);
    if (configuration != null) {
      this.removeGroupConfiguration(configuration);
    }
  }

  @Override
  public void removeGroupConfiguration(@NonNull GroupConfiguration groupConfiguration) {
    if (!this.eventManager.callEvent(new LocalGroupConfigurationRemoveEvent(groupConfiguration)).cancelled()) {
      this.removeGroupConfigurationSilently(groupConfiguration);
      // publish the change to the cluster
      ChannelMessage.builder()
        .targetAll()
        .message("remove_group_configuration")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .buffer(DataBuf.empty().writeObject(groupConfiguration))
        .build()
        .send();
    }
  }

  public void addGroupConfigurationSilently(@NonNull GroupConfiguration groupConfiguration) {
    // add the group to the local cache
    this.groupConfigurations.put(groupConfiguration.name(), groupConfiguration);
    // store the group file
    this.writeGroupConfiguration(groupConfiguration);
  }

  public void removeGroupConfigurationSilently(@NonNull GroupConfiguration groupConfiguration) {
    // remove the group from the cache
    this.groupConfigurations.remove(groupConfiguration.name());
    // remove the local file
    FileUtil.delete(this.groupFile(groupConfiguration));
  }

  private void upgrade() {
    if (Files.exists(OLD_GROUPS_FILE)) {
      // read all groups from the old file
      Collection<GroupConfiguration> oldConfigurations = JsonDocument.newDocument(OLD_GROUPS_FILE).get("groups", TYPE);
      // add all configurations to the current configurations
      oldConfigurations.forEach(config -> this.groupConfigurations.put(config.name(), config));
      // save the new configurations
      this.writeAllGroupConfigurations();
      // delete the old file
      FileUtil.delete(OLD_GROUPS_FILE);
    }
  }

  protected @NonNull Path groupFile(@NonNull GroupConfiguration configuration) {
    return GROUP_DIRECTORY_PATH.resolve(configuration.name() + ".json");
  }

  protected void writeGroupConfiguration(@NonNull GroupConfiguration configuration) {
    JsonDocument.newDocument(configuration).write(this.groupFile(configuration));
  }

  protected void writeAllGroupConfigurations() {
    // write all configurations
    for (var configuration : this.groupConfigurations.values()) {
      this.writeGroupConfiguration(configuration);
    }
    // delete all group files which do not exist anymore
    FileUtil.walkFileTree(GROUP_DIRECTORY_PATH, ($, file) -> {
      // check if we know the file name
      var groupName = file.getFileName().toString().replace(".json", "");
      if (!this.groupConfigurations.containsKey(groupName)) {
        FileUtil.delete(file);
      }
    }, false, "*.json");
  }

  protected void loadGroupConfigurations() {
    FileUtil.walkFileTree(GROUP_DIRECTORY_PATH, ($, file) -> {
      var document = JsonDocument.newDocument(file);

      // TODO: remove in 4.1
      // check if the task has environment variables
      var variables = document.get("environmentVariables");
      if (variables == null) {
        document.append("environmentVariables", new HashMap<>());
      }

      // load the group
      var group = document.toInstanceOf(GroupConfiguration.class);

      // check if the file name is still up-to-date
      var groupName = file.getFileName().toString().replace(".json", "");
      if (!groupName.equals(group.name())) {
        // rename the file
        FileUtil.move(file, this.groupFile(group), StandardCopyOption.REPLACE_EXISTING);
      }
      // cache the group
      this.addGroupConfiguration(group);
    }, false, "*.json");
  }
}
