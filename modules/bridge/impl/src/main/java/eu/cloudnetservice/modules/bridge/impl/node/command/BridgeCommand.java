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

package eu.cloudnetservice.modules.bridge.impl.node.command;

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.config.ProxyFallbackConfiguration;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;

@Singleton
@Permission("cloudnet.command.bridge")
@Description("module-bridge-command-description")
public class BridgeCommand {

  private final ServiceTaskProvider taskProvider;
  private final BridgeManagement bridgeManagement;
  private final GroupConfigurationProvider groupConfigurationProvider;

  @Inject
  public BridgeCommand(
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull BridgeManagement bridgeManagement,
    @NonNull GroupConfigurationProvider groupConfigurationProvider
  ) {
    this.taskProvider = taskProvider;
    this.bridgeManagement = bridgeManagement;
    this.groupConfigurationProvider = groupConfigurationProvider;
  }

  @Parser(name = "bridgeGroups", suggestions = "bridgeGroups")
  public GroupConfiguration bridgeGroupParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var name = input.readString();
    var group = this.groupConfigurationProvider.groupConfiguration(name);
    if (group == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-general-group-does-not-exist"));
    }
    var fallbacks = this.bridgeManagement.configuration().fallbackConfigurations()
      .stream();
    // don't allow duplicated entries
    if (fallbacks.anyMatch(fallback -> fallback.targetGroup().equals(group.name()))) {
      throw new ArgumentNotAvailableException(i18n.translate("module-bridge-command-entry-already-exists"));
    }
    return group;
  }

  @Suggestions("bridgeGroups")
  public Stream<String> suggestBridgeGroups() {
    return this.groupConfigurationProvider.groupConfigurations().stream()
      .map(Named::name)
      .filter(group -> this.bridgeManagement.configuration().fallbackConfigurations().stream()
        .noneMatch(fallback -> fallback.targetGroup().equals(group)));
  }

  @Command("bridge create entry <targetGroup>")
  public void createBridgeEntry(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument(value = "targetGroup", parserName = "bridgeGroups") GroupConfiguration group
  ) {
    // create a new configuration for the given target group
    var fallbackConfiguration = new ProxyFallbackConfiguration(
      group.name(),
      "Lobby",
      Collections.emptyList());
    var configuration = this.bridgeManagement.configuration();
    // add the new fallback entry to the configuration
    configuration.fallbackConfigurations().add(fallbackConfiguration);
    // save and update the configuration
    this.bridgeManagement.configuration(configuration);
    source.sendMessage(i18n.translate("module-bridge-command-create-entry-success"));
  }

  @Command("bridge task <task> set requiredPermission <permission>")
  public void setRequiredPermission(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("task") Collection<ServiceTask> serviceTasks,
    @NonNull @Argument("permission") String permission
  ) {
    for (var task : serviceTasks) {
      this.taskProvider.addServiceTask(ServiceTask.builder(task)
        .modifyProperties(properties -> properties.append("requiredPermission", permission))
        .build());
      source.sendMessage(i18n.translate("command-tasks-set-property-success",
        "requiredPermission",
        task.name(),
        permission));
    }
  }
}
