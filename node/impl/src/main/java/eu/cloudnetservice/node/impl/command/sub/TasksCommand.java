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

package eu.cloudnetservice.node.impl.command.sub;

import eu.cloudnetservice.driver.base.JavaVersion;
import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceConfigurationBase;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.command.source.ConsoleCommandSource;
import eu.cloudnetservice.node.impl.console.Console;
import eu.cloudnetservice.node.impl.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.node.impl.log.QueuedConsoleLogAppender;
import eu.cloudnetservice.node.impl.setup.SpecificTaskSetup;
import eu.cloudnetservice.node.impl.util.JavaVersionResolver;
import eu.cloudnetservice.node.impl.util.NetworkUtil;
import eu.cloudnetservice.node.impl.util.WildcardUtil;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.utils.base.column.ColumnFormatter;
import eu.cloudnetservice.utils.base.column.RowedFormatter;
import io.vavr.Tuple2;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotation.specifier.Liberal;
import org.incendo.cloud.annotation.specifier.Quoted;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.Regex;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;
import org.jetbrains.annotations.Nullable;

@Singleton
@Permission("cloudnet.command.tasks")
@Description("command-tasks-description")
public final class TasksCommand {

  private static final String TASK_SETUP_HEADER = """
    &f _____              _       &b           _                \s
    &f/__   \\  __ _  ___ | | __  &b ___   ___ | |_  _   _  _ __ \s
    &f  / /\\/ / _` |/ __|| |/ /  &b/ __| / _ \\| __|| | | || '_ \\\s
    &f / /   | (_| |\\__ \\|   <  &b \\__ \\|  __/| |_ | |_| || |_) |
    &f \\/     \\__,_||___/|_|\\_\\&b  |___/ \\___| \\__| \\__,_|| .__/\s
    &f                             &b                     |_|   \s""";
  // Formatter for the table based looking
  private static final RowedFormatter<ServiceTask> TASK_LIST_FORMATTER = RowedFormatter.<ServiceTask>builder()
    .defaultFormatter(ColumnFormatter.builder()
      .columnTitles("Name", "MinServiceCount", "Maintenance", "Nodes", "StartPort")
      .build())
    .column(ServiceTask::name)
    .column(ServiceTask::minServiceCount)
    .column(ServiceTask::maintenance)
    .column(task -> task.associatedNodes().isEmpty() ? "All" : String.join(", ", task.associatedNodes()))
    .column(ServiceTask::startPort)
    .build();

  private final I18n i18n;
  private final Configuration configuration;
  private final ConsoleSetupAnimation taskSetupAnimation;
  private final ServiceTaskProvider taskProvider;
  private final CloudServiceManager serviceManager;
  private final ClusterNodeProvider clusterNodeProvider;

  @Inject
  public TasksCommand(
    @NonNull @Service I18n i18n,
    @NonNull EventManager eventManager,
    @NonNull Configuration configuration,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull QueuedConsoleLogAppender logHandler,
    @NonNull CloudServiceManager serviceManager,
    @NonNull ClusterNodeProvider clusterNodeProvider
  ) {
    this.i18n = i18n;
    this.configuration = configuration;
    this.taskProvider = taskProvider;
    this.serviceManager = serviceManager;
    this.clusterNodeProvider = clusterNodeProvider;
    this.taskSetupAnimation = new ConsoleSetupAnimation(
      this.i18n,
      eventManager,
      logHandler,
      TASK_SETUP_HEADER,
      "Task creation complete!",
      "&r> &e");
  }

  public static void applyServiceConfigurationDisplay(
    @NonNull Collection<String> messages,
    @NonNull ServiceConfigurationBase configurationBase
  ) {
    messages.add(" ");

    messages.add("Includes:");

    for (var inclusion : configurationBase.inclusions()) {
      messages.add("- " + inclusion.url() + ':' + inclusion.cacheStrategy() + " => " + inclusion.destination());
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

    messages.addAll(Arrays.asList(configurationBase.propertyHolder().serializeToString().split("\n")));
    messages.add(" ");
  }

  @Parser(suggestions = "serviceTask")
  public @NonNull ServiceTask defaultTaskParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var name = input.readString();
    var task = this.taskProvider.serviceTask(name);
    if (task == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-tasks-task-not-found"));
    }

    return task;
  }

