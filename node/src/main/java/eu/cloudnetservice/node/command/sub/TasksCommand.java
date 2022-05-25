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

package eu.cloudnetservice.node.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.Regex;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Liberal;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.specifier.Range;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.JavaVersion;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.WildcardUtil;
import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowBasedFormatter;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceConfigurationBase;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.command.source.ConsoleCommandSource;
import eu.cloudnetservice.node.console.Console;
import eu.cloudnetservice.node.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.node.setup.SpecificTaskSetup;
import eu.cloudnetservice.node.util.JavaVersionResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;

@CommandPermission("cloudnet.command.tasks")
@Description("Administers the configurations of all persistent tasks")
public final class TasksCommand {

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

  public TasksCommand(@NonNull Console console) {
    this.console = console;
  }

  public static void applyServiceConfigurationDisplay(
    @NonNull Collection<String> messages,
    @NonNull ServiceConfigurationBase configurationBase
  ) {
    messages.add(" ");

    messages.add("Includes:");

    for (var inclusion : configurationBase.inclusions()) {
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
  public @NonNull ServiceTask defaultTaskParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var name = input.remove();
    var task = Node.instance().serviceTaskProvider().serviceTask(name);
    if (task == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-tasks-task-not-found"));
    }

    return task;
  }

  @Suggestions("serviceTask")
  public @NonNull List<String> suggestTask(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.taskProvider().serviceTasks().stream().map(Nameable::name).toList();
  }

  @Parser(suggestions = "serviceTask")
  public @NonNull Collection<ServiceTask> wildcardTaskParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var name = input.remove();
    var matchedTasks = WildcardUtil.filterWildcard(this.taskProvider().serviceTasks(), name);
    if (matchedTasks.isEmpty()) {
      throw new ArgumentNotAvailableException(I18n.trans("command-tasks-task-not-found"));
    }

    return matchedTasks;
  }

  @Parser(name = "javaCommand")
  public @NonNull Pair<String, JavaVersion> javaCommandParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var command = String.join(" ", input);
    var version = JavaVersionResolver.resolveFromJavaExecutable(command);
    if (version == null) {
      throw new ArgumentNotAvailableException(
        I18n.trans("command-tasks-setup-question-javacommand-invalid"));
    }

