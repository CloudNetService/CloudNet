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

package eu.cloudnetservice.node.impl.provider;

import dev.derklaro.aerogel.PostConstruct;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.base.JavaVersion;
import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.event.task.LocalServiceTaskAddEvent;
import eu.cloudnetservice.node.event.task.LocalServiceTaskRemoveEvent;
import eu.cloudnetservice.node.impl.cluster.sync.DefaultDataSyncHandler;
import eu.cloudnetservice.node.impl.network.listener.message.TaskChannelMessageListener;
import eu.cloudnetservice.node.impl.setup.DefaultInstallation;
import eu.cloudnetservice.node.impl.setup.DefaultTaskSetup;
import eu.cloudnetservice.node.impl.util.JavaVersionResolver;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Provides(ServiceTaskProvider.class)
public class NodeServiceTaskProvider implements ServiceTaskProvider {

  private static final Path TASKS_DIRECTORY = Path.of(
    System.getProperty("cloudnet.config.tasks.directory.path", "local/tasks"));
  private static final Type LIST_DOCUMENT_TYPE = TypeFactory.parameterizedClass(List.class, Document.class);

  private static final Logger LOGGER = LoggerFactory.getLogger(NodeServiceTaskProvider.class);

  private final I18n i18n;
  private final EventManager eventManager;
  private final Map<String, ServiceTask> serviceTasks = new ConcurrentHashMap<>();

  @Inject
  public NodeServiceTaskProvider(
    @NonNull @Service I18n i18n,
    @NonNull EventManager eventManager,
    @NonNull RPCFactory rpcFactory,
    @NonNull DataSyncRegistry syncRegistry,
    @NonNull RPCHandlerRegistry handlerRegistry
  ) {
    this.i18n = i18n;
    this.eventManager = eventManager;

    // rpc
    var rpcHandler = rpcFactory.newRPCHandlerBuilder(ServiceTaskProvider.class).targetInstance(this).build();
    handlerRegistry.registerHandler(rpcHandler);

    // cluster data sync
    syncRegistry.registerHandler(
      DefaultDataSyncHandler.<ServiceTask>builder()
        .key("task")
        .nameExtractor(Named::name)
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
    // register the task locally and notify all event listeners
    var serviceTaskAddEvent = this.eventManager.callEvent(new LocalServiceTaskAddEvent(serviceTask));
    this.addPermanentServiceTaskSilently(serviceTaskAddEvent.task());

    // notify the cluster
    ChannelMessage.builder()
      .targetAll()
      .message("add_service_task")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(serviceTaskAddEvent.task()))
      .build()
      .send();
    return true;
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
    // remove the task locally and notify all event listeners
    this.removePermanentServiceTaskSilently(serviceTask);
    this.eventManager.callEvent(new LocalServiceTaskRemoveEvent(serviceTask));

    // notify the cluster
    ChannelMessage.builder()
      .targetAll()
      .message("remove_service_task")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(serviceTask))
      .build()
      .send();
  }

  @Override
  public @NonNull CompletableFuture<Void> reloadAsync() {
    return TaskUtil.runAsync(this::reload);
  }

  @Override
  public @NonNull CompletableFuture<Collection<ServiceTask>> serviceTasksAsync() {
    return TaskUtil.supplyAsync(this::serviceTasks);
  }

  @Override
  public @NonNull CompletableFuture<ServiceTask> serviceTaskAsync(@NonNull String name) {
    return TaskUtil.supplyAsync(() -> this.serviceTask(name));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> addServiceTaskAsync(@NonNull ServiceTask serviceTask) {
    return TaskUtil.supplyAsync(() -> this.addServiceTask(serviceTask));
  }

  @Override
  public @NonNull CompletableFuture<Void> removeServiceTaskByNameAsync(@NonNull String name) {
    return TaskUtil.runAsync(() -> this.removeServiceTaskByName(name));
  }

  @Override
  public @NonNull CompletableFuture<Void> removeServiceTaskAsync(@NonNull ServiceTask serviceTask) {
    return TaskUtil.runAsync(() -> this.removeServiceTask(serviceTask));
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
    Document.newJsonDocument().appendTree(serviceTask).writeTo(this.taskFile(serviceTask));
  }

  protected void loadServiceTasks() {
    FileUtil.walkFileTree(TASKS_DIRECTORY, ($, file) -> {
      var document = DocumentFactory.json().parse(file);

      // TODO: remove in 4.1
      // check if the task has a name splitter
      if (!document.containsNonNull("nameSplitter")) {
        document.append("nameSplitter", "-");
      }

      // check if the task has environment variables
      var processConfiguration = document.readMutableDocument("processConfiguration");
      if (!processConfiguration.containsNonNull("environmentVariables")) {
        processConfiguration.append("environmentVariables", new HashMap<>());
        document.append("processConfiguration", processConfiguration);
      }

      // inclusions now feature a mandatory cache strategy field.
      // Convert old inclusions that do not have a non-null value already set.
      List<Document> remoteInclusions = document.readObject("includes", LIST_DOCUMENT_TYPE);
      var migratedInclusions = remoteInclusions.stream().map(remoteInclusion -> {
        if (remoteInclusion.containsNonNull("cacheStrategy")) {
          return remoteInclusion;
        }

        return remoteInclusion.mutableCopy().append("cacheStrategy", ServiceRemoteInclusion.NO_CACHE_STRATEGY);
      }).toList();
      document.append("includes", migratedInclusions);

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

        // remove all custom java paths that do not support Java 23
        var javaVersion = JavaVersionResolver.resolveFromJavaExecutable(task.javaCommand());
        if (javaVersion != JavaVersion.JAVA_23) {
          task = ServiceTask.builder(task).javaCommand(null).build();
          LOGGER.warn(this.i18n.translate("cloudnet-load-task-unsupported-java-version", taskName));
        }
      }

      // cache the task
      this.addServiceTask(task);
    }, false, "*.json");
  }
}