  @Suggestions("serviceTask")
  public @NonNull Stream<String> suggestTask() {
    return this.taskProvider.serviceTasks().stream().map(Named::name);
  }

  @Parser(suggestions = "ipAliasHostAddress", name = "ipAliasHostAddress")
  public @NonNull String hostAddressParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var address = input.readString();
    var alias = this.configuration.ipAliases().get(address);
    // check if we can resolve the host address using our ip alias
    if (alias != null) {
      return address;
    }
    // check if the host address is parsable and assignable
    var hostAndPort = NetworkUtil.parseAssignableHostAndPort(address, false);
    if (hostAndPort == null || NetworkUtil.checkWildcard(hostAndPort)) {
      // could not parse
      throw new ArgumentNotAvailableException(i18n.translate("command-tasks-unknown-host-address-or-alias", address));
    }

    return hostAndPort.host();
  }

  @Suggestions("ipAliasHostAddress")
  public @NonNull List<String> suggestHostAddress() {
    // all network addresses
    var hostAddresses = new ArrayList<>(NetworkUtil.availableIPAddresses());
    // all ip aliases
    hostAddresses.addAll(this.configuration.ipAliases().keySet());
    return hostAddresses;
  }

  @Parser(suggestions = "serviceTask")
  public @NonNull Collection<ServiceTask> wildcardTaskParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var name = input.readString();
    var matchedTasks = WildcardUtil.filterWildcard(this.taskProvider.serviceTasks(), name);
    if (matchedTasks.isEmpty()) {
      throw new ArgumentNotAvailableException(i18n.translate("command-tasks-task-not-found"));
    }

    return matchedTasks;
  }

  @Parser(name = "javaCommand")
  public @NonNull Tuple2<String, JavaVersion> javaCommandParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var command = input.remainingInput();
    // we have to clear the queue as we consumed the input using String.join
    input.cursor(input.length());

    var version = JavaVersionResolver.resolveFromJavaExecutable(command);
    if (version == null) {
      throw new ArgumentNotAvailableException(
        i18n.translate("command-tasks-setup-question-javacommand-invalid"));
    }

    return new Tuple2<>(command, version);
  }

  @Parser(name = "nodeId", suggestions = "clusterNode")
  public @NonNull String defaultClusterNodeParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var nodeId = input.readString();
    for (var node : this.clusterNodeProvider.nodes()) {
      if (node.uniqueId().equals(nodeId)) {
        return nodeId;
      }
    }
    throw new ArgumentNotAvailableException(i18n.translate("command-tasks-node-not-found"));
  }

  @Suggestions("clusterNode")
  public @NonNull Stream<String> suggestNode() {
    return this.clusterNodeProvider.nodes()
      .stream()
      .map(NetworkClusterNode::uniqueId);
  }

  @Parser(name = "taskRuntime", suggestions = "taskRuntime")
  public @NonNull String taskRuntimeParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var runtime = input.readString();
    if (this.serviceManager.cloudServiceFactory(runtime) == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-tasks-runtime-not-found", runtime));
    }

    return runtime;
  }

  @Suggestions("taskRuntime")
  public @NonNull List<String> taskRuntimeSuggester() {
    return List.copyOf(this.serviceManager.cloudServiceFactories().keySet());
  }

  @Command(value = "tasks setup", requiredSender = ConsoleCommandSource.class)
  public void taskSetup(@NonNull SpecificTaskSetup taskSetup, @NonNull Console console) {
    // apply the questions and handle the results
    taskSetup.applyQuestions(this.taskSetupAnimation);
    this.taskSetupAnimation.addFinishHandler(() -> taskSetup.handleResults(this.taskSetupAnimation));
    // start the animation
    console.startAnimation(this.taskSetupAnimation);
  }

  @Command("tasks reload")
  public void reloadTasks(@NonNull @Service I18n i18n, @NonNull CommandSource source) {
    this.taskProvider.reload();
    source.sendMessage(i18n.translate("command-tasks-reload-success"));
  }

  @Command("tasks delete <name>")
  public void deleteTask(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks
  ) {
    for (var serviceTask : tasks) {
      this.taskProvider.removeServiceTask(serviceTask);
      source.sendMessage(i18n.translate("command-tasks-delete-task", serviceTask.name()));
    }
  }

  @Command("tasks list")
  public void listTasks(@NonNull CommandSource source) {
    source.sendMessage(TASK_LIST_FORMATTER.format(this.taskProvider.serviceTasks()));
  }

  @Command("tasks create <name> <environment>")
  public void createTask(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Regex(ServiceTask.NAMING_REGEX) @Argument("name") String taskName,
    @NonNull @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    if (this.taskProvider.serviceTask(taskName) != null) {
      source.sendMessage(i18n.translate("command-tasks-task-already-existing", taskName));
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
    this.taskProvider.addServiceTask(serviceTask);
    source.sendMessage(i18n.translate("command-tasks-create-task"));
  }

  @Command("tasks task <name>")
  public void displayTask(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks
  ) {
    for (var serviceTask : tasks) {
      Collection<String> messages = new ArrayList<>();
      messages.add("Name: " + serviceTask.name());
      messages.add("Runtime: " + serviceTask.runtime());
      messages.add("Host address: " + serviceTask.hostAddress());
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

  @Command("tasks rename <oldName> <newName>")
  public void renameTask(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument(value = "oldName") ServiceTask serviceTask,
    @NonNull @Regex(ServiceTask.NAMING_REGEX) @Argument("newName") String newName
  ) {
    if (this.taskProvider.serviceTask(newName) != null) {
      source.sendMessage(i18n.translate("command-tasks-task-already-existing", newName));
    } else {
      // create a copy with the new name and remove the old task
      this.taskProvider.removeServiceTask(serviceTask);
      this.taskProvider.addServiceTask(ServiceTask.builder(serviceTask).name(newName).build());
      source.sendMessage(i18n.translate("command-tasks-task-rename-success", serviceTask.name(), newName));
    }
  }

  @Command("tasks task <name> set autoDeleteOnStop <enabled>")
  public void setAutoDeleteOnStop(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @Argument("enabled") @Liberal boolean enabled
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::autoDeleteOnStop,
      "command-tasks-set-property-success",
      "autoDeleteOnStop",
      enabled);
  }

  @Command("tasks task <name> set runtime <runtime>")
  public void setRuntime(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument("runtime") String runtime
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::runtime,
      "command-tasks-set-property-success",
      "runtime",
      runtime);
  }

  @Command("tasks task <name> set nameSplitter <splitter>")
  public void setNameSplitter(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Regex(ServiceTask.NAMING_REGEX) @Argument("splitter") String splitter
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::nameSplitter,
      "command-tasks-set-property-success",
      "nameSplitter",
      splitter);
  }

  @Command("tasks task <name> set minServiceCount <amount>")
  public void setMinServiceCount(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @Argument("amount") @Range(min = "0") int amount
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::minServiceCount,
      "command-tasks-set-property-success",
      "minServiceCount",
      amount);
  }

  @Command("tasks task <name> set hostAddress <hostAddress>")
  public void setHostAddress(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument(value = "hostAddress", parserName = "ipAliasHostAddress") String hostAddress
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::hostAddress,
      "command-tasks-set-property-success",
      "hostAddress",
      hostAddress);
  }

  @Command("tasks task <name> set maintenance <enabled>")
  public void setMaintenance(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @Argument("enabled") @Liberal boolean enabled
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::maintenance,
      "command-tasks-set-property-success",
      "maintenance",
      enabled);
  }

  @Command("tasks task <name> set maxHeapMemory <amount>")
  public void setMaxHeapMemory(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @Argument("amount") @Range(min = "50") int amount
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::maxHeapMemory,
      "command-tasks-set-property-success",
      "maxHeapMemory",
      amount);
  }

  @Command("tasks task <name> set startPort <port>")
  public void setStartPort(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @Argument("port") @Range(min = "0") int port
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::startPort,
      "command-tasks-set-property-success",
      "startPort",
      port);
  }

  @Command("tasks task <name> set staticServices|static|staticService <enabled>")
  public void setStaticServices(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @Argument("enabled") @Liberal boolean enabled
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::staticServices,
      "command-tasks-set-property-success",
      "staticServices",
      enabled);
  }

  @Command("tasks task <name> set environment <environment>")
  public void setEnvironment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.serviceEnvironmentType(environmentType),
      "command-tasks-set-property-success",
      "environment",
      environmentType.name());
  }

  @Command("tasks task <name> set disableIpRewrite <enabled>")
  public void setDisableIpRewrite(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @Argument("enabled") @Liberal boolean enabled
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::disableIpRewrite,
      "command-tasks-set-property-success",
      "disableIpRewrite",
      enabled);
  }

  @Command("tasks task <name> set javaCommand <executable>")
  public void setJavaCommand(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument(value = "executable", parserName = "javaCommand") Tuple2<String, JavaVersion> executable
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::javaCommand,
      "command-tasks-set-property-success",
      "node",
      executable._1());
  }

  @Command("tasks task <name> add node <uniqueId>")
  public void addNode(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument(value = "uniqueId", parserName = "nodeId") String node
  ) {
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyAssociatedNodes(col -> col.add(node)),
      "command-tasks-add-collection-property",
      "node",
      node);
  }

  @Command("tasks task <name> add group <group>")
  public void addGroup(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument("group") GroupConfiguration group
  ) {
    this.applyChange(
      source,
      tasks,
      (builder, groupName) -> builder.modifyGroups(col -> col.add(groupName)),
      "command-tasks-add-collection-property",
      "group",
      group.name());
  }

  @Command("tasks task <name> add deployment <deployment>")
  public void addDeployment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument("deployment") ServiceTemplate template,
    @Nullable @Flag("excludes") @Quoted String excludes,
    @Nullable @Flag("includes") @Quoted String includes,
    @Flag("case-sensitive") boolean caseSensitive
  ) {
    var deployment = ServiceDeployment.builder()
      .template(template)
      .excludes(ServiceCommand.parseDeploymentPatterns(excludes, caseSensitive))
      .includes(ServiceCommand.parseDeploymentPatterns(includes, caseSensitive))
      .withDefaultExclusions()
      .build();
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyDeployments(col -> col.add(deployment)),
      "command-tasks-add-collection-property",
      "deployment",
      deployment);
  }

  @Command("tasks task <name> add template <template>")
  public void addTemplate(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument("template") ServiceTemplate template
  ) {
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyTemplates(col -> col.add(template)),
      "command-tasks-add-collection-property",
      "template",
      template);
  }

  @Command("tasks task <name> add inclusion <url> <path> [cacheStrategy]")
  public void addInclusion(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument("url") String url,
    @NonNull @Argument("path") String path,
    @NonNull @Default(ServiceRemoteInclusion.NO_CACHE_STRATEGY) @Argument(
      value = "cacheStrategy",
      parserName = "inclusionCacheStrategy") String cacheStrategy
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).cacheStrategy(cacheStrategy).build();
    this.applyChange(
      source,
      tasks,
      (builder, _) -> builder.modifyInclusions(col -> col.add(inclusion)),
      "command-tasks-add-collection-property",
      "inclusion",
      inclusion);
  }

  @Command("tasks task <name> add jvmOption <options>")
  public void addJvmOption(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Greedy @Argument("options") String jvmOptions
  ) {
    var splittedOptions = Arrays.asList(jvmOptions.split(" "));
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyJvmOptions(col -> col.addAll(splittedOptions)),
      "command-tasks-add-collection-property",
      "jvmOption",
      splittedOptions);
  }

  @Command("tasks task <name> add processParameter <options>")
  public void addProcessParameter(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Greedy @Argument("options") String processParameters
  ) {
    var splittedOptions = Arrays.asList(processParameters.split(" "));
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyProcessParameters(col -> col.addAll(splittedOptions)),
      "command-tasks-add-collection-property",
      "processParameter",
      splittedOptions);
  }

  @Command("tasks task <name> remove deployment <deployment>")
  public void removeDeployment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument("deployment") ServiceTemplate template,
    @Nullable @Flag("excludes") @Quoted String excludes,
    @Nullable @Flag("includes") @Quoted String includes,
    @Flag("case-sensitive") boolean caseSensitive
  ) {
    var deployment = ServiceDeployment.builder()
      .template(template)
      .excludes(ServiceCommand.parseDeploymentPatterns(excludes, caseSensitive))
      .includes(ServiceCommand.parseDeploymentPatterns(includes, caseSensitive))
      .withDefaultExclusions()
      .build();
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyDeployments(col -> col.remove(deployment)),
      "command-tasks-remove-collection-property",
      "deployment",
      template);
  }

  @Command("tasks task <name> remove template <template>")
  public void removeTemplate(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument("template") ServiceTemplate template
  ) {
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyTemplates(col -> col.remove(template)),
      "command-tasks-remove-collection-property",
      "template",
      template);
  }

  @Command("tasks task <name> remove inclusion <url> <path>")
  public void removeInclusion(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument("url") String url,
    @NonNull @Argument("path") String path
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyInclusions(col -> col.remove(inclusion)),
      "command-tasks-remove-collection-property",
      "inclusion",
      inclusion);
  }

  @Command("tasks task <name> remove jvmOption <options>")
  public void removeJvmOption(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Greedy @Argument("options") String jvmOptions
  ) {
    var splittedOptions = Arrays.asList(jvmOptions.split(" "));
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyJvmOptions(col -> col.removeAll(splittedOptions)),
      "command-tasks-remove-collection-property",
      "jvmOption",
      jvmOptions);
  }

  @Command("tasks task <name> remove processParameter <options>")
  public void removeProcessParameter(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Greedy @Argument("options") String processParameters
  ) {
    var splittedOptions = Arrays.asList(processParameters.split(" "));
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyAssociatedNodes(col -> col.removeAll(splittedOptions)),
      "command-tasks-remove-collection-property",
      "processParameters",
      processParameters);
  }

  @Command("tasks task <name> remove group <group>")
  public void removeGroup(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument("group") String group
  ) {
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyGroups(col -> col.remove(group)),
      "command-tasks-remove-collection-property",
      "group",
      group);
  }

  @Command("tasks task <name> remove node <uniqueId>")
  public void removeNode(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks,
    @NonNull @Argument("uniqueId") String node
  ) {
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyAssociatedNodes(col -> col.remove(node)),
      "command-tasks-remove-collection-property",
      "node",
      node);
  }

  @Command("tasks task <name> clear jvmOptions")
  public void clearJvmOptions(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks
  ) {
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyJvmOptions(Collection::clear),
      "command-tasks-clear-property",
      "jvmOptions",
      null);
  }

  @Command("tasks task <name> clear processParameters")
  public void clearProcessParameter(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks
  ) {
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyProcessParameters(Collection::clear),
      "command-tasks-clear-property",
      "processParameters",
      null);
  }

  @Command("tasks task <name> clear inclusions")
  public void clearInclusions(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks
  ) {
    this.applyChange(
      source,
      tasks,
      (builder, $) -> builder.modifyInclusions(Collection::clear),
      "command-tasks-clear-property",
      "inclusions",
      null);
  }

  @Command("tasks task <name> unset javaCommand")
  public void unsetJavaCommand(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::javaCommand,
      "command-tasks-set-property-success",
      "javaCommand",
      null);
  }

  @Command("tasks task <name> unset hostAddress")
  public void unsetHostAddress(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceTask> tasks
  ) {
    this.applyChange(
      source,
      tasks,
      ServiceTask.Builder::hostAddress,
      "command-tasks-set-property-success",
      "hostAddress",
      null);
  }

  private <T> void applyChange(
    @NonNull CommandSource source,
    @NonNull Collection<ServiceTask> tasks,
    @NonNull BiConsumer<ServiceTask.Builder, T> consumer,
    @NonNull String translation,
    @NonNull String property,
    @Nullable T newValue
  ) {
    for (var task : tasks) {
      var builder = ServiceTask.builder(task);
      consumer.andThen((result, $) -> this.taskProvider.addServiceTask(result.build())).accept(builder, newValue);
      source.sendMessage(this.i18n.translate(translation, property, task.name(), newValue));
    }
  }
}
