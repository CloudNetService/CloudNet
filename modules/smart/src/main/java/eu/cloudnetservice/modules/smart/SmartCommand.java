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

package eu.cloudnetservice.modules.smart;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Range;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import lombok.NonNull;

@CommandPermission("cloudnet.command.smart")
@Description("module-smart-command-description")
public class SmartCommand {

  @Parser(name = "smartTask", suggestions = "smartTask")
  public @NonNull ServiceTask smartTaskParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var task = Node.instance().serviceTaskProvider().serviceTask(input.remove());
    if (task == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-tasks-task-not-found"));
    }
    // only allow tasks with the smart config
    if (!task.properties().contains("smartConfig")) {
      throw new ArgumentNotAvailableException(I18n.trans("module-smart-command-task-no-entry", task.name()));
    }
    return task;
  }

  @Suggestions("smartTask")
  public @NonNull List<String> suggestSmartTasks(@NonNull CommandContext<?> $, @NonNull String input) {
    return Node.instance().serviceTaskProvider().serviceTasks()
      .stream()
      .filter(serviceTask -> serviceTask.properties().contains("smartConfig"))
      .map(Nameable::name)
      .toList();
  }

  @CommandMethod("smart task <task> enabled <enabled>")
  public void enable(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("enabled") boolean enabled
  ) {
    this.updateSmart(task, config -> config.enabled(enabled));
    source.sendMessage(I18n.trans(
      "command-tasks-set-property-success",
      "enabled", task.name(),
      enabled));
  }

  @CommandMethod("smart task <task> priority <priority>")
  public void priority(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("priority") int priority
  ) {
    this.updateSmart(task, config -> config.priority(priority));
    source.sendMessage(I18n.trans(
      "command-tasks-set-property-success",
      "priority",
      task.name(),
      priority));
  }

  @CommandMethod("smart task <task> maxServices <amount>")
  public void maxServices(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("amount") int maxServices
  ) {
    this.updateSmart(task, config -> config.maxServices(maxServices));
    source.sendMessage(I18n.trans(
      "command-tasks-set-property-success",
      "maxServices",
      task.name(),
      maxServices));
  }

  @CommandMethod("smart task <task> preparedServices <amount>")
  public void preparedServices(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("amount") int preparedServices
  ) {
    this.updateSmart(task, config -> config.preparedServices(preparedServices));
    source.sendMessage(
      I18n.trans(
        "command-tasks-set-property-success",
        "preparedServices",
        task.name(),
        preparedServices));
  }

  @CommandMethod("smart task <task> smartMinServiceCount <amount>")
  public void smartMinServiceCount(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("amount") int smartMinServiceCount
  ) {
    this.updateSmart(task, config -> config.smartMinServiceCount(smartMinServiceCount));
    source.sendMessage(
      I18n.trans(
        "command-tasks-set-property-success",
        "smartMinServiceCount",
        task.name(),
        smartMinServiceCount));
  }

  @CommandMethod("smart task <task> splitLogicallyOverNodes <enabled>")
  public void splitLogicallyOverNodes(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("enabled") boolean enabled
  ) {
    this.updateSmart(task, config -> config.splitLogicallyOverNodes(enabled));
    source.sendMessage(
      I18n.trans(
        "command-tasks-set-property-success",
        "splitLogicallyOverNodes",
        task.name(),
        enabled));
  }

  @CommandMethod("smart task <task> directTemplatesAndInclusionsSetup <enabled>")
  public void directTemplatesAndInclusionsSetup(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("enabled") boolean enabled
  ) {
    this.updateSmart(task, config -> config.directTemplatesAndInclusionsSetup(enabled));
    source.sendMessage(I18n.trans(
      "command-tasks-set-property-success",
      "directTemplatesAndInclusionsSetup",
      task.name(),
      enabled));
  }

  @CommandMethod("smart task <task> templateInstaller <installer>")
  public void templateInstaller(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @NonNull @Argument("installer") SmartServiceTaskConfig.TemplateInstaller installer
  ) {
    this.updateSmart(task, config -> config.templateInstaller(installer));
    source.sendMessage(I18n.trans(
      "command-tasks-set-property-success",
      "templateInstaller",
      task.name(),
      installer));
  }

  @CommandMethod("smart task <task> autoStopTimeByUnusedServiceInSeconds <seconds>")
  public void autoStopTimeByUnusedServiceInSeconds(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("seconds") int seconds
  ) {
    this.updateSmart(task, config -> config.autoStopTimeByUnusedServiceInSeconds(seconds));
    source.sendMessage(I18n.trans(
      "command-tasks-set-property-success",
      "autoStopTimeByUnusedServiceInSeconds",
      task.name(),
      seconds));
  }

  @CommandMethod("smart task <task> percentOfPlayersToCheckShouldStopTheService <percent>")
  public void percentOfPlayersToCheckShouldStopTheService(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("percent") @Range(min = "0", max = "100") int percent
  ) {
    this.updateSmart(task, config -> config.percentOfPlayersToCheckShouldStop(percent));
    source.sendMessage(I18n.trans(
      "command-tasks-set-property-success",
      "percentOfPlayersToCheckShouldStop",
      task.name(),
      percent));
  }

  @CommandMethod("smart task <task> forAnewInstanceDelayTimeInSeconds <seconds>")
  public void forAnewInstanceDelayTimeInSeconds(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("seconds") int seconds
  ) {
    this.updateSmart(task, config -> config.forAnewInstanceDelayTimeInSeconds(seconds));
    source.sendMessage(I18n.trans(
      "command-tasks-set-property-success",
      "forAnewInstanceDelayTimeInSeconds",
      task.name(),
      seconds));
  }

  @CommandMethod("smart task <task> percentOfPlayersForANewServiceByInstance <percent>")
  public void percentOfPlayersForANewServiceByInstance(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "smartTask") ServiceTask task,
    @Argument("percent") @Range(min = "0", max = "100") int percent
  ) {
    this.updateSmart(task, config -> config.percentOfPlayersForANewServiceByInstance(percent));
    source.sendMessage(I18n.trans(
      "command-tasks-set-property-success",
      "percentOfPlayersForANewServiceByInstance",
      task.name(),
      percent));
  }

  private void updateSmart(
    @NonNull ServiceTask serviceTask,
    @NonNull Function<SmartServiceTaskConfig.Builder, SmartServiceTaskConfig.Builder> modifier
  ) {
    // read the smart config from the task
    var property = serviceTask.properties().get("smartConfig", SmartServiceTaskConfig.class);
    // rewrite the config and update it in the cluster
    var task = ServiceTask
      .builder(serviceTask)
      .properties(serviceTask.properties()
        .append("smartConfig", modifier.apply(SmartServiceTaskConfig.builder(property)).build()))
      .build();
    Node.instance().serviceTaskProvider().addServiceTask(task);
  }
}
