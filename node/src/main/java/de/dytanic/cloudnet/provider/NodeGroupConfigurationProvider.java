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

package de.dytanic.cloudnet.provider;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.event.group.LocalGroupConfigurationAddEvent;
import de.dytanic.cloudnet.event.group.LocalGroupConfigurationRemoveEvent;
import de.dytanic.cloudnet.network.listener.message.GroupChannelMessageListener;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class NodeGroupConfigurationProvider implements GroupConfigurationProvider {

  private static final Path OLD_GROUPS_FILE = Paths.get(
    System.getProperty("cloudnet.config.groups.path", "local/groups.json"));
  private static final Path GROUP_DIRECTORY_PATH = Paths.get(
    System.getProperty("cloudnet.config.groups.directory.path", "local/groups"));

  private static final Type TYPE = TypeToken.getParameterized(Collection.class, GroupConfiguration.class).getType();

  private final IEventManager eventManager;
  private final Set<GroupConfiguration> groupConfigurations = ConcurrentHashMap.newKeySet();

  public NodeGroupConfigurationProvider(@NotNull IEventManager eventManager) {
    this.eventManager = eventManager;
    this.eventManager.registerListener(new GroupChannelMessageListener(eventManager, this));

    if (Files.exists(GROUP_DIRECTORY_PATH)) {
      this.loadGroupConfigurations();
    } else {
      FileUtils.createDirectoryReported(GROUP_DIRECTORY_PATH);
    }
    // run the conversion of the old file
    this.upgrade();
  }

  @Override
  public void reload() {
    // clear the local cache
    this.groupConfigurations.clear();
    // load the group files
    this.loadGroupConfigurations();
  }

  @Override
  public @NotNull @UnmodifiableView Collection<GroupConfiguration> getGroupConfigurations() {
    return Collections.unmodifiableCollection(this.groupConfigurations);
  }

  @Override
  public void setGroupConfigurations(@NotNull Collection<GroupConfiguration> groupConfigurations) {
    this.setGroupConfigurationsSilently(groupConfigurations);
    // publish the change to the cluster
    ChannelMessage.builder()
      .targetNodes()
      .message("set_group_configurations")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(groupConfigurations))
      .build()
      .send();
  }

  @Override
  public @Nullable GroupConfiguration getGroupConfiguration(@NotNull String name) {
    return this.groupConfigurations.stream()
      .filter(group -> group.getName().equals(name))
      .findFirst()
      .orElse(null);
  }

  @Override
  public boolean isGroupConfigurationPresent(@NotNull String name) {
    return this.groupConfigurations.stream().anyMatch(group -> group.getName().equals(name));
  }

  @Override
  public void addGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
    if (!this.eventManager.callEvent(new LocalGroupConfigurationAddEvent(groupConfiguration)).isCancelled()) {
      this.addGroupConfigurationSilently(groupConfiguration);
      // publish the change to the cluster
      ChannelMessage.builder()
        .targetAll()
        .message("add_group_configuration")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .buffer(DataBuf.empty().writeObject(groupConfiguration))
        .build()
        .send();
    }
  }

  @Override
  public void removeGroupConfigurationByName(@NotNull String name) {
    GroupConfiguration configuration = this.getGroupConfiguration(name);
    if (configuration != null) {
      this.removeGroupConfiguration(configuration);
    }
  }

  @Override
  public void removeGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
    if (!this.eventManager.callEvent(new LocalGroupConfigurationRemoveEvent(groupConfiguration)).isCancelled()) {
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

  public void addGroupConfigurationSilently(@NotNull GroupConfiguration groupConfiguration) {
    // add the group to the local cache
    this.groupConfigurations.add(groupConfiguration);
    // store the configuration files
    this.writeAllGroupConfigurations();
  }

  public void removeGroupConfigurationSilently(@NotNull GroupConfiguration groupConfiguration) {
    // remove the group from the cache
    this.groupConfigurations.remove(groupConfiguration);
    // remove the local file
    FileUtils.delete(this.getGroupFile(groupConfiguration));
  }

  public void setGroupConfigurationsSilently(@NotNull Collection<GroupConfiguration> groupConfigurations) {
    // update the local cache
    this.groupConfigurations.clear();
    this.groupConfigurations.addAll(groupConfigurations);
    // save the group files
    this.writeAllGroupConfigurations();
  }

  @Internal
  private void upgrade() {
    if (Files.exists(OLD_GROUPS_FILE)) {
      // read all groups from the old file
      Collection<GroupConfiguration> oldConfigurations = JsonDocument.newDocument(OLD_GROUPS_FILE).get("groups", TYPE);
      // add all configurations to the current configurations
      this.groupConfigurations.addAll(oldConfigurations);
      // save the new configurations
      this.writeAllGroupConfigurations();
      // delete the old file
      FileUtils.delete(OLD_GROUPS_FILE);
    }
  }

  protected @NotNull Path getGroupFile(@NotNull GroupConfiguration configuration) {
    return GROUP_DIRECTORY_PATH.resolve(configuration.getName() + ".json");
  }

  protected void writeGroupConfiguration(@NotNull GroupConfiguration configuration) {
    JsonDocument.newDocument(configuration).write(this.getGroupFile(configuration));
  }

  protected void writeAllGroupConfigurations() {
    // write all configurations
    for (GroupConfiguration configuration : this.groupConfigurations) {
      this.writeGroupConfiguration(configuration);
    }
    // delete all group files which do not exist anymore
    FileUtils.walkFileTree(GROUP_DIRECTORY_PATH, ($, file) -> {
      // check if we know the file name
      String groupName = file.getFileName().toString().replace(".json", "");
      if (this.groupConfigurations.stream().noneMatch(group -> groupName.equals(group.getName()))) {
        FileUtils.delete(file);
      }
    }, false, "*.json");
  }

  protected void loadGroupConfigurations() {
    FileUtils.walkFileTree(GROUP_DIRECTORY_PATH, ($, file) -> {
      // load the group
      GroupConfiguration group = JsonDocument.newDocument(file).toInstanceOf(GroupConfiguration.class);
      // check if the file name is still up-to-date
      String groupName = file.getFileName().toString().replace(".json", "");
      if (!groupName.equals(group.getName())) {
        // rename the file
        FileUtils.move(file, this.getGroupFile(group), StandardCopyOption.REPLACE_EXISTING);
      }
      // cache the task
      this.groupConfigurations.add(group);
    }, false, "*.json");
  }
}
