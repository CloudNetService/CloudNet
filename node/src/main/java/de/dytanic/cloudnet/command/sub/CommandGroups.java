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
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

@CommandPermission("cloudnet.command.groups")
@Description("Administers the configurations of all persistent groups")
public class CommandGroups {

  @Parser(suggestions = "groupConfiguration")
  public GroupConfiguration defaultGroupParser(CommandContext<CommandSource> $, Queue<String> input) {
    String name = input.remove();

    GroupConfiguration configuration = this.groupProvider().getGroupConfiguration(name);
    if (configuration == null) {
      throw new ArgumentNotAvailableException(LanguageManager.getMessage("command-service-base-group-not-found"));
    }

    return configuration;
  }

  @Suggestions("groupConfiguration")
  public List<String> suggestGroups(CommandContext<CommandSource> $, String input) {
    return this.groupProvider().getGroupConfigurations().stream().map(INameable::getName).collect(Collectors.toList());
  }

  @CommandMethod("groups delete <name>")
  public void deleteGroup(CommandSource source, @Argument("name") GroupConfiguration configuration) {
    CloudNet.getInstance().getGroupConfigurationProvider().removeGroupConfiguration(configuration);
    source.sendMessage(LanguageManager.getMessage("command-groups-delete-group"));
  }

  @CommandMethod("groups create <name>")
  public void createGroup(CommandSource source, @Argument("name") String groupName) {
    if (!this.groupProvider().isGroupConfigurationPresent(groupName)) {
      this.groupProvider().addGroupConfiguration(GroupConfiguration.empty(groupName));
    }
  }

  @CommandMethod("groups reload")
  public void reloadGroups(CommandSource source) {
    CloudNet.getInstance().getGroupConfigurationProvider().reload();
  }

  @CommandMethod("groups list")
  public void listGroups(CommandSource source) {
    Collection<GroupConfiguration> groups = CloudNet.getInstance().getGroupConfigurationProvider()
      .getGroupConfigurations();
    if (groups.isEmpty()) {
      return;
    }

    source.sendMessage("- Groups");
    source.sendMessage(" ");
    for (GroupConfiguration group : groups) {
      source.sendMessage("- " + group.getName());
    }
  }

  @CommandMethod("groups group <name>")
  public void displayGroup(CommandSource source, @Argument("name") GroupConfiguration group) {
    Collection<String> messages = new ArrayList<>();
    messages.add("Name: " + group.getName());
    messages.add("Environments:" + Arrays.toString(group.getTargetEnvironments().toArray()));

    CommandServiceConfiguration.applyServiceConfigurationDisplay(messages, group);
    source.sendMessage(messages);
  }

  @CommandMethod("groups group <name> add environment <environment>")
  public void addEnvironment(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    group.getTargetEnvironments().add(environmentType);
    this.groupProvider().addGroupConfiguration(group);
    source.sendMessage(LanguageManager.getMessage("command-groups-add-environment-success"));
  }

  @CommandMethod("groups group <name> remove environment <environment>")
  public void removeEnvironment(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    if (group.getTargetEnvironments().remove(environmentType)) {
      this.groupProvider().addGroupConfiguration(group);
      source.sendMessage(LanguageManager.getMessage("command-groups-remove-environment-success"));
    } else {
      source.sendMessage(LanguageManager.getMessage("command-groups-remove-environment-not-found"));
    }
  }

  @CommandMethod("groups group <name> add deployment <deployment>")
  public void addDeployment(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("deployment") ServiceTemplate template
  ) {
    ServiceDeployment deployment = new ServiceDeployment(template, new ArrayList<>());

    group.getDeployments().add(deployment);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> add template <template>")
  public void addTemplate(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("template") ServiceTemplate template
  ) {
    group.getTemplates().add(template);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> add inclusion <url> <path>")
  public void addInclusion(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("url") String url,
    @Argument("path") String path
  ) {
    ServiceRemoteInclusion inclusion = new ServiceRemoteInclusion(url, path);

    group.getIncludes().add(inclusion);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> add jvmOption <options>")
  public void addJvmOption(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Greedy @Argument("options") String jvmOptions
  ) {
    for (String jvmOption : jvmOptions.split(" ")) {
      group.getJvmOptions().add(jvmOption);
    }
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> add processParameter <options>")
  public void addProcessParameter(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Greedy @Argument("options") String processParameters
  ) {
    for (String processParameter : processParameters.split(" ")) {
      group.getProcessParameters().add(processParameter);
    }
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> remove deployment <deployment>")
  public void removeDeployment(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("deployment") ServiceTemplate template
  ) {
    ServiceDeployment deployment = new ServiceDeployment(template, new ArrayList<>());

    group.getDeployments().remove(deployment);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> remove template <template>")
  public void removeTemplate(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("template") ServiceTemplate template
  ) {
    group.getTemplates().remove(template);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> remove inclusion <url> <path>")
  public void removeInclusion(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("url") String url,
    @Argument("path") String path
  ) {
    ServiceRemoteInclusion inclusion = new ServiceRemoteInclusion(url, path);

    group.getIncludes().remove(inclusion);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> remove jvmOption <options>")
  public void removeJvmOption(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Greedy @Argument(value = "options") String jvmOptions
  ) {
    for (String jvmOption : jvmOptions.split(" ")) {
      group.getJvmOptions().remove(jvmOption);
    }
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> remove processParameter <options>")
  public void removeProcessParameter(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Greedy @Argument("options") String processParameters
  ) {
    for (String processParameter : processParameters.split(" ")) {
      group.getProcessParameters().remove(processParameter);
    }
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> clear jvmOptions")
  public void clearJvmOptions(CommandSource source, @Argument("name") GroupConfiguration group) {
    group.getJvmOptions().clear();
    this.updateGroup(group);
  }

  private void updateGroup(GroupConfiguration groupConfiguration) {
    this.groupProvider().addGroupConfiguration(groupConfiguration);
  }

  private GroupConfigurationProvider groupProvider() {
    return CloudNet.getInstance().getGroupConfigurationProvider();
  }


}
