/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.smart.impl;

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.modules.smart.SmartServiceManagement;
import eu.cloudnetservice.modules.smart.SmartServiceTaskConfig;
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

  private final SmartServiceManagement smartManagement;
  private final ServiceTaskProvider serviceTaskProvider;

  @Inject
  public SmartCommand(
    @NonNull SmartServiceManagement smartManagement,
    @NonNull ServiceTaskProvider serviceTaskProvider
  ) {
    this.smartManagement = smartManagement;
    this.serviceTaskProvider = serviceTaskProvider;
  }

  @Parser(suggestions = "smartConfig")
  public @NonNull SmartServiceTaskConfig smartConfigParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var targetTask = input.readString();
    var config = this.smartManagement.smartServiceTaskConfig(targetTask);
    if (config == null) {
      throw new ArgumentNotAvailableException(i18n.translate("module-smart-command-task-no-entry", targetTask));
    }

    return config;
  }

  @Suggestions("smartConfig")
  public @NonNull Stream<String> suggestSmartTasks() {
    return this.smartManagement.configurations()
      .values()
      .stream()
      .map(SmartServiceTaskConfig::targetTask);
  }

  @Parser(name = "newSmartConfigs", suggestions = "newSmartConfigs")
  public @NonNull String newSmartConfigParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var taskName = input.readString();
    var task = this.serviceTaskProvider.serviceTask(taskName);
    if (task == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-tasks-task-not-found", taskName));
    }

    var config = this.smartManagement.smartServiceTaskConfig(taskName);
    if (config != null) {
      throw new ArgumentNotAvailableException(i18n.translate("module-smart-command-task-already-exists", taskName));
    }

    return taskName;
  }

  @Suggestions("newSmartConfigs")
  public @NonNull Stream<String> suggestNewSmartConfigs() {
    return this.serviceTaskProvider.serviceTasks().stream()
      .map(Named::name)
      .filter(taskName -> this.smartManagement.smartServiceTaskConfig(taskName) == null);
  }

  @Command("smart create <task>")
  public void createEntry(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument(value = "task", parserName = "newSmartConfigs") String taskName
  ) {
    this.updateSmart(SmartServiceTaskConfig.builder().targetTask(taskName).build(), Function.identity());
    source.sendMessage(i18n.translate("module-smart-command-task-created", taskName));
  }

  @Command("smart delete <task>")
  public void deleteEntry(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config
  ) {
    this.smartManagement.removeSmartServiceTaskConfig(config.targetTask());
    source.sendMessage(i18n.translate("module-smart-command-task-deleted", config.targetTask()));
  }

  @Command("smart task <task> enabled <enabled>")
  public void enable(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("enabled") boolean enabled
  ) {
    this.updateSmart(config, builder -> builder.enabled(enabled));
    source.sendMessage(i18n.translate(
      "command-tasks-set-property-success",
      "enabled", config.targetTask(),
      enabled));
  }

  @Command("smart task <task> priority <priority>")
  public void priority(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("priority") int priority
  ) {
    this.updateSmart(config, builder -> builder.priority(priority));
    source.sendMessage(i18n.translate(
      "command-tasks-set-property-success",
      "priority",
      config.targetTask(),
      priority));
  }

  @Command("smart task <task> maxServices <amount>")
  public void maxServices(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("amount") int maxServices
  ) {
    this.updateSmart(config, builder -> builder.maxServices(maxServices));
    source.sendMessage(i18n.translate(
      "command-tasks-set-property-success",
      "maxServices",
      config.targetTask(),
      maxServices));
  }

  @Command("smart task <task> preparedServices <amount>")
  public void preparedServices(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("amount") int preparedServices
  ) {
    this.updateSmart(config, builder -> builder.preparedServices(preparedServices));
    source.sendMessage(
      i18n.translate(
        "command-tasks-set-property-success",
        "preparedServices",
        config.targetTask(),
        preparedServices));
  }

  @Command("smart task <task> smartMinServiceCount <amount>")
  public void smartMinServiceCount(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("amount") int smartMinServiceCount
  ) {
    this.updateSmart(config, builder -> builder.smartMinServiceCount(smartMinServiceCount));
    source.sendMessage(
      i18n.translate(
        "command-tasks-set-property-success",
        "smartMinServiceCount",
        config.targetTask(),
        smartMinServiceCount));
  }

  @Command("smart task <task> splitLogicallyOverNodes <enabled>")
  public void splitLogicallyOverNodes(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("enabled") boolean enabled
  ) {
    this.updateSmart(config, builder -> builder.splitLogicallyOverNodes(enabled));
    source.sendMessage(
      i18n.translate(
        "command-tasks-set-property-success",
        "splitLogicallyOverNodes",
        config.targetTask(),
        enabled));
  }

  @Command("smart task <task> directTemplatesAndInclusionsSetup <enabled>")
  public void directTemplatesAndInclusionsSetup(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("enabled") boolean enabled
  ) {
    this.updateSmart(config, builder -> builder.directTemplatesAndInclusionsSetup(enabled));
    source.sendMessage(i18n.translate(
      "command-tasks-set-property-success",
      "directTemplatesAndInclusionsSetup",
      config.targetTask(),
      enabled));
  }

  @Command("smart task <task> templateInstaller <installer>")
  public void templateInstaller(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @NonNull @Argument("installer") SmartServiceTaskConfig.TemplateInstaller installer
  ) {
    this.updateSmart(config, builder -> builder.templateInstaller(installer));
    source.sendMessage(i18n.translate(
      "command-tasks-set-property-success",
      "templateInstaller",
      config.targetTask(),
      installer));
  }

  @Command("smart task <task> autoStopTimeByUnusedServiceInSeconds <seconds>")
  public void autoStopTimeByUnusedServiceInSeconds(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("seconds") int seconds
  ) {
    this.updateSmart(config, builder -> builder.autoStopTimeByUnusedServiceInSeconds(seconds));
    source.sendMessage(i18n.translate(
      "command-tasks-set-property-success",
      "autoStopTimeByUnusedServiceInSeconds",
      config.targetTask(),
      seconds));
  }

  @Command("smart task <task> percentOfPlayersToCheckShouldStopTheService <percent>")
  public void percentOfPlayersToCheckShouldStopTheService(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("percent") @Range(min = "0", max = "100") int percent
  ) {
    this.updateSmart(config, builder -> builder.percentOfPlayersToCheckShouldStop(percent));
    source.sendMessage(i18n.translate(
      "command-tasks-set-property-success",
      "percentOfPlayersToCheckShouldStop",
      config.targetTask(),
      percent));
  }

  @Command("smart task <task> forAnewInstanceDelayTimeInSeconds <seconds>")
  public void forAnewInstanceDelayTimeInSeconds(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("seconds") int seconds
  ) {
    this.updateSmart(config, builder -> builder.forAnewInstanceDelayTimeInSeconds(seconds));
    source.sendMessage(i18n.translate(
      "command-tasks-set-property-success",
      "forAnewInstanceDelayTimeInSeconds",
      config.targetTask(),
      seconds));
  }

  @Command("smart task <task> percentOfPlayersForANewServiceByInstance <percent>")
  public void percentOfPlayersForANewServiceByInstance(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") SmartServiceTaskConfig config,
    @Argument("percent") @Range(min = "0", max = "100") int percent
  ) {
    this.updateSmart(config, builder -> builder.percentOfPlayersForANewServiceByInstance(percent));
    source.sendMessage(i18n.translate(
      "command-tasks-set-property-success",
      "percentOfPlayersForANewServiceByInstance",
      config.targetTask(),
      percent));
  }

  private void updateSmart(
    @NonNull SmartServiceTaskConfig config,
    @NonNull Function<SmartServiceTaskConfig.Builder, SmartServiceTaskConfig.Builder> modifier
  ) {
    var resultingConfig = modifier.apply(SmartServiceTaskConfig.builder(config)).build();
    this.smartManagement.addSmartServiceTaskConfig(resultingConfig);
  }
}
