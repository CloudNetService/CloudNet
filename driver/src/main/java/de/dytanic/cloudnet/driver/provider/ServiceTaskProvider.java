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

package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.Task;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCValidation;
import de.dytanic.cloudnet.driver.service.ServiceTask;
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
  @NonNull Collection<ServiceTask> permanentServiceTasks();

  /**
   * Clears all existing service tasks and sets the given collection as the new service tasks
   *
   * @param serviceTasks the new service tasks
   */
  void permanentServiceTasks(@NonNull Collection<ServiceTask> serviceTasks);

  /**
   * Gets a specific task by its name
   *
   * @param name the name of the task
   * @return the task or {@code null} if no task with that name exists
   */
  @Nullable
  ServiceTask serviceTask(@NonNull String name);

  /**
   * Checks whether the task with a specific name exists
   *
   * @param name the name of the task
   * @return {@code true} if the task exists or {@code false} otherwise
   */
  boolean serviceTaskPresent(@NonNull String name);

  /**
   * Adds a new task to the cloud
   *
   * @param serviceTask the task to be added
   */
  boolean addPermanentServiceTask(@NonNull ServiceTask serviceTask);

  /**
   * Removes a task from the cloud
   *
   * @param name the name of the task to be removed
   */
  void removePermanentServiceTaskByName(@NonNull String name);

  /**
   * Removes a task from the cloud
   *
   * @param serviceTask the task to be removed (the only thing that matters in this object is the name, the rest is
   *                    ignored)
   */
  void removePermanentServiceTask(@NonNull ServiceTask serviceTask);

  /**
   * Reloads all tasks
   */
  @NonNull
  default Task<Void> reloadAsync() {
    return CompletableTask.supply(this::reload);
  }

  /**
   * Gets all tasks that are registered in the cloud
   *
   * @return a list containing the task configurations of all tasks
   */
  @NonNull
  default Task<Collection<ServiceTask>> permanentServiceTasksAsync() {
    return CompletableTask.supply(() -> this.permanentServiceTasks());
  }

  /**
   * Clears all existing service tasks and sets the given collection as the new service tasks
   *
   * @param serviceTasks the new service tasks
   */
  @NonNull
  default Task<Void> permanentServiceTasksAsync(@NonNull Collection<ServiceTask> serviceTasks) {
    return CompletableTask.supply(() -> this.permanentServiceTasks(serviceTasks));
  }

  /**
   * Gets a specific task by its name
   *
   * @param name the name of the task
   * @return the task or {@code null} if no task with that name exists
   */
  @NonNull
  default Task<ServiceTask> serviceTaskAsync(@NonNull String name) {
    return CompletableTask.supply(() -> this.serviceTask(name));
  }

  /**
   * Checks whether the task with a specific name exists
   *
   * @param name the name of the task
   * @return {@code true} if the task exists or {@code false} otherwise
   */
  @NonNull
  default Task<Boolean> isServiceTaskPresentAsync(@NonNull String name) {
    return CompletableTask.supply(() -> this.serviceTaskPresent(name));
  }

  /**
   * Adds a new task to the cloud
   *
   * @param serviceTask the task to be added
   */
  @NonNull
  default Task<Boolean> addPermanentServiceTaskAsync(@NonNull ServiceTask serviceTask) {
    return CompletableTask.supply(() -> this.addPermanentServiceTask(serviceTask));
  }

  /**
   * Removes a task from the cloud
   *
   * @param name the name of the task to be removed
   */
  @NonNull
  default Task<Void> removePermanentServiceTaskByNameAsync(@NonNull String name) {
    return CompletableTask.supply(() -> this.removePermanentServiceTaskByName(name));
  }

  /**
   * Removes a task from the cloud
   *
   * @param serviceTask the task to be removed (the only thing that matters in this object is the name, the rest is
   *                    ignored)
   */
  @NonNull
  default Task<Void> removePermanentServiceTaskAsync(@NonNull ServiceTask serviceTask) {
    return this.removePermanentServiceTaskByNameAsync(serviceTask.name());
  }

}
