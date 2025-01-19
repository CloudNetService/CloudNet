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

import eu.cloudnetservice.driver.service.ServiceTask;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The main api which allows read and write access to task configurations. This provider directly represents the holder
 * of the configurations as well, therefore every lookup made through this class should not require a cluster wide
 * lookup.
 * <p>
 * All tasks which are manageable via this provider are permanent. While a task a service is based on can be temporarily
 * registered in the system, this provider will <strong>never</strong> return these tasks. A service holds the same
 * configuration as a task, therefore no lookup of the task should be required. This does also mean that services could
 * be based on a task but the task was unregistered while the service is running.
 *
 * @since 4.0
 */
public interface ServiceTaskProvider {

  /**
   * Reloads this provider by clearing the backing task cache and re-reading all task configurations in the associated
   * directory. Note that this method will not trigger a cluster re-sync of the task configurations.
   */
  void reload();

  /**
   * Get all task configurations which are registered within the cluster. The backing collection will be updated if a
   * group configuration was added via the api endpoint of any node in the cluster. Additions and removals to the
   * returned collection are not possible and will not have any effect.
   *
   * @return all registered task configurations within the cluster.
   */
  @UnmodifiableView
  @NonNull
  Collection<ServiceTask> serviceTasks();

  /**
   * Get a task configuration which has the given name and is registered within the cluster. This method returns null if
   * no task with the given name is registered.
   *
   * @param name the name of the task to get.
   * @return the task configuration which has the given name or null if no task with the given name is registered.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable ServiceTask serviceTask(@NonNull String name);

  /**
   * Adds a new task configuration by caching the given object, creating the task file and syncing the change to all
   * other nodes which are currently connected in the cluster. This method either creates the task or updates it if it
   * already exists. There are no checks made if there is a diff before updating the configuration.
   *
   * @param serviceTask the task configuration to create or update.
   * @return true if the service task was registered or updated, false otherwise.
   * @throws NullPointerException if the given task configuration is null.
   */
  boolean addServiceTask(@NonNull ServiceTask serviceTask);

  /**
   * Deletes the task configuration with the given name on the local node and all other nodes in the cluster by removing
   * it from the backing collection and deleting the associated file.
   * <p>
   * This method does nothing if no task configuration with the given name exists.
   *
   * @param name the name of the task configuration to remove.
   * @throws NullPointerException if the given task name is null.
   */
  void removeServiceTaskByName(@NonNull String name);

  /**
   * Deletes the task configuration with the given name on the local node and all other nodes in the cluster by removing
   * it from the backing collection and deleting the associated file.
   * <p>
   * This method does nothing if no task configuration with the given name exists.
   *
   * @param serviceTask the service task to remove.
   * @throws NullPointerException if the given task name is null.
   */
  void removeServiceTask(@NonNull ServiceTask serviceTask);

  /**
   * Reloads this provider by clearing the backing task cache and re-reading all task configurations in the associated
   * directory. Note that this method will not trigger a cluster re-sync of the task configurations.
   *
   * @return a task completed when the provider was reloaded successfully.
   */
  @NonNull
  CompletableFuture<Void> reloadAsync();

  /**
   * Get all task configurations which are registered within the cluster. The backing collection will be updated if a
   * group configuration was added via the api endpoint of any node in the cluster. Additions and removals to the
   * returned collection are not possible and will not have any effect.
   *
   * @return a task completed with all registered task configurations within the cluster.
   */
  @NonNull
  CompletableFuture<Collection<ServiceTask>> serviceTasksAsync();

  /**
   * Get a task configuration which has the given name and is registered within the cluster. This method returns null if
   * no task with the given name is registered.
   *
   * @param name the name of the task to get.
   * @return a task completed with the task configuration which has the given name or null if such task exists.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull
  CompletableFuture<ServiceTask> serviceTaskAsync(@NonNull String name);

  /**
   * Adds a new task configuration by caching the given object, creating the task file and syncing the change to all
   * other nodes which are currently connected in the cluster. This method either creates the task or updates it if it
   * already exists. There are no checks made if there is a diff before updating the configuration.
   *
   * @param serviceTask the task configuration to create or update.
   * @return a task completed with true if the task configuration was registered or updated, false otherwise.
   * @throws NullPointerException if the given task configuration is null.
   */
  @NonNull
  CompletableFuture<Boolean> addServiceTaskAsync(@NonNull ServiceTask serviceTask);

  /**
   * Deletes the task configuration with the given name on the local node and all other nodes in the cluster by removing
   * it from the backing collection and deleting the associated file.
   * <p>
   * This method does nothing if no task configuration with the given name exists.
   *
   * @param name the name of the task configuration to remove.
   * @return a task completed when the service task with the given name was removed.
   * @throws NullPointerException if the given task name is null.
   */
  @NonNull
  CompletableFuture<Void> removeServiceTaskByNameAsync(@NonNull String name);

  /**
   * Deletes the task configuration with the given name on the local node and all other nodes in the cluster by removing
   * it from the backing collection and deleting the associated file.
   * <p>
   * This method does nothing if no task configuration with the given name exists.
   *
   * @param serviceTask the service task to remove.
   * @return a task completed when the given service task was removed.
   * @throws NullPointerException if the given task name is null.
   */
  @NonNull
  CompletableFuture<Void> removeServiceTaskAsync(@NonNull ServiceTask serviceTask);
}
