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

package eu.cloudnetservice.modules.smart;

import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;

@Singleton
@Permission("cloudnet.command.smart")
@Description("module-smart-command-description")
public class SmartCommand {

  private final ServiceTaskProvider taskProvider;

  @Inject
  public SmartCommand(@NonNull ServiceTaskProvider taskProvider) {
    this.taskProvider = taskProvider;
  }

  @Parser(name = "smartTask", suggestions = "smartTask")
  public @NonNull ServiceTask smartTaskParser(@NonNull CommandInput input) {
    var task = this.taskProvider.serviceTask(input.readString());
    if (task == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-tasks-task-not-found"));
    }
    // only allow tasks with the smart config
    if (!task.propertyHolder().contains("smartConfig")) {
      throw new ArgumentNotAvailableException(I18n.trans("module-smart-command-task-no-entry", task.name()));
    }
    return task;
  }

  @Suggestions("smartTask")
  public @NonNull Stream<String> suggestSmartTasks() {
    return this.taskProvider.serviceTasks()
      .stream()
      .filter(serviceTask -> serviceTask.propertyHolder().contains("smartConfig"))
      .map(Named::name);
  }

  @Command("smart task <task> enabled <enabled>")
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

  @Command("smart task <task> priority <priority>")
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

  @Command("smart task <task> maxServices <amount>")
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

  @Command("smart task <task> preparedServices <amount>")
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

  @Command("smart task <task> smartMinServiceCount <amount>")
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

  @Command("smart task <task> splitLogicallyOverNodes <enabled>")
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

  @Command("smart task <task> directTemplatesAndInclusionsSetup <enabled>")
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

  @Command("smart task <task> templateInstaller <installer>")
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

  @Command("smart task <task> autoStopTimeByUnusedServiceInSeconds <seconds>")
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

  @Command("smart task <task> percentOfPlayersToCheckShouldStopTheService <percent>")
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

  @Command("smart task <task> forAnewInstanceDelayTimeInSeconds <seconds>")
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

  @Command("smart task <task> percentOfPlayersForANewServiceByInstance <percent>")
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
    var property = serviceTask.propertyHolder().readObject("smartConfig", SmartServiceTaskConfig.class);

    // rewrite the config and update it in the cluster
    var task = ServiceTask.builder(serviceTask)
      .modifyProperties(properties -> {
        var newSmartConfigEntry = modifier.apply(SmartServiceTaskConfig.builder(property)).build();
        properties.append("smartConfig", newSmartConfigEntry);
      })
      .build();
    this.taskProvider.addServiceTask(task);
  }
}
