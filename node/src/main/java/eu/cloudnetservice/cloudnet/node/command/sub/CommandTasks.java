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

package eu.cloudnetservice.cloudnet.node.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Liberal;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.specifier.Range;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.cloudnet.common.JavaVersion;
import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.WildcardUtil;
import eu.cloudnetservice.cloudnet.common.collection.Pair;
import eu.cloudnetservice.cloudnet.common.column.ColumnFormatter;
import eu.cloudnetservice.cloudnet.common.column.RowBasedFormatter;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.cloudnet.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.cloudnet.driver.service.GroupConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ServiceConfigurationBase;
import eu.cloudnetservice.cloudnet.driver.service.ServiceDeployment;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTask;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.command.source.ConsoleCommandSource;
import eu.cloudnetservice.cloudnet.node.console.Console;
import eu.cloudnetservice.cloudnet.node.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.cloudnet.node.setup.SpecificTaskSetup;
import eu.cloudnetservice.cloudnet.node.util.JavaVersionResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandPermission("cloudnet.command.tasks")
@Description("Administers the configurations of all persistent tasks")
public final class CommandTasks {

  // Task Setup ASCII
  private static final ConsoleSetupAnimation TASK_SETUP = new ConsoleSetupAnimation(
    """
      &f _____              _       &b           _                \s
      &f/__   \\  __ _  ___ | | __  &b ___   ___ | |_  _   _  _ __ \s
      &f  / /\\/ / _` |/ __|| |/ /  &b/ __| / _ \\| __|| | | || '_ \\\s
      &f / /   | (_| |\\__ \\|   <  &b \\__ \\|  __/| |_ | |_| || |_) |
      &f \\/     \\__,_||___/|_|\\_\\&b  |___/ \\___| \\__| \\__,_|| .__/\s
      &f                             &b                     |_|   \s""",
    "Task creation complete!",
    "&r> &e");
  // Formatter for the table based looking
  private static final RowBasedFormatter<ServiceTask> TASK_LIST_FORMATTER = RowBasedFormatter.<ServiceTask>builder()
    .defaultFormatter(ColumnFormatter.builder()
      .columnTitles("Name", "MinServiceCount", "Maintenance", "Nodes", "StartPort")
      .build())
    .column(ServiceTask::name)
    .column(ServiceTask::minServiceCount)
    .column(ServiceTask::maintenance)
    .column(task -> task.associatedNodes().isEmpty() ? "All" : String.join(", ", task.associatedNodes()))
    .column(ServiceTask::startPort)
    .build();

  private final Console console;

  public CommandTasks(@NonNull Console console) {
    this.console = console;
  }

  public static void applyServiceConfigurationDisplay(
    @NonNull Collection<String> messages,
    @NonNull ServiceConfigurationBase configurationBase) {
    messages.add(" ");

    messages.add("Includes:");

    for (var inclusion : configurationBase.includes()) {
      messages.add("- " + inclusion.url() + " => " + inclusion.destination());
    }

    messages.add(" ");
    messages.add("Templates:");

    for (var template : configurationBase.templates()) {
      messages.add("- " + template);
    }

    messages.add(" ");
    messages.add("Deployments:");

    for (var deployment : configurationBase.deployments()) {
      messages.add("- ");
      messages.add(
        "Template:  " + deployment.template());
      messages.add("Excludes: " + deployment.excludes());
    }

    messages.add(" ");
    messages.add("JVM Options:");

    for (var jvmOption : configurationBase.jvmOptions()) {
      messages.add("- " + jvmOption);
    }

    messages.add(" ");
    messages.add("Process Parameters:");

    for (var processParameters : configurationBase.processParameters()) {
      messages.add("- " + processParameters);
    }

    messages.add(" ");

    messages.add("Properties: ");

    messages.addAll(Arrays.asList(configurationBase.properties().toPrettyJson().split("\n")));
    messages.add(" ");
  }

