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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Range;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotations.ConsoleOnly;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.util.JavaVersionResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommandTasks {

  @Parser(suggestions = "serviceTask")
  public ServiceTask defaultTaskParser(CommandContext<CommandSource> $, Queue<String> input) {
    String name = input.remove();
    ServiceTask task = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(name);
    if (task == null) {
      throw new ArgumentNotAvailableException("No task found");
    }

    return task;
  }

  @Suggestions("serviceTask")
  public List<String> suggestTask(CommandContext<CommandSource> $, String input) {
    return this.taskProvider().getPermanentServiceTasks().stream().map(INameable::getName).collect(Collectors.toList());
  }

  @Parser
  public Pair<String, JavaVersion> javaCommandParser(CommandContext<CommandSource> $, Queue<String> input) {
    String command = String.join(" ", input);
    JavaVersion version = JavaVersionResolver.resolveFromJavaExecutable(command);
    if (version == null) {
      throw new ArgumentNotAvailableException("Version not resolved");
    }

    return new Pair<>(command, version);
  }

  @Parser(name = "nodeId", suggestions = "clusterNode")
  public String defaultClusterNodeParser(CommandContext<CommandSource> $, Queue<String> input) {
    String nodeId = input.remove();
    for (NetworkClusterNode node : CloudNet.getInstance().getConfig().getClusterConfig().getNodes()) {
      if (node.getUniqueId().equals(nodeId)) {
        return nodeId;
      }
    }
    throw new ArgumentNotAvailableException("That node does not exist");
  }

  @Suggestions("clusterNode")
  public List<String> suggestNode(CommandContext<CommandSource> $, String input) {
    return CloudNet.getInstance().getConfig().getClusterConfig().getNodes()
      .stream()
      .map(NetworkClusterNode::getUniqueId)
      .collect(Collectors.toList());
  }

  @ConsoleOnly
  @CommandMethod("tasks setup")
  public void taskSetup(CommandSource source) {
    //TODO: start the setup
  }

  @CommandMethod("tasks reload")
  public void reloadTasks(CommandSource source) {
    this.taskProvider().reload();
  }

  @CommandMethod("tasks list")
  public void listTasks(CommandSource source) {
    for (ServiceTask task : this.taskProvider().getPermanentServiceTasks()) {
      this.singleTaskInfo(source, task);
    }
  }

  @CommandMethod("tasks create <name> <environment>")
  public void createTask(CommandSource source,
    @Argument("name") String taskName,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    if (this.taskProvider().isServiceTaskPresent(taskName)) {
      source.sendMessage("already exists");
      return;
    }

    ServiceTask serviceTask = ServiceTask.builder()
      .templates(Collections.singletonList(ServiceTemplate.local(taskName, "default")))
      .name(taskName)
      .autoDeleteOnStop(true)
      .groups(Collections.singletonList(taskName))
      .serviceEnvironmentType(environmentType)
      .maxHeapMemory(environmentType.isMinecraftProxy() ? 256 : 512)
      .startPort(environmentType.getDefaultStartPort())
      .build();
    this.taskProvider().addPermanentServiceTask(serviceTask);
  }

  @CommandMethod("tasks task <name>")
  public void displayTask(CommandSource source, @Argument("name") ServiceTask task) {
    Collection<String> messages = new ArrayList<>();
    messages.add("Name: " + task.getName());
    messages.add("Groups: " + Arrays.toString(task.getGroups().toArray()));
    messages.add("Max Heap: " + task.getProcessConfiguration().getMaxHeapMemorySize());
    messages.add("Maintenance: " + task.isMaintenance());
    messages.add(
      "Nodes:" + (task.getAssociatedNodes().isEmpty() ? "All"
        : Arrays.toString(task.getAssociatedNodes().toArray())));
    messages.add("Minimal Services: " + task.getMinServiceCount());
    messages.add("Java Command: " + task.getJavaCommand());
    messages.add("Start Port: " + task.getStartPort());
    messages.add("Static services: " + task.isStaticServices());
    messages.add("Auto delete on stop: " + task.isAutoDeleteOnStop());
    messages.add("Deleted files after stop: " + Arrays.toString(task.getDeletedFilesAfterStop().toArray()));
    messages.add("Environment: " + task.getProcessConfiguration().getEnvironment());

    source.sendMessage(messages);
  }

  @CommandMethod("tasks task <name> set minServiceCount <amount>")
  public void setMinServiceCount(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument("amount") @Range(min = "0") Integer amount
  ) {
    this.updateTask(task, serviceTask -> serviceTask.setMinServiceCount(amount));
  }

  @CommandMethod("tasks task <name> set maintenance <enabled>")
  public void setMaintenance(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument("enabled") boolean enabled
  ) {
    this.updateTask(task, serviceTask -> serviceTask.setMaintenance(enabled));
  }

  @CommandMethod("tasks task <name> set maxHeapMemory <amount>")
  public void setMaxHeapMemory(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument("amount") @Range(min = "0") Integer amount
  ) {
    this.updateTask(task, serviceTask -> serviceTask.getProcessConfiguration().setMaxHeapMemorySize(amount));
  }

  @CommandMethod("tasks task <name> set startPort <amount>")
  public void setStartPort(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument("amount") @Range(min = "0") Integer amount
  ) {
    this.updateTask(task, serviceTask -> serviceTask.setStartPort(amount));
  }

  @CommandMethod("tasks task <name> set autoDeleteOnStop <enabled>")
  public void setAutoDeleteOnStop(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument("enabled") boolean enabled
  ) {
    this.updateTask(task, serviceTask -> serviceTask.setAutoDeleteOnStop(enabled));
  }

  @CommandMethod("tasks task <name> set staticServices <enabled>")
  public void setStaticServices(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument("enabled") boolean enabled
  ) {
    this.updateTask(task, serviceTask -> serviceTask.setStaticServices(enabled));
  }

  @CommandMethod("tasks task <name> set environment <environment>")
  public void setEnvironment(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    this.updateTask(task, serviceTask -> serviceTask.getProcessConfiguration().setEnvironment(environmentType));
  }

  @CommandMethod("tasks task <name> set disableIpRewrite <enabled>")
  public void setDisableIpRewrite(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument("enabled") boolean enabled
  ) {
    this.updateTask(task, serviceTask -> serviceTask.setDisableIpRewrite(enabled));
  }

  @CommandMethod("tasks task <name> set javaCommand <executable>")
  public void setJavaCommand(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument("executable") Pair<String, JavaVersion> executable
  ) {
    this.updateTask(task, serviceTask -> serviceTask.setJavaCommand(executable.getFirst()));
  }

  @CommandMethod("tasks task <name> add node <uniqueId>")
  public void addNode(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument(value = "uniqueId", parserName = "nodeId") String node
  ) {
    if (task.getAssociatedNodes().contains(node)) {
      return;
    }

    this.updateTask(task, serviceTask -> serviceTask.getAssociatedNodes().add(node));
  }

  @CommandMethod("tasks task <name> add group <group>")
  public void addGroup(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument(value = "group") GroupConfiguration group
  ) {
    if (task.getGroups().contains(group.getName())) {
      return;
    }

    this.updateTask(task, serviceTask -> serviceTask.getGroups().add(group.getName()));
  }

  @CommandMethod("tasks task <name> remove node <uniqueId>")
  public void removeNode(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument(value = "uniqueId") String node
  ) {
    this.updateTask(task, serviceTask -> serviceTask.getAssociatedNodes().remove(node));
  }

  @CommandMethod("tasks task <name> remove group <group>")
  public void removeGroup(
    CommandSource source,
    @Argument("name") ServiceTask task,
    @Argument(value = "group") String group
  ) {
    this.updateTask(task, serviceTask -> serviceTask.getGroups().remove(group));
  }

  private void updateTask(ServiceTask task, Consumer<ServiceTask> consumer) {
    consumer.accept(task);
    this.taskProvider().addPermanentServiceTask(task);
  }

  private void singleTaskInfo(CommandSource source, ServiceTask task) {
    source.sendMessage(task.getName() +
      " | MinServiceCount: " + task.getMinServiceCount() +
      " | Maintenance: " + task.isMaintenance() +
      " | Nodes: " + (task.getAssociatedNodes().isEmpty() ? "All"
      : Arrays.toString(task.getAssociatedNodes().toArray())) +
      " | StartPort: " + task.getStartPort()
    );
  }

  public ServiceTaskProvider taskProvider() {
    return CloudNet.getInstance().getServiceTaskProvider();
  }

}