    return new Pair<>(command, version);
  }

  @Parser(name = "nodeId", suggestions = "clusterNode")
  public @NonNull String defaultClusterNodeParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var nodeId = input.remove();
    for (var node : Node.instance().clusterNodeProvider().nodes()) {
      if (node.uniqueId().equals(nodeId)) {
        return nodeId;
      }
    }
    throw new ArgumentNotAvailableException(I18n.trans("command-tasks-node-not-found"));
  }

  @Suggestions("clusterNode")
  public @NonNull List<String> suggestNode(@NonNull CommandContext<CommandSource> $, @NonNull String input) {
    return Node.instance().clusterNodeProvider().nodes()
      .stream()
      .map(NetworkClusterNode::uniqueId)
      .toList();
  }

  @CommandMethod(value = "tasks setup", requiredSender = ConsoleCommandSource.class)
  public void taskSetup(@NonNull CommandSource source) {
    var setup = new SpecificTaskSetup();
    setup.applyQuestions(TASK_SETUP);

    TASK_SETUP.addFinishHandler(() -> setup.handleResults(TASK_SETUP));

    this.console.startAnimation(TASK_SETUP);
  }

  @CommandMethod("tasks reload")
  public void reloadTasks(@NonNull CommandSource source) {
    this.taskProvider().reload();
    source.sendMessage(I18n.trans("command-tasks-reload-success"));
  }

  @CommandMethod("tasks delete <name>")
  public void deleteTask(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks
  ) {
    for (var serviceTask : serviceTasks) {
      this.taskProvider().removeServiceTask(serviceTask);
      source.sendMessage(I18n.trans("command-tasks-delete-task"));
    }
  }

  @CommandMethod("tasks list")
  public void listTasks(@NonNull CommandSource source) {
    source.sendMessage(TASK_LIST_FORMATTER.format(this.taskProvider().serviceTasks()));
  }

  @CommandMethod("tasks create <name> <environment>")
  public void createTask(
    @NonNull CommandSource source,
    @NonNull @Regex(ServiceTask.NAMING_REGEX) @Argument("name") String taskName,
    @NonNull @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    if (this.taskProvider().serviceTask(taskName) != null) {
      source.sendMessage(I18n.trans("command-tasks-task-already-existing"));
      return;
    }

    var serviceTask = ServiceTask.builder()
      .templates(Set.of(ServiceTemplate.builder().prefix(taskName).name("default").build()))
      .name(taskName)
      .autoDeleteOnStop(true)
      .groups(List.of(taskName))
      .serviceEnvironmentType(environmentType)
      .maxHeapMemory(512)
      .startPort(environmentType.defaultStartPort())
      .build();
    this.taskProvider().addServiceTask(serviceTask);
    source.sendMessage(I18n.trans("command-tasks-create-task"));
  }

  @CommandMethod("tasks task <name>")
  public void displayTask(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks
  ) {
    for (var serviceTask : serviceTasks) {
      Collection<String> messages = new ArrayList<>();
      messages.add("Name: " + serviceTask.name());
      messages.add("Splitter: " + serviceTask.nameSplitter());
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

  @CommandMethod("tasks task <name> set nameSplitter <splitter>")
  public void setNameSplitter(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Regex(ServiceTask.NAMING_REGEX) @Argument("splitter") String splitter
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.nameSplitter(splitter));
      source.sendMessage(I18n.trans("command-tasks-set-property-success",
        "nameSplitter",
        task.name(),
        splitter));
    }
  }

  @CommandMethod("tasks task <name> set minServiceCount <amount>")
  public void setMinServiceCount(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("amount") @Range(min = "0") int amount
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.minServiceCount(amount));
      source.sendMessage(I18n.trans("command-tasks-set-property-success",
        "minServiceCount",
        task.name(),
        amount));
    }
  }

  @CommandMethod("tasks task <name> set maintenance <enabled>")
  public void setMaintenance(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
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
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("amount") @Range(min = "0") int amount
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.maxHeapMemory(amount));
      source.sendMessage(I18n.trans("command-tasks-set-property-success", "maxHeapMemory", task.name(), amount));
    }
  }

  @CommandMethod("tasks task <name> set startPort <amount>")
  public void setStartPort(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("amount") @Range(min = "0") int amount
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.startPort(amount));
      source.sendMessage(I18n.trans("command-tasks-set-property-success", "startPort", task.name(), amount));
    }
  }

  @CommandMethod("tasks task <name> set staticServices|static|staticService <enabled>")
  public void setStaticServices(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("enabled") @Liberal boolean enabled
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.staticServices(enabled));
      source.sendMessage(I18n.trans("command-tasks-set-property-success", "staticServices", task.name(), enabled));
    }
  }

  @CommandMethod("tasks task <name> set environment <environment>")
  public void setEnvironment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
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
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @Argument("enabled") @Liberal boolean enabled
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.disableIpRewrite(enabled));
      source.sendMessage(I18n.trans("command-tasks-set-property-success",
        "disableIpRewrite",
        task.name(),
        enabled));
    }
  }

  @CommandMethod("tasks task <name> set javaCommand <executable>")
  public void setJavaCommand(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument(value = "executable", parserName = "javaCommand") @Quoted Pair<String, JavaVersion> executable
  ) {
    for (var task : serviceTasks) {
      this.updateTask(task, builder -> builder.javaCommand(executable.first()));
      source.sendMessage(
        I18n.trans("command-tasks-set-property-success",
          "javaCommand",
          task.name(),
          executable.first()));
    }
  }

  @CommandMethod("tasks task <name> add node <uniqueId>")
  public void addNode(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument(value = "uniqueId", parserName = "nodeId") String node
  ) {
    for (var task : serviceTasks) {
      if (task.associatedNodes().contains(node)) {
        continue;
      }

      this.updateTask(task, builder -> builder.addAssociatedNode(node));
      source.sendMessage(I18n.trans("command-tasks-add-collection-property",
        "node",
        node,
        task.name()));
    }
  }

  @CommandMethod("tasks task <name> add group <group>")
  public void addGroup(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument("group") GroupConfiguration group
  ) {
    for (var task : serviceTasks) {
      if (task.groups().contains(group.name())) {
        continue;
      }

      this.updateTask(task, builder -> builder.addGroup(group.name()));
      source.sendMessage(I18n.trans("command-tasks-add-collection-property",
        "group",
        group.name(),
        task.name()));
    }
  }

  @CommandMethod("tasks task <name> remove node <uniqueId>")
  public void removeNode(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument("uniqueId") String node
  ) {
    for (var task : serviceTasks) {
      this.updateTaskDirect(task, serviceTask -> serviceTask.associatedNodes().remove(node));
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property",
        "node",
        node,
        task.name()));
    }
  }

  @CommandMethod("tasks task <name> remove group <group>")
  public void removeGroup(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument("group") String group
  ) {
    for (var task : serviceTasks) {
      this.updateTaskDirect(task, serviceTask -> serviceTask.groups().remove(group));
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property",
        "group",
        group,
        task.name()));
    }
  }

  @CommandMethod("tasks task <name> add deployment <deployment>")
  public void addDeployment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument("deployment") ServiceTemplate template,
    @Flag("excludes") @Quoted String excludes,
    @Flag("includes") @Quoted String includes,
    @Flag("case-sensitive") boolean caseSensitive
  ) {
    var deployment = ServiceDeployment.builder()
      .template(template)
      .excludes(ServiceCommand.parseDeploymentPatterns(excludes, caseSensitive))
      .includes(ServiceCommand.parseDeploymentPatterns(includes, caseSensitive))
      .withDefaultExclusions()
      .build();
    for (var serviceTask : serviceTasks) {
      this.updateTask(serviceTask, builder -> builder.addDeployments(Set.of(deployment)));
      source.sendMessage(I18n.trans("command-tasks-add-collection-property",
        "deployment",
        deployment.template(),
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> add template <template>")
  public void addTemplate(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument("template") ServiceTemplate template
  ) {
    for (var serviceTask : serviceTasks) {
      this.updateTask(serviceTask, task -> task.addTemplates(Set.of(template)));
      source.sendMessage(I18n.trans("command-tasks-add-collection-property",
        "template",
        template,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> add inclusion <url> <path>")
  public void addInclusion(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument("url") String url,
    @NonNull @Argument("path") String path
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();

    for (var serviceTask : serviceTasks) {
      this.updateTask(serviceTask, task -> task.addInclusions(Set.of(inclusion)));
      source.sendMessage(I18n.trans("command-tasks-add-collection-property",
        "inclusion",
        inclusion,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> add jvmOption <options>")
  public void addJvmOption(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Greedy @Argument("options") String jvmOptions
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
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Greedy @Argument("options") String processParameters
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
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument("deployment") ServiceTemplate template,
    @Flag("excludes") @Quoted String excludes,
    @Flag("includes") @Quoted String includes,
    @Flag("case-sensitive") boolean caseSensitive
  ) {
    var deployment = ServiceDeployment.builder()
      .template(template)
      .excludes(ServiceCommand.parseDeploymentPatterns(excludes, caseSensitive))
      .includes(ServiceCommand.parseDeploymentPatterns(includes, caseSensitive))
      .withDefaultExclusions()
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
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument("template") ServiceTemplate template
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
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument("url") String url,
    @NonNull @Argument("path") String path
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();

    for (var serviceTask : serviceTasks) {
      this.updateTaskDirect(serviceTask, task -> task.inclusions().remove(inclusion));
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property",
        "inclusion",
        inclusion,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> remove jvmOption <options>")
  public void removeJvmOption(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Greedy @Argument("options") String jvmOptions
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
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks,
    @NonNull @Greedy @Argument("options") String processParameters
  ) {
    Collection<String> splitOptions = Arrays.asList(processParameters.split(" "));
    for (var serviceTask : serviceTasks) {
      serviceTask.processParameters().removeAll(splitOptions);
      this.updateTask(serviceTask);
      source.sendMessage(I18n.trans("command-tasks-remove-collection-property",
        "processParamter",
        processParameters,
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> clear jvmOptions")
  public void clearJvmOptions(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks
  ) {
    for (var serviceTask : serviceTasks) {
      this.updateTaskDirect(serviceTask, task -> task.jvmOptions().clear());
      source.sendMessage(I18n.trans("command-tasks-clear-property",
        "jvmOptions",
        serviceTask.name()));
    }
  }

  @CommandMethod("tasks task <name> clear processParameters")
  public void clearProcessParameter(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> serviceTasks
  ) {
    for (var serviceTask : serviceTasks) {
      this.updateTaskDirect(serviceTask, task -> task.processParameters().clear());
      source.sendMessage(I18n.trans("command-tasks-clear-property",
        "processParameters",
        serviceTask.name()));
    }
  }

  private void updateTask(@NonNull ServiceTask task) {
    this.taskProvider().addServiceTask(task);
  }

  private void updateTask(@NonNull ServiceTask task, @NonNull Consumer<ServiceTask.Builder> consumer) {
    consumer
      .andThen(result -> this.updateTask(result.build()))
      .accept(ServiceTask.builder(task));
  }

  private void updateTaskDirect(@NonNull ServiceTask task, @NonNull Consumer<ServiceTask> consumer) {
    consumer.andThen(this::updateTask).accept(task);
  }

  private @NonNull ServiceTaskProvider taskProvider() {
    return Node.instance().serviceTaskProvider();
  }

}
