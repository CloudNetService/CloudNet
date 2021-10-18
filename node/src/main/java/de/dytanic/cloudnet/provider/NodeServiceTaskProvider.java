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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.task.LocalServiceTaskAddEvent;
import de.dytanic.cloudnet.event.task.LocalServiceTaskRemoveEvent;
import de.dytanic.cloudnet.network.listener.message.TaskChannelMessageListener;
import de.dytanic.cloudnet.setup.DefaultTaskSetup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeServiceTaskProvider implements ServiceTaskProvider {

  private static final Path TASKS_DIRECTORY = Paths.get(
    System.getProperty("cloudnet.config.tasks.directory.path", "local/tasks"));

  private final IEventManager eventManager;
  private final Set<ServiceTask> serviceTasks = ConcurrentHashMap.newKeySet();

  public NodeServiceTaskProvider(@NotNull CloudNet nodeInstance) {
    this.eventManager = nodeInstance.getEventManager();
    this.eventManager.registerListener(new TaskChannelMessageListener(this.eventManager, this));

    nodeInstance.getRPCProviderFactory().newHandler(ServiceTaskProvider.class, this).registerToDefaultRegistry();

    if (Files.exists(TASKS_DIRECTORY)) {
      this.loadServiceTasks();
    } else {
      FileUtils.createDirectory(TASKS_DIRECTORY);
      nodeInstance.getInstallation().registerSetup(new DefaultTaskSetup());
    }
  }

  @Override
  public void reload() {
    // clear the cache
    this.serviceTasks.clear();
    // load all service tasks
    this.loadServiceTasks();
  }

  @Override
  public @NotNull Collection<ServiceTask> getPermanentServiceTasks() {
    return Collections.unmodifiableCollection(this.serviceTasks);
  }

  @Override
  public void setPermanentServiceTasks(@NotNull Collection<ServiceTask> serviceTasks) {
    this.setPermanentServiceTasksSilently(serviceTasks);
    // notify the cluster
    ChannelMessage.builder()
      .targetNodes()
      .message("set_service_tasks")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(serviceTasks))
      .build()
      .send();
  }

  @Override
  public @Nullable ServiceTask getServiceTask(@NotNull String name) {
    return this.serviceTasks.stream()
      .filter(task -> task.getName().equals(name))
      .findFirst()
      .orElse(null);
  }

  @Override
  public boolean isServiceTaskPresent(@NotNull String name) {
    return this.serviceTasks.stream().anyMatch(task -> task.getName().equals(name));
  }

  @Override
  public boolean addPermanentServiceTask(@NotNull ServiceTask serviceTask) {
    if (!this.eventManager.callEvent(new LocalServiceTaskAddEvent(serviceTask)).isCancelled()) {
      this.addPermanentServiceTaskSilently(serviceTask);
      // notify the cluster
      ChannelMessage.builder()
        .targetAll()
        .message("add_service_task")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .buffer(DataBuf.empty().writeObject(serviceTask))
        .build()
        .send();
      return true;
    }
    return false;
  }

  @Override
  public void removePermanentServiceTaskByName(@NotNull String name) {
    ServiceTask task = this.getServiceTask(name);
    if (task != null) {
      this.removePermanentServiceTask(task);
    }
  }

  @Override
  public void removePermanentServiceTask(@NotNull ServiceTask serviceTask) {
    if (!this.eventManager.callEvent(new LocalServiceTaskRemoveEvent(serviceTask)).isCancelled()) {
      this.removePermanentServiceTaskSilently(serviceTask);
      // notify the whole network
      ChannelMessage.builder()
        .targetAll()
        .message("remove_service_task")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .buffer(DataBuf.empty().writeObject(serviceTask))
        .build()
        .send();
    }
  }

  public void addPermanentServiceTaskSilently(@NotNull ServiceTask serviceTask) {
    // cache locally
    this.serviceTasks.add(serviceTask);
    this.writeServiceTask(serviceTask);
  }

  public void removePermanentServiceTaskSilently(@NotNull ServiceTask serviceTask) {
    // remove from cache
    this.serviceTasks.remove(serviceTask);
    // remove the local file if it exists
    FileUtils.delete(this.getTaskFile(serviceTask));
  }

  public void setPermanentServiceTasksSilently(@NotNull Collection<ServiceTask> serviceTasks) {
    // update the cache
    this.serviceTasks.clear();
    this.serviceTasks.addAll(serviceTasks);
    // store all tasks
    this.writeAllServiceTasks();
  }

  protected @NotNull Path getTaskFile(@NotNull ServiceTask task) {
    return TASKS_DIRECTORY.resolve(task.getName() + ".json");
  }

  protected void writeServiceTask(@NotNull ServiceTask serviceTask) {
    JsonDocument.newDocument(serviceTask).write(this.getTaskFile(serviceTask));
  }

  protected void writeAllServiceTasks() {
    // write all service tasks
    for (ServiceTask serviceTask : this.serviceTasks) {
      this.writeServiceTask(serviceTask);
    }
    // delete all service task files which do not exist anymore
    FileUtils.walkFileTree(TASKS_DIRECTORY, ($, file) -> {
      // check if we know the file name
      String taskName = file.getFileName().toString().replace(".json", "");
      if (this.serviceTasks.stream().noneMatch(task -> taskName.equals(task.getName()))) {
        FileUtils.delete(file);
      }
    }, false, "*.json");
  }

  protected void loadServiceTasks() {
    FileUtils.walkFileTree(TASKS_DIRECTORY, ($, file) -> {
      // load the service task
      ServiceTask task = JsonDocument.newDocument(file).toInstanceOf(ServiceTask.class);
      // check if the file name is still up-to-date
      String taskName = file.getFileName().toString().replace(".json", "");
      if (!taskName.equals(task.getName())) {
        // rename the file
        FileUtils.move(file, this.getTaskFile(task), StandardCopyOption.REPLACE_EXISTING);
      }
      // cache the task
      this.serviceTasks.add(task);
    }, false, "*.json");
  }
}
