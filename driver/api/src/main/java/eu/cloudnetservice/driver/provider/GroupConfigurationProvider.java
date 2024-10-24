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

package eu.cloudnetservice.driver.provider;

import eu.cloudnetservice.driver.service.GroupConfiguration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The main api which allows read and write access to group configurations. This provider directly represents the holder
 * of the configurations as well, therefore every lookup made through this class should not require a cluster wide
 * lookup.
 *
 * @since 4.0
 */
public interface GroupConfigurationProvider {

  /**
   * Reloads this provider by clearing the backing group cache and re-reading all group configurations in the associated
   * directory. Note that this method will not trigger a cluster re-sync of the group configurations.
   */
  void reload();

  /**
   * Get all group configurations which are registered within the cluster. The backing collection will be updated if a
   * group configuration was added via the api endpoint of any node in the cluster. Additions and removals to the
   * returned collection are not possible and will not have any effect.
   *
   * @return all registered group configurations within the cluster.
   */
  @NonNull
  @UnmodifiableView
  Collection<GroupConfiguration> groupConfigurations();

  /**
   * Get a group configuration which has the given name and is registered within the cluster. This method returns null
   * if no group with the given name is registered.
   *
   * @param name the name of the group to get.
   * @return the group configuration which has the given name or null if no group with the given name is registered.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable
  GroupConfiguration groupConfiguration(@NonNull String name);

  /**
   * Adds a new group configuration by caching the given object, creating the group file and syncing the change to all
   * other nodes which are currently connected in the cluster. This method either creates the group or updates it if it
   * already exists. There are no checks made if there is a diff before updating the configuration.
   *
   * @param groupConfiguration the group configuration to create or update.
   * @return true if the group configuration was registered or updated, false otherwise.
   * @throws NullPointerException if the given group configuration is null.
   */
  boolean addGroupConfiguration(@NonNull GroupConfiguration groupConfiguration);

  /**
   * Deletes the group configuration with the given name on the local node and all other nodes in the cluster by
   * removing it from the backing collection and deleting the associated file.
   * <p>
   * This method does nothing if no group configuration with the given name exists.
   *
   * @param name the name of the group configuration to remove.
   * @throws NullPointerException if the given group name is null.
   */
  void removeGroupConfigurationByName(@NonNull String name);

  /**
   * Deletes the group configuration with the given name on the local node and all other nodes in the cluster by
   * removing it from the backing collection and deleting the associated file.
   * <p>
   * This method does nothing if no group configuration with the given name exists.
   *
   * @param groupConfiguration the group configuration to remove.
   * @throws NullPointerException if the given group configuration is null.
   */
  void removeGroupConfiguration(@NonNull GroupConfiguration groupConfiguration);

  /**
   * Reloads this provider by clearing the backing group cache and re-reading all group configurations in the associated
   * directory. Note that this method will not trigger a cluster re-sync of the group configurations.
   *
   * @return a task completed if the group configurations were reloaded.
   */
  @NonNull
  CompletableFuture<Void> reloadAsync();

  /**
   * Get all group configurations which are registered within the cluster. The backing collection will be updated if a
   * group configuration was added via the api endpoint of any node in the cluster. Additions and removals to the
   * returned collection are not possible and will not have any effect.
   *
   * @return a task completed with all registered group configurations within the cluster.
   */
  @NonNull
  CompletableFuture<Collection<GroupConfiguration>> groupConfigurationsAsync();

  /**
   * Get a group configuration which has the given name and is registered within the cluster. This method returns null
   * if no group with the given name is registered.
   *
   * @param name the name of the group to get.
   * @return a task completed with the group which has the given name or null if no such group with is registered.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull
  CompletableFuture<GroupConfiguration> groupConfigurationAsync(@NonNull String name);

  /**
   * Adds a new group configuration by caching the given object, creating the group file and syncing the change to all
   * other nodes which are currently connected in the cluster. This method either creates the group or updates it if it
   * already exists. There are no checks made if there is a diff before updating the configuration.
   *
   * @param groupConfiguration the group configuration to create or update.
   * @return a task completed with true if the group configuration was added or updated, false otherwise.
   * @throws NullPointerException if the given group configuration is null.
   */
  @NonNull
  CompletableFuture<Boolean> addGroupConfigurationAsync(@NonNull GroupConfiguration groupConfiguration);

  /**
   * Deletes the group configuration with the given name on the local node and all other nodes in the cluster by
   * removing it from the backing collection and deleting the associated file.
   * <p>
   * This method does nothing if no group configuration with the given name exists.
   *
   * @param name the name of the group configuration to remove.
   * @return a task completed when the group configuration with the given name was removed.
   * @throws NullPointerException if the given group name is null.
   */
  @NonNull
  CompletableFuture<Void> removeGroupConfigurationByNameAsync(@NonNull String name);

  /**
   * Deletes the group configuration with the given name on the local node and all other nodes in the cluster by
   * removing it from the backing collection and deleting the associated file.
   * <p>
   * This method does nothing if no group configuration with the given name exists.
   *
   * @param groupConfiguration the group configuration to remove.
   * @return a task completed when the given group configuration was removed.
   * @throws NullPointerException if the given group configuration is null.
   */
  @NonNull
  CompletableFuture<Void> removeGroupConfigurationAsync(@NonNull GroupConfiguration groupConfiguration);
}
