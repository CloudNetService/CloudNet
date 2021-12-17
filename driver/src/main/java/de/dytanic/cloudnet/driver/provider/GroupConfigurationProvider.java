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

package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCValidation;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * This class provides access to the groups of the cloud (groups.json file).
 */
@RPCValidation
public interface GroupConfigurationProvider {

  /**
   * Reloads the groups.json file
   */
  void reload();

  /**
   * Gets all groups that are registered in the cloud
   *
   * @return a list containing the group configurations of all groups
   */
  @NotNull
  @UnmodifiableView Collection<GroupConfiguration> groupConfigurations();

  /**
   * Clears all existing groups and sets the given collection as the new groups
   *
   * @param groupConfigurations the new groups
   */
  void groupConfigurations(@NotNull Collection<GroupConfiguration> groupConfigurations);

  /**
   * Gets a specific group by its name
   *
   * @param name the name of the group
   * @return the group or {@code null} if no group with that name exists
   */
  @Nullable GroupConfiguration groupConfiguration(@NotNull String name);

  /**
   * Checks whether the group with a specific name exists
   *
   * @param name the name of the group
   * @return {@code true} if the group exists or {@code false} otherwise
   */
  boolean isGroupConfigurationPresent(@NotNull String name);

  /**
   * Adds a new group to the cloud
   *
   * @param groupConfiguration the group to be added
   */
  void addGroupConfiguration(@NotNull GroupConfiguration groupConfiguration);

  /**
   * Removes a group from the cloud
   *
   * @param name the name of the group to be removed
   */
  void removeGroupConfigurationByName(@NotNull String name);

  /**
   * Removes a group from the cloud
   *
   * @param groupConfiguration the group to be removed (the only thing that matters in this object is the name, the rest
   *                           is ignored)
   */
  void removeGroupConfiguration(@NotNull GroupConfiguration groupConfiguration);

  /**
   * Reloads the groups.json file
   */
  @NotNull
  default ITask<Void> reloadAsync() {
    return CompletableTask.supply(this::reload);
  }

  /**
   * Gets all groups that are registered in the cloud
   *
   * @return a list containing the group configurations of all groups
   */
  @NotNull
  default ITask<Collection<GroupConfiguration>> groupConfigurationsAsync() {
    return CompletableTask.supply(() -> this.groupConfigurations());
  }

  /**
   * Clears all existing groups and sets the given collection as the new groups
   *
   * @param groupConfigurations the new groups
   */
  @NotNull
  default ITask<Void> groupConfigurationsAsync(@NotNull Collection<GroupConfiguration> groupConfigurations) {
    return CompletableTask.supply(() -> this.groupConfigurations(groupConfigurations));
  }

  /**
   * Gets a specific group by its name
   *
   * @param name the name of the group
   * @return the group or {@code null} if no group with that name exists
   */
  @NotNull
  default ITask<GroupConfiguration> groupConfigurationAsync(@NotNull String name) {
    return CompletableTask.supply(() -> this.groupConfiguration(name));
  }

  /**
   * Checks whether the group with a specific name exists
   *
   * @param name the name of the group
   * @return {@code true} if the group exists or {@code false} otherwise
   */
  @NotNull
  default ITask<Boolean> isGroupConfigurationPresentAsync(@NotNull String name) {
    return CompletableTask.supply(() -> this.isGroupConfigurationPresent(name));
  }

  /**
   * Adds a new group to the cloud
   *
   * @param groupConfiguration the group to be added
   */
  @NotNull
  default ITask<Void> addGroupConfigurationAsync(@NotNull GroupConfiguration groupConfiguration) {
    return CompletableTask.supply(() -> this.addGroupConfiguration(groupConfiguration));
  }

  /**
   * Removes a group from the cloud
   *
   * @param name the name of the group to be removed
   */
  @NotNull
  default ITask<Void> removeGroupConfigurationByNameAsync(@NotNull String name) {
    return CompletableTask.supply(() -> this.removeGroupConfigurationByName(name));
  }

  /**
   * Removes a group from the cloud
   *
   * @param groupConfiguration the group to be removed (the only thing that matters in this object is the name, the rest
   *                           is ignored)
   */
  @NotNull
  default ITask<Void> removeGroupConfigurationAsync(@NotNull GroupConfiguration groupConfiguration) {
    return CompletableTask.supply(() -> this.removeGroupConfiguration(groupConfiguration));
  }
}
