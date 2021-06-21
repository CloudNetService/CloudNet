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

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.task.ServiceTaskAddEvent;
import de.dytanic.cloudnet.event.service.task.ServiceTaskRemoveEvent;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;

public class NodeServiceTaskProvider implements ServiceTaskProvider {

  private static final Path OLD_TASK_CONFIG_FILE = Paths
    .get(System.getProperty("cloudnet.config.task.path", "local/tasks.json"));
  private static final Path TASKS_DIRECTORY = Paths
    .get(System.getProperty("cloudnet.config.tasks.directory.path", "local/tasks"));

  private final CloudNet cloudNet;
  private final Collection<ServiceTask> permanentServiceTasks = new CopyOnWriteArrayList<>();

  public NodeServiceTaskProvider(CloudNet cloudNet) {
    this.cloudNet = cloudNet;
  }

  public boolean isFileCreated() {
    return Files.exists(TASKS_DIRECTORY);
  }

  private void load() throws IOException {
    // check if the old tasks.json is still in use and upgrade if necessary
    this.upgrade();
    // remove all pre-loaded tasks
    this.permanentServiceTasks.clear();
    if (Files.notExists(TASKS_DIRECTORY)) {
      return;
    }
    // walk over all files in the document tree
    // maxDepth 1 indicates that we only walk in the tasks directory, not in sub directories
    Files.walkFileTree(TASKS_DIRECTORY, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        System.out.println(LanguageManager.getMessage("cloudnet-load-task")
          .replace("%path%", path.toString()));

        ServiceTask task = JsonDocument.newDocument(path).toInstanceOf(ServiceTask.class);
        if (task != null) {
          // write all new configuration entries (the name too if missing) to the file if needed
          JsonDocument.newDocument(task).write(path);
          // check if we can load the task
          if (task.getName() != null) {
            NodeServiceTaskProvider.this.permanentServiceTasks.add(task);
            System.out.println(LanguageManager.getMessage("cloudnet-load-task-success")
              .replace("%path%", path.toString()).replace("%name%", task.getName()));
            // just a notify for the user that cloudnet is not attempting to start new services
            if (task.isMaintenance()) {
              CloudNet.getInstance().getLogger().warning(LanguageManager.getMessage(
                "cloudnet-load-task-maintenance-warning").replace("%task%", task.getName()));
            }
          }
        } else {
          System.err.println(LanguageManager.getMessage("cloudnet-load-task-failed")
            .replace("%path%", path.toString()));
        }

        return FileVisitResult.CONTINUE;
      }
    });
  }

  private void upgrade() throws IOException {
    if (Files.exists(OLD_TASK_CONFIG_FILE)) {
      JsonDocument document = JsonDocument.newDocument(OLD_TASK_CONFIG_FILE);
      this.permanentServiceTasks
        .addAll(document.get("tasks", TypeToken.getParameterized(Collection.class, ServiceTask.class).getType()));
      this.save();

      try {
        Files.delete(OLD_TASK_CONFIG_FILE);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
  }

  private void save() throws IOException {
    Files.createDirectories(TASKS_DIRECTORY);

    for (ServiceTask task : this.permanentServiceTasks) {
      this.writeTask(task);
    }

    Files.walkFileTree(TASKS_DIRECTORY, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String name = file.getFileName().toString();
        if (NodeServiceTaskProvider.this.permanentServiceTasks.stream()
          .noneMatch(serviceTask -> (serviceTask.getName() + ".json").equalsIgnoreCase(name))) {
          Files.delete(file);
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private void deleteTaskFile(String name) {
    try {
      Files.deleteIfExists(TASKS_DIRECTORY.resolve(name + ".json"));
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  public void writeTask(ServiceTask task) {
    new JsonDocument(task).write(TASKS_DIRECTORY.resolve(task.getName() + ".json"));
  }

  @Override
  public void reload() {
    try {
      this.load();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  @Override
  public Collection<ServiceTask> getPermanentServiceTasks() {
    return Collections.unmodifiableCollection(this.permanentServiceTasks);
  }

  @Override
  public void setPermanentServiceTasks(@NotNull Collection<ServiceTask> serviceTasks) {
    this.setServiceTasksWithoutClusterSync(serviceTasks);
    this.cloudNet.updateServiceTasksInCluster(serviceTasks, NetworkUpdateType.SET);
  }

  public void setServiceTasksWithoutClusterSync(@NotNull Collection<ServiceTask> tasks) {
    Preconditions.checkNotNull(tasks);

    this.permanentServiceTasks.clear();
    this.permanentServiceTasks.addAll(tasks);
    try {
      this.save();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  @Override
  public ServiceTask getServiceTask(@NotNull String name) {
    Preconditions.checkNotNull(name);

    return this.permanentServiceTasks.stream().filter(serviceTask -> serviceTask.getName().equalsIgnoreCase(name))
      .findFirst().orElse(null);
  }

  @Override
  public boolean isServiceTaskPresent(@NotNull String name) {
    Preconditions.checkNotNull(name);

    return this.getServiceTask(name) != null;
  }

  @Override
  public boolean addPermanentServiceTask(@NotNull ServiceTask serviceTask) {
    Preconditions.checkNotNull(serviceTask);

    if (this.addServiceTaskWithoutClusterSync(serviceTask)) {
      this.cloudNet.updateServiceTasksInCluster(Collections.singletonList(serviceTask), NetworkUpdateType.ADD);
      return true;
    }
    return false;
  }

  public boolean addServiceTaskWithoutClusterSync(ServiceTask serviceTask) {
    ServiceTaskAddEvent event = new ServiceTaskAddEvent(serviceTask);
    CloudNetDriver.getInstance().getEventManager().callEvent(event);

    if (!event.isCancelled()) {
      if (this.isServiceTaskPresent(serviceTask.getName())) {
        this.permanentServiceTasks.removeIf(task -> task.getName().equalsIgnoreCase(serviceTask.getName()));
      }

      this.permanentServiceTasks.add(serviceTask);

      this.writeTask(serviceTask);

      return true;
    }
    return false;
  }

  @Override
  public void removePermanentServiceTask(@NotNull String name) {
    Preconditions.checkNotNull(name);

    ServiceTask serviceTask = this.removeServiceTaskWithoutClusterSync(name);
    if (serviceTask != null) {
      this.cloudNet.updateServiceTasksInCluster(Collections.singletonList(serviceTask), NetworkUpdateType.REMOVE);
    }
  }

  public ServiceTask removeServiceTaskWithoutClusterSync(String name) {
    for (ServiceTask serviceTask : this.permanentServiceTasks) {
      if (serviceTask.getName().equalsIgnoreCase(name)) {
        if (!CloudNetDriver.getInstance().getEventManager().callEvent(new ServiceTaskRemoveEvent(serviceTask))
          .isCancelled()) {
          this.permanentServiceTasks.remove(serviceTask);
          this.deleteTaskFile(name);
          return serviceTask;
        }
      }
    }
    return null;
  }

  @Override
  public void removePermanentServiceTask(@NotNull ServiceTask serviceTask) {
    Preconditions.checkNotNull(serviceTask);

    this.removePermanentServiceTask(serviceTask.getName());
  }

  @Override
  public @NotNull ITask<Void> reloadAsync() {
    return this.cloudNet.scheduleTask(() -> {
      this.reload();
      return null;
    });
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
    return this.cloudNet.scheduleTask(this::getPermanentServiceTasks);
  }

  @Override
  public @NotNull ITask<Void> setPermanentServiceTasksAsync(@NotNull Collection<ServiceTask> serviceTasks) {
    return this.cloudNet.scheduleTask(() -> {
      this.setPermanentServiceTasks(serviceTasks);
      return null;
    });
  }

  @Override
  @NotNull
  public ITask<ServiceTask> getServiceTaskAsync(@NotNull String name) {
    return this.cloudNet.scheduleTask(() -> this.getServiceTask(name));
  }

  @Override
  @NotNull
  public ITask<Boolean> isServiceTaskPresentAsync(@NotNull String name) {
    return this.cloudNet.scheduleTask(() -> this.isServiceTaskPresent(name));
  }

  @Override
  @NotNull
  public ITask<Boolean> addPermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
    return this.cloudNet.scheduleTask(() -> {
      this.addPermanentServiceTask(serviceTask);
      return null;
    });
  }

  @Override
  @NotNull
  public ITask<Void> removePermanentServiceTaskAsync(@NotNull String name) {
    return this.cloudNet.scheduleTask(() -> {
      this.removePermanentServiceTask(name);
      return null;
    });
  }

  @Override
  @NotNull
  public ITask<Void> removePermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
    return this.cloudNet.scheduleTask(() -> {
      this.removePermanentServiceTask(serviceTask);
      return null;
    });
  }

}