  @Parser(suggestions = "serviceTask")
  public ServiceTask defaultTaskParser(CommandContext<CommandSource> $, Queue<String> input) {
    var name = input.remove();
    var task = CloudNet.instance().serviceTaskProvider().serviceTask(name);
    if (task == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-tasks-task-not-found"));
    }

    return task;
  }

  @Suggestions("serviceTask")
  public List<String> suggestTask(CommandContext<CommandSource> $, String input) {
    return this.taskProvider().permanentServiceTasks().stream().map(Nameable::name).toList();
  }

  @Parser(suggestions = "serviceTask")
  public Collection<ServiceTask> wildcardTaskParser(CommandContext<CommandSource> $, Queue<String> input) {
    var name = input.remove();
    var matchedTasks = WildcardUtil.filterWildcard(
      this.taskProvider().permanentServiceTasks(),
      name);
    if (matchedTasks.isEmpty()) {
      throw new ArgumentNotAvailableException(I18n.trans("command-tasks-task-not-found"));
    }

    return matchedTasks;
  }

  @Parser(name = "javaCommand")
  public Pair<String, JavaVersion> javaCommandParser(CommandContext<CommandSource> $, Queue<String> input) {
    var command = String.join(" ", input);
    var version = JavaVersionResolver.resolveFromJavaExecutable(command);
    if (version == null) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-tasks-setup-question-javacommand-invalid"));
    }

