/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.provider;

import eu.cloudnetservice.cloudnet.common.concurrent.CompletableTask;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCValidation;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTask;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * This class provides access to the tasks of the cloud (tasks folder)
 */
@RPCValidation
public interface ServiceTaskProvider {

  /**
   * Reloads all tasks
   */
  void reload();

  /**
   * Gets all tasks that are registered in the cloud
   *
   * @return a list containing the task configurations of all tasks
   */
  @UnmodifiableView
  @NonNull Collection<ServiceTask> serviceTasks();

  /**
   * Gets a specific task by its name
   *
   * @param name the name of the task
   * @return the task or null if no task with that name exists
   */
  @Nullable ServiceTask serviceTask(@NonNull String name);

  /**
   * Adds a new task to the cloud
   *
   * @param serviceTask the task to be added
   */
  boolean addServiceTask(@NonNull ServiceTask serviceTask);

  /**
   * Removes a task from the cloud
   *
   * @param name the name of the task to be removed
   */
  void removeServiceTaskByName(@NonNull String name);

  /**
   * Removes a task from the cloud
   *
   * @param serviceTask the task to be removed (the only thing that matters in this object is the name, the rest is
   *                    ignored)
   */
  void removeServiceTask(@NonNull ServiceTask serviceTask);

  /**
   * Reloads all tasks
   */
  default @NonNull Task<Void> reloadAsync() {
    return CompletableTask.supply(this::reload);
  }

  /**
   * Gets all tasks that are registered in the cloud
   *
   * @return a list containing the task configurations of all tasks
   */
  default @NonNull Task<Collection<ServiceTask>> serviceTasksAsync() {
    return CompletableTask.supply(this::serviceTasks);
  }

  /**
   * Gets a specific task by its name
   *
   * @param name the name of the task
   * @return the task or null if no task with that name exists
   */
  default @NonNull Task<ServiceTask> serviceTaskAsync(@NonNull String name) {
    return CompletableTask.supply(() -> this.serviceTask(name));
  }

  /**
   * Adds a new task to the cloud
   *
   * @param serviceTask the task to be added
   */
  default @NonNull Task<Boolean> addServiceTaskAsync(@NonNull ServiceTask serviceTask) {
    return CompletableTask.supply(() -> this.addServiceTask(serviceTask));
  }

  /**
   * Removes a task from the cloud
   *
   * @param name the name of the task to be removed
   */
  default @NonNull Task<Void> removeServiceTaskByNameAsync(@NonNull String name) {
    return CompletableTask.supply(() -> this.removeServiceTaskByName(name));
  }

  /**
   * Removes a task from the cloud
   *
   * @param serviceTask the task to be removed (the only thing that matters in this object is the name, the rest is
   *                    ignored)
   */
  default @NonNull Task<Void> removeServiceTaskAsync(@NonNull ServiceTask serviceTask) {
    return CompletableTask.supply(() -> this.removeServiceTask(serviceTask));
  }
}
