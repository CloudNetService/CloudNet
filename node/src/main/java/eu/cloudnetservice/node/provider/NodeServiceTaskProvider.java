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
import eu.cloudnetservice.common.JavaVersion;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.event.task.LocalServiceTaskAddEvent;
import eu.cloudnetservice.node.event.task.LocalServiceTaskRemoveEvent;
import eu.cloudnetservice.node.network.listener.message.TaskChannelMessageListener;
import eu.cloudnetservice.node.setup.DefaultInstallation;
import eu.cloudnetservice.node.setup.DefaultTaskSetup;
import eu.cloudnetservice.node.util.JavaVersionResolver;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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

@Singleton
@Provides(ServiceTaskProvider.class)
public class NodeServiceTaskProvider implements ServiceTaskProvider {

  private static final Path TASKS_DIRECTORY = Path.of(
    System.getProperty("cloudnet.config.tasks.directory.path", "local/tasks"));

  private static final Logger LOGGER = LogManager.logger(NodeServiceTaskProvider.class);

  private final EventManager eventManager;
  private final Map<String, ServiceTask> serviceTasks = new ConcurrentHashMap<>();

  @Inject
  public NodeServiceTaskProvider(
    @NonNull EventManager eventManager,
    @NonNull RPCFactory rpcFactory,
    @NonNull DataSyncRegistry syncRegistry,
    @NonNull RPCHandlerRegistry handlerRegistry
  ) {
    this.eventManager = eventManager;

    // rpc
    rpcFactory.newHandler(ServiceTaskProvider.class, this).registerTo(handlerRegistry);

    // cluster data sync
    syncRegistry.registerHandler(
      DataSyncHandler.<ServiceTask>builder()
        .key("task")
        .nameExtractor(Nameable::name)
        .convertObject(ServiceTask.class)
        .writer(this::addPermanentServiceTaskSilently)
        .dataCollector(this::serviceTasks)
        .currentGetter(task -> this.serviceTask(task.name()))
        .build());
  }

  @Inject
  private void loadTasks(@NonNull DefaultInstallation installation) {
    if (Files.exists(TASKS_DIRECTORY)) {
      this.loadServiceTasks();
    } else {
      FileUtil.createDirectory(TASKS_DIRECTORY);
      installation.registerSetup(DefaultTaskSetup.class);
    }
  }

  @PostConstruct
  private void registerTaskChannelListener() {
    this.eventManager.registerListener(TaskChannelMessageListener.class);
  }

  @Override
  public void reload() {
    // clear the cache
    this.serviceTasks.clear();
    // load all service tasks
    this.loadServiceTasks();
  }

  @Override
  public @NonNull Collection<ServiceTask> serviceTasks() {
    return Collections.unmodifiableCollection(this.serviceTasks.values());
  }

  @Override
  public @Nullable ServiceTask serviceTask(@NonNull String name) {
    return this.serviceTasks.get(name);
  }

  @Override
  public boolean addServiceTask(@NonNull ServiceTask serviceTask) {
    if (!this.eventManager.callEvent(new LocalServiceTaskAddEvent(serviceTask)).cancelled()) {
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
  public void removeServiceTaskByName(@NonNull String name) {
    var task = this.serviceTask(name);
    if (task != null) {
      this.removeServiceTask(task);
    }
  }

  @Override
  public void removeServiceTask(@NonNull ServiceTask serviceTask) {
    if (!this.eventManager.callEvent(new LocalServiceTaskRemoveEvent(serviceTask)).cancelled()) {
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

  public void addPermanentServiceTaskSilently(@NonNull ServiceTask serviceTask) {
    // cache locally
    this.serviceTasks.put(serviceTask.name(), serviceTask);
    this.writeServiceTask(serviceTask);
  }

  public void removePermanentServiceTaskSilently(@NonNull ServiceTask serviceTask) {
    // remove from cache
    this.serviceTasks.remove(serviceTask.name());
    // remove the local file if it exists
    FileUtil.delete(this.taskFile(serviceTask));
  }

  protected @NonNull Path taskFile(@NonNull ServiceTask task) {
    return TASKS_DIRECTORY.resolve(task.name() + ".json");
  }

  protected void writeServiceTask(@NonNull ServiceTask serviceTask) {
    JsonDocument.newDocument(serviceTask).write(this.taskFile(serviceTask));
  }

  protected void loadServiceTasks() {
    FileUtil.walkFileTree(TASKS_DIRECTORY, ($, file) -> {
      var document = JsonDocument.newDocument(file);

      // TODO: remove in 4.1
      // check if the task has a name splitter
      if (!document.contains("nameSplitter")) {
        document.append("nameSplitter", "-");
      }

      // check if the task has environment variables
      var processConfiguration = document.getDocument("processConfiguration");
      if (!processConfiguration.contains("environmentVariables")) {
        processConfiguration.append("environmentVariables", new HashMap<>());
        document.append("processConfiguration", processConfiguration);
      }

      // load the service task
      var task = document.toInstanceOf(ServiceTask.class);
      // check if the file name is still up-to-date
      var taskName = file.getFileName().toString().replace(".json", "");
      if (!taskName.equals(task.name())) {
        // rename the file
        FileUtil.move(file, this.taskFile(task), StandardCopyOption.REPLACE_EXISTING);
      }

      // Wrap the command to a path and unwrap it again to ensure that the command is os specific
      // This allows for example Windows users to use '/' in the task file which is way easier as there
      // is no need to escape. ('\' must be escaped)
      var javaCommand = task.javaCommand();
      if (javaCommand != null && !javaCommand.equals("java")) {
        var command = Path.of(javaCommand).toAbsolutePath().normalize().toString();
        // validate if the task java command needs an update
        if (!javaCommand.equals(command)) {
          task = ServiceTask.builder(task).javaCommand(command).build();
        }

        // remove all custom java paths that do not support Java 17
        var javaVersion = JavaVersionResolver.resolveFromJavaExecutable(task.javaCommand());
        if (javaVersion == null || !javaVersion.isNewerOrAt(JavaVersion.JAVA_17)) {
          task = ServiceTask.builder(task).javaCommand(null).build();
          LOGGER.warning(I18n.trans("cloudnet-load-task-unsupported-java-version", taskName));
        }
      }

      // cache the task
      this.addServiceTask(task);
    }, false, "*.json");
  }
}
