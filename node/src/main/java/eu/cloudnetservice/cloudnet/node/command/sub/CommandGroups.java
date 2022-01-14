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
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.cloudnet.driver.service.GroupConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ServiceDeployment;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandPermission("cloudnet.command.groups")
@Description("Administers the configurations of all persistent groups")
public final class CommandGroups {

  @Parser(suggestions = "groupConfiguration")
  public GroupConfiguration defaultGroupParser(CommandContext<CommandSource> $, Queue<String> input) {
    var name = input.remove();

    var configuration = this.groupProvider().groupConfiguration(name);
    if (configuration == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-general-group-does-not-exist"));
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
    if (this.groupProvider().groupConfigurationPresent(groupName)) {
      source.sendMessage(I18n.trans("command-groups-group-already-existing"));
    } else {
      this.updateGroup(GroupConfiguration.builder().name(groupName).build());
      source.sendMessage(I18n.trans("command-groups-create-success", groupName));
    }
  }

  @CommandMethod("groups reload")
  public void reloadGroups(CommandSource source) {
    CloudNet.instance().groupConfigurationProvider().reload();
    source.sendMessage(I18n.trans("command-groups-reload-success"));
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

    CommandTasks.applyServiceConfigurationDisplay(messages, group);
    source.sendMessage(messages);
  }

  @CommandMethod("groups group <name> add environment <environment>")
  public void addEnvironment(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    group.targetEnvironments().add(environmentType.name());
    this.updateGroup(group);

    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "environment",
      environmentType.name(),
      group.name()));
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
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "deployment",
      deployment.template(),
      group.name()));
  }

  @CommandMethod("groups group <name> add template <template>")
  public void addTemplate(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("template") ServiceTemplate template
  ) {
    group.templates().add(template);
    this.updateGroup(group);
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "template",
      template,
      group.name()));
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
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "inclusion",
      inclusion,
      group.name()));
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
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "jvmOption",
      jvmOptions,
      group.name()));
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
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "processParameter",
      processParameters,
      group.name()));
  }

  @CommandMethod("groups group <name> remove environment <environment>")
  public void removeEnvironment(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    if (group.targetEnvironments().remove(environmentType.name())) {
      this.updateGroup(group);
    }
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "environment",
      environmentType.name(),
      group.name()));
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
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "deployment",
      deployment.template(),
      group.name()));
  }

  @CommandMethod("groups group <name> remove template <template>")
  public void removeTemplate(
    CommandSource source,
    @Argument("name") GroupConfiguration group,
    @Argument("template") ServiceTemplate template
  ) {
    group.templates().remove(template);
    this.updateGroup(group);
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "template",
      template,
      group.name()));
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
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "inclusion",
      inclusion,
      group.name()));
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
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "jvmOptions",
      jvmOptions,
      group.name()));
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
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "processParameter",
      processParameters,
      group.name()));
  }

  @CommandMethod("groups group <name> clear jvmOptions")
  public void clearJvmOptions(CommandSource source, @Argument("name") GroupConfiguration group) {
    group.jvmOptions().clear();
    this.updateGroup(group);
    source.sendMessage(I18n.trans("command-service-base-clear-property",
      "jvmOptions",
      "group",
      group.name()));
  }

  @CommandMethod("groups group <name> clear processParameters")
  public void clearProcessParameters(CommandSource source, @Argument("name") GroupConfiguration group) {
    group.processParameters().clear();
    this.updateGroup(group);
    source.sendMessage(I18n.trans("command-service-base-clear-property",
      "processParameters",
      "group",
      group.name()));
  }

  private void updateGroup(@NonNull GroupConfiguration groupConfiguration) {
    this.groupProvider().addGroupConfiguration(groupConfiguration);
  }

  private @NonNull GroupConfigurationProvider groupProvider() {
    return CloudNet.instance().groupConfigurationProvider();
  }

  private @NonNull Collection<String> parseExcludes(@Nullable String excludes) {
    if (excludes == null) {
      return Collections.emptyList();
    }

    return Arrays.asList(excludes.split(";"));
  }
}
