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
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.Nameable;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import org.jetbrains.annotations.Nullable;

@CommandPermission("cloudnet.command.groups")
@Description("Administers the configurations of all persistent groups")
public final class CommandGroups {

  @Parser(suggestions = "groupConfiguration")
  public GroupConfiguration defaultGroupParser(CommandContext<CommandSource> $, Queue<String> input) {
    var name = input.remove();

    var configuration = this.groupProvider().groupConfiguration(name);
    if (configuration == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-service-base-group-not-found"));
    }

    return configuration;
  }

  @Suggestions("groupConfiguration")
  public List<String> suggestGroups(CommandContext<CommandSource> $, String input) {
    return this.groupProvider().groupConfigurations().stream().map(Nameable::name).toList();
  }

  @CommandMethod("groups delete <name>")
  public void deleteGroup(CommandSource source, @Argument("name") GroupConfiguration configuration) {
    CloudNet.instance().groupConfigurationProvider().removeGroupConfiguration(configuration);
    source.sendMessage(I18n.trans("command-groups-delete-group"));
  }

  @CommandMethod("groups create <name>")
  public void createGroup(CommandSource source, @Argument("name") String groupName) {
    if (!this.groupProvider().groupConfigurationPresent(groupName)) {
      this.groupProvider().addGroupConfiguration(GroupConfiguration.builder().name(groupName).build());
    }
  }

  @CommandMethod("groups reload")
  public void reloadGroups(CommandSource source) {
    CloudNet.instance().groupConfigurationProvider().reload();
  }

  @CommandMethod("groups list")
  public void listGroups(CommandSource source) {
    var groups = CloudNet.instance().groupConfigurationProvider()
      .groupConfigurations();
    if (groups.isEmpty()) {
      return;
    }

    source.sendMessage("- Groups");
    source.sendMessage(" ");
    for (var group : groups) {
      source.sendMessage("- " + group.name());
    }
  }

  @CommandMethod("groups group <name>")
  public void displayGroup(CommandSource source, @Argument("name") GroupConfiguration group) {
    Collection<String> messages = new ArrayList<>();
    messages.add("Name: " + group.name());
    messages.add("Environments:" + Arrays.toString(group.targetEnvironments().toArray()));

    CommandServiceConfiguration.applyServiceConfigurationDisplay(messages, group);
    source.sendMessage(messages);
  }

  @CommandMethod("groups group <name> add environment <environment>")
  public void addEnvironment(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    group.targetEnvironments().add(environmentType.name());
    this.groupProvider().addGroupConfiguration(group);
    source.sendMessage(I18n.trans("command-groups-add-environment-success"));
  }

  @CommandMethod("groups group <name> remove environment <environment>")
  public void removeEnvironment(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    if (group.targetEnvironments().remove(environmentType.name())) {
      this.groupProvider().addGroupConfiguration(group);
      source.sendMessage(I18n.trans("command-groups-remove-environment-success"));
    } else {
      source.sendMessage(I18n.trans("command-groups-remove-environment-not-found"));
    }
  }

  @CommandMethod("groups group <name> add deployment <deployment>")
  public void addDeployment(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("deployment") ServiceTemplate template,
    @Flag("excludes") @Quoted String excludes
  ) {
    var deployment = ServiceDeployment.builder()
      .template(template)
      .excludes(this.parseExcludes(excludes))
      .build();

    group.deployments().add(deployment);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> add template <template>")
  public void addTemplate(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("template") ServiceTemplate template
  ) {
    group.templates().add(template);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> add inclusion <url> <path>")
  public void addInclusion(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("url") String url,
    @Argument("path") String path
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();

    group.includes().add(inclusion);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> add jvmOption <options>")
  public void addJvmOption(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Greedy @Argument("options") String jvmOptions
  ) {
    for (var jvmOption : jvmOptions.split(" ")) {
      group.jvmOptions().add(jvmOption);
    }
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> add processParameter <options>")
  public void addProcessParameter(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Greedy @Argument("options") String processParameters
  ) {
    for (var processParameter : processParameters.split(" ")) {
      group.processParameters().add(processParameter);
    }
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> remove deployment <deployment>")
  public void removeDeployment(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("deployment") ServiceTemplate template,
    @Flag("excludes") @Quoted String excludes
  ) {
    var deployment = ServiceDeployment.builder()
      .template(template)
      .excludes(this.parseExcludes(excludes))
      .build();

    group.deployments().remove(deployment);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> remove template <template>")
  public void removeTemplate(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("template") ServiceTemplate template
  ) {
    group.templates().remove(template);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> remove inclusion <url> <path>")
  public void removeInclusion(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("url") String url,
    @Argument("path") String path
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();

    group.includes().remove(inclusion);
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> remove jvmOption <options>")
  public void removeJvmOption(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Greedy @Argument(value = "options") String jvmOptions
  ) {
    for (var jvmOption : jvmOptions.split(" ")) {
      group.jvmOptions().remove(jvmOption);
    }
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> remove processParameter <options>")
  public void removeProcessParameter(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Greedy @Argument("options") String processParameters
  ) {
    for (var processParameter : processParameters.split(" ")) {
      group.processParameters().remove(processParameter);
    }
    this.updateGroup(group);
  }

  @CommandMethod("groups group <name> clear jvmOptions")
  public void clearJvmOptions(CommandSource source, @Argument("name") GroupConfiguration group) {
    group.jvmOptions().clear();
    this.updateGroup(group);
  }

  private void updateGroup(GroupConfiguration groupConfiguration) {
    this.groupProvider().addGroupConfiguration(groupConfiguration);
  }

  private GroupConfigurationProvider groupProvider() {
    return CloudNet.instance().groupConfigurationProvider();
  }

  private Collection<String> parseExcludes(@Nullable String excludes) {
    if (excludes == null) {
      return Collections.emptyList();
    }

    return Arrays.asList(excludes.split(";"));
  }


}