    return new Pair<>(command, version);
  }

  @Parser(name = "nodeId", suggestions = "clusterNode")
  public String defaultClusterNodeParser(CommandContext<CommandSource> $, Queue<String> input) {
    var nodeId = input.remove();
    for (var node : CloudNet.instance().config().clusterConfig().nodes()) {
      if (node.uniqueId().equals(nodeId)) {
        return nodeId;
      }
    }

    throw new ArgumentNotAvailableException(I18n.trans("command-tasks-node-not-found"));
  }

  @Suggestions("clusterNode")
  public List<String> suggestNode(CommandContext<CommandSource> $, String input) {
    return CloudNet.instance().config().clusterConfig().nodes()
      .stream()
      .map(NetworkClusterNode::uniqueId)
      .toList();
  }

  @CommandMethod(value = "tasks setup", requiredSender = ConsoleCommandSource.class)
  public void taskSetup(CommandSource source) {
    var setup = new SpecificTaskSetup();
    setup.applyQuestions(TASK_SETUP);

    TASK_SETUP.addFinishHandler(() -> setup.handleResults(TASK_SETUP));

    this.console.startAnimation(TASK_SETUP);
  }

  @CommandMethod("tasks reload")
  public void reloadTasks(CommandSource source) {
    this.taskProvider().reload();
    source.sendMessage(I18n.trans("command-tasks-reload-success"));
  }

  @CommandMethod("tasks delete <name>")
  public void deleteTask(CommandSource source, @Argument("name") Collection<ServiceTask> serviceTasks) {
    for (var serviceTask : serviceTasks) {
      this.taskProvider().removePermanentServiceTask(serviceTask);
      source.sendMessage(I18n.trans("command-tasks-delete-task"));
    }
  }

  @CommandMethod("tasks list")
  public void listTasks(CommandSource source) {
    source.sendMessage(TASK_LIST_FORMATTER.format(this.taskProvider().permanentServiceTasks()));
  }

  @CommandMethod("tasks create <name> <environment>")
  public void createTask(CommandSource source,
    @Argument("name") String taskName,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    if (this.taskProvider().serviceTaskPresent(taskName)) {
      source.sendMessage(I18n.trans("command-tasks-task-already-existing"));
      return;
    }

    var serviceTask = ServiceTask.builder()
      .addTemplate(ServiceTemplate.builder().prefix(taskName).name("default").build())
      .name(taskName)
      .autoDeleteOnStop(true)
      .groups(Collections.singletonList(taskName))
      .serviceEnvironmentType(environmentType)
      .maxHeapMemory(512)
      .startPort(environmentType.defaultStartPort())
      .build();
    this.taskProvider().addPermanentServiceTask(serviceTask);
    source.sendMessage(I18n.trans("command-tasks-create-task"));
  }

  @CommandMethod("tasks task <name>")
  public void displayTask(CommandSource source, @Argument("name") Collection<ServiceTask> serviceTasks) {
    for (var serviceTask : serviceTasks) {
      Collection<String> messages = new ArrayList<>();
      messages.add("Name: " + serviceTask.name());
      messages.add("Groups: " + Arrays.toString(serviceTask.groups().toArray()));
      messages.add("Max heap memory: " + serviceTask.processConfiguration().maxHeapMemorySize());
      messages.add("Maintenance: " + serviceTask.maintenance());
      messages.add(
        "Nodes:" + (serviceTask.associatedNodes().isEmpty() ? "All"
          : Arrays.toString(serviceTask.associatedNodes().toArray())));
      messages.add("Minimal Services: " + serviceTask.minServiceCount());
      messages.add("Java Command: " + serviceTask.javaCommand());
      messages.add("Start Port: " + serviceTask.startPort());
      messages.add("Static services: " + serviceTask.staticServices());
      messages.add("Auto delete on stop: " + serviceTask.autoDeleteOnStop());
      messages.add("Deleted files after stop: " + Arrays.toString(serviceTask.deletedFilesAfterStop().toArray()));
      messages.add("Environment: " + serviceTask.processConfiguration().environment());

      applyServiceConfigurationDisplay(messages, serviceTask);
      source.sendMessage(messages);
    }
  }

  @CommandMethod("tasks task <name> set minServiceCount <amount>")
  public void setMinServiceCount(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("amount") @Range(min = "0") Integer amount
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.minServiceCount(amount));
      source.sendMessage(
        I18n.trans("command-tasks-set-property-success")
          .replace("%property%", "minServiceCount")
          .replace("%name%", task.name())
          .replace("%value%", amount.toString())
      );
    }
  }

  @CommandMethod("tasks task <name> set maintenance <enabled>")
  public void setMaintenance(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("enabled") @Liberal boolean enabled
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.maintenance(enabled));
      source.sendMessage(I18n.trans("command-tasks-set-property-success",
        "maintenance",
        task.name(),
        enabled));
    }
  }

  @CommandMethod("tasks task <name> set maxHeapMemory <amount>")
  public void setMaxHeapMemory(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("amount") @Range(min = "0") Integer amount
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.maxHeapMemory(amount));
      source.sendMessage(I18n.trans("command-tasks-set-property-success", "maxHeapMemory", task.name(), amount));
    }
  }

  @CommandMethod("tasks task <name> set startPort <amount>")
  public void setStartPort(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("amount") @Range(min = "0") Integer amount
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.startPort(amount));
      source.sendMessage(I18n.trans("command-tasks-set-property-success", "startPort", task.name(), amount));
    }
  }

  @CommandMethod("tasks task <name> set staticServices <enabled>")
  public void setStaticServices(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("enabled") @Liberal boolean enabled
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.staticServices(enabled));
      source.sendMessage(I18n.trans("command-tasks-set-property-success", "staticServices", task.name(), enabled));
    }
  }

  @CommandMethod("tasks task <name> set environment <environment>")
  public void setEnvironment(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.serviceEnvironmentType(environmentType));
      source.sendMessage(I18n.trans("command-tasks-set-property-success",
        "environment",
        task.name(),
        environmentType.name()));
    }
  }

  @CommandMethod("tasks task <name> set disableIpRewrite <enabled>")
  public void setDisableIpRewrite(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("enabled") @Liberal boolean enabled
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.disableIpRewrite(enabled));
      source.sendMessage(I18n.trans("command-tasks-set-property-success", "disableIpRewrite", task.name(), enabled));
    }
  }

  @CommandMethod("tasks task <name> set javaCommand <executable>")
  public void setJavaCommand(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument(value = "executable", parserName = "javaCommand") @Quoted Pair<String, JavaVersion> executable
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.javaCommand(executable.first()));
      source.sendMessage(
        I18n.trans("command-tasks-set-property-success", "javaCommand", task.name(), executable.first()));
    }
  }

  @CommandMethod("tasks task <name> add node <uniqueId>")
  public void addNode(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument(value = "uniqueId", parserName = "nodeId") String node
  ) {
    for (var task : serviceTasks) {
      if (task.associatedNodes().contains(node)) {
        continue;
      }

      this.updateTask(task, builder -> builder.addAssociatedNode(node));
      source.sendMessage(I18n.trans("command-tasks-add-collection-property", "node", node, task.name()));
    }
  }

  @CommandMethod("tasks task <name> add group <group>")
  public void addGroup(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("group") GroupConfiguration group
  ) {
    for (var task : serviceTasks) {
      if (task.groups().contains(group.name())) {
        continue;
      }

      this.updateTask(task, builder -> builder.addGroup(group.name()));
      source.sendMessage(I18n.trans("command-tasks-add-collection-property", "group", group, task.name()));
    }
  }

  @CommandMethod("tasks task <name> remove node <uniqueId>")
  public void removeNode(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("uniqueId") String node
  ) {
    for (var task : serviceTasks) {
      this.updateTaskDirect(task, serviceTask -> serviceTask.associatedNodes().remove(node));
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property", "node", node, task.name()));
    }
  }

  @CommandMethod("tasks task <name> remove group <group>")
  public void removeGroup(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("group") String group
  ) {
    for (var task : serviceTasks) {
      this.updateTaskDirect(task, serviceTask -> serviceTask.groups().remove(group));
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property", "group", group, task.name()));
    }
  }

  @CommandMethod("tasks task <name> add deployment <deployment>")
  public void addDeployment(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("deployment") ServiceTemplate template,
    @Flag("excludes") @Quoted String excludes
  ) {
    var deployment = ServiceDeployment.builder()
      .template(template)
      .excludes(this.parseExcludes(excludes))
      .build();
    for (var serviceTask : serviceTasks) {
      this.updateTask(serviceTask, builder -> builder.addDeployment(deployment));
      source.sendMessage(I18n.trans("command-tasks-add-collection-property",
        "deployment",
        deployment.template(),
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> add template <template>")
  public void addTemplate(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("template") ServiceTemplate template
  ) {
    for (var serviceTask : serviceTasks) {
      this.updateTask(serviceTask, task -> task.addTemplate(template));
      source.sendMessage(I18n.trans("command-tasks-add-collection-property", "template", template, serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> add inclusion <url> <path>")
  public void addInclusion(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("url") String url,
    @Argument("path") String path
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();

    for (var serviceTask : serviceTasks) {
      this.updateTask(serviceTask, task -> task.addInclude(inclusion));
      source.sendMessage(I18n.trans("command-tasks-add-collection-property",
        "inclusion",
        inclusion,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> add jvmOption <options>")
  public void addJvmOption(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Greedy @Argument("options") String jvmOptions
  ) {
    Collection<String> splittedOptions = Arrays.asList(jvmOptions.split(" "));
    for (var serviceTask : serviceTasks) {
      serviceTask.jvmOptions().addAll(splittedOptions);
      this.updateTask(serviceTask);
      source.sendMessage(I18n.trans("command-tasks-add-collection-property",
        "jvmOption",
        jvmOptions,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> add processParameter <options>")
  public void addProcessParameter(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Greedy @Argument("options") String processParameters
  ) {
    Collection<String> splittedOptions = Arrays.asList(processParameters.split(" "));
    for (var serviceTask : serviceTasks) {
      serviceTask.processParameters().addAll(splittedOptions);
      this.updateTask(serviceTask);
      source.sendMessage(I18n.trans("command-tasks-add-collection-property",
        "processParameter",
        processParameters,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> remove deployment <deployment>")
  public void removeDeployment(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("deployment") ServiceTemplate template,
    @Flag("excludes") @Quoted String excludes
  ) {
    var deployment = ServiceDeployment.builder()
      .template(template)
      .excludes(this.parseExcludes(excludes))
      .build();
    for (var serviceTask : serviceTasks) {
      this.updateTaskDirect(serviceTask, task -> task.deployments().remove(deployment));
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property",
        "deployment",
        template,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> remove template <template>")
  public void removeTemplate(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("template") ServiceTemplate template
  ) {
    for (var serviceTask : serviceTasks) {
      this.updateTaskDirect(serviceTask, task -> task.templates().remove(template));
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property",
        "template",
        template,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> remove inclusion <url> <path>")
  public void removeInclusion(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("url") String url,
    @Argument("path") String path
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();

    for (var serviceTask : serviceTasks) {
      this.updateTaskDirect(serviceTask, task -> task.includes().remove(inclusion));
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property",
        "inclusion",
        inclusion,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> remove jvmOption <options>")
  public void removeJvmOption(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Greedy @Argument("options") String jvmOptions
  ) {
    Collection<String> splittedOptions = Arrays.asList(jvmOptions.split(" "));
    for (var serviceTask : serviceTasks) {
      serviceTask.jvmOptions().removeAll(splittedOptions);
      this.updateTask(serviceTask);
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property",
        "jvmOption",
        jvmOptions,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> remove processParameter <options>")
  public void removeProcessParameter(
    CommandSource source,
    @Argument("name") Collection<ServiceTask> serviceTasks,
    @Greedy @Argument("options") String processParameters
  ) {
    Collection<String> splittedOptions = Arrays.asList(processParameters.split(" "));
    for (var serviceTask : serviceTasks) {
      serviceTask.processParameters().removeAll(splittedOptions);
      this.updateTask(serviceTask);
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property",
        "processParamter",
        processParameters,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> clear jvmOptions")
  public void clearJvmOptions(CommandSource source, @Argument("name") Collection<ServiceTask> serviceTasks) {
    for (var serviceTask : serviceTasks) {
      this.updateTaskDirect(serviceTask, task -> task.jvmOptions().clear());
      source.sendMessage(I18n.trans("command-service-base-clear-property",
        "jvmOptions",
        "task",
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> clear processParameters")
  public void clearProcessParameter(CommandSource source, @Argument("name") Collection<ServiceTask> serviceTasks) {
    for (var serviceTask : serviceTasks) {
      this.updateTaskDirect(serviceTask, task -> task.processParameters().clear());
      source.sendMessage(I18n.trans("command-service-base-clear-property",
        "processParameters",
        "task",
        serviceTask.name()));
    }
  }

  private void updateTask(@NonNull ServiceTask task) {
    this.taskProvider().addPermanentServiceTask(task);
  }

  private void updateTask(@NonNull ServiceTask task, @NonNull Consumer<ServiceTask.Builder> consumer) {
    consumer
      .andThen(result -> this.updateTask(result.build()))
      .accept(ServiceTask.builder(task));
  }

  private void updateTaskDirect(@NonNull ServiceTask task, @NonNull Consumer<ServiceTask> consumer) {
    consumer.andThen(this::updateTask).accept(task);
  }

  private @NonNull Collection<String> parseExcludes(@Nullable String excludes) {
    if (excludes == null) {
      return Collections.emptyList();
    }

    return Arrays.asList(excludes.split(";"));
  }

  private @NonNull ServiceTaskProvider taskProvider() {
    return CloudNet.instance().serviceTaskProvider();
  }

}
