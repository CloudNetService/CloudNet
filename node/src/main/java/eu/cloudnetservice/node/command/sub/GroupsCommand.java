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
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandPermission("cloudnet.command.groups")
@Description("command-groups-description")
public final class GroupsCommand {

  @Parser(suggestions = "groupConfiguration")
  public @NonNull GroupConfiguration defaultGroupParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var name = input.remove();

    var configuration = this.groupProvider().groupConfiguration(name);
    if (configuration == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-general-group-does-not-exist"));
    }

    return configuration;
  }

  @Suggestions("groupConfiguration")
  public @NonNull List<String> suggestGroups(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.groupProvider().groupConfigurations().stream().map(Nameable::name).toList();
  }

  @CommandMethod("groups delete <name>")
  public void deleteGroup(@NonNull CommandSource source, @NonNull @Argument("name") GroupConfiguration configuration) {
    Node.instance().groupConfigurationProvider().removeGroupConfiguration(configuration);
    source.sendMessage(I18n.trans("command-groups-delete-group"));
  }

  @CommandMethod("groups create <name>")
  public void createGroup(@NonNull CommandSource source, @NonNull @Argument("name") String groupName) {
    if (this.groupProvider().groupConfiguration(groupName) != null) {
      source.sendMessage(I18n.trans("command-groups-group-already-existing"));
    } else {
      this.groupProvider().addGroupConfiguration(GroupConfiguration.builder().name(groupName).build());
      source.sendMessage(I18n.trans("command-groups-create-success", groupName));
    }
  }

  @CommandMethod("groups reload")
  public void reloadGroups(@NonNull CommandSource source) {
    Node.instance().groupConfigurationProvider().reload();
    source.sendMessage(I18n.trans("command-groups-reload-success"));
  }

  @CommandMethod("groups list")
  public void listGroups(@NonNull CommandSource source) {
    var groups = Node.instance().groupConfigurationProvider()
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
  public void displayGroup(@NonNull CommandSource source, @NonNull @Argument("name") GroupConfiguration group) {
    Collection<String> messages = new ArrayList<>();
    messages.add("Name: " + group.name());
    messages.add("Environments:" + Arrays.toString(group.targetEnvironments().toArray()));

    TasksCommand.applyServiceConfigurationDisplay(messages, group);
    source.sendMessage(messages);
  }

  @CommandMethod("groups group <name> add environment <environment>")
  public void addEnvironment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    this.updateGroup(group, builder -> builder.modifyTargetEnvironments(env -> env.add(environmentType.name())));
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "environment",
      environmentType.name(),
      group.name()));
  }

  @CommandMethod("groups group <name> add deployment <deployment>")
  public void addDeployment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
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
    this.updateGroup(group, builder -> builder.modifyDeployments(deployments -> deployments.add(deployment)));
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "deployment",
      deployment.template(),
      group.name()));
  }

  @CommandMethod("groups group <name> add template <template>")
  public void addTemplate(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("template") ServiceTemplate template
  ) {
    this.updateGroup(group, builder -> builder.modifyTemplates(templates -> templates.add(template)));
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "template",
      template,
      group.name()));
  }

  @CommandMethod("groups group <name> add inclusion <url> <path>")
  public void addInclusion(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("url") String url,
    @NonNull @Argument("path") String path
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();
    this.updateGroup(group, builder -> builder.modifyInclusions(inclusions -> inclusions.add(inclusion)));
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "inclusion",
      inclusion,
      group.name()));
  }

  @CommandMethod("groups group <name> add jvmOption <options>")
  public void addJvmOption(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Greedy @Argument("options") String jvmOptions
  ) {
    var splittedOptions = List.of(jvmOptions.split(" "));
    this.updateGroup(group, builder -> builder.modifyJvmOptions(options -> options.addAll(splittedOptions)));
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "jvmOption",
      jvmOptions,
      group.name()));
  }

  @CommandMethod("groups group <name> add processParameter <options>")
  public void addProcessParameter(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Greedy @Argument("options") String processParameters
  ) {
    var splittedOptions = List.of(processParameters.split(" "));
    this.updateGroup(
      group,
      builder -> builder.modifyProcessParameters(parameters -> parameters.addAll(splittedOptions)));
    source.sendMessage(I18n.trans("command-groups-add-collection-property",
      "processParameter",
      processParameters,
      group.name()));
  }

  @CommandMethod("groups group <name> remove environment <environment>")
  public void removeEnvironment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    this.updateGroup(group, builder -> builder.modifyTargetEnvironments(env -> env.remove(environmentType.name())));
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "environment",
      environmentType.name(),
      group.name()));
  }

  @CommandMethod("groups group <name> remove deployment <deployment>")
  public void removeDeployment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
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

    this.updateGroup(group, builder -> builder.modifyDeployments(deployments -> deployments.remove(deployment)));
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "deployment",
      deployment.template(),
      group.name()));
  }

  @CommandMethod("groups group <name> remove template <template>")
  public void removeTemplate(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("template") ServiceTemplate template
  ) {
    this.updateGroup(group, builder -> builder.modifyTemplates(templates -> templates.remove(template)));
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "template",
      template,
      group.name()));
  }

  @CommandMethod("groups group <name> remove inclusion <url> <path>")
  public void removeInclusion(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("url") String url,
    @NonNull @Argument("path") String path
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();
    this.updateGroup(group, builder -> builder.modifyInclusions(inclusions -> inclusions.remove(inclusion)));
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "inclusion",
      inclusion,
      group.name()));
  }

  @CommandMethod("groups group <name> remove jvmOption <options>")
  public void removeJvmOption(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Greedy @Argument(value = "options") String jvmOptions
  ) {
    var splittedOptions = List.of(jvmOptions.split(" "));
    this.updateGroup(group, builder -> builder.modifyJvmOptions(options -> options.removeAll(splittedOptions)));
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "jvmOptions",
      jvmOptions,
      group.name()));
  }

  @CommandMethod("groups group <name> remove processParameter <options>")
  public void removeProcessParameter(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Greedy @Argument("options") String processParameters
  ) {
    var splittedOptions = List.of(processParameters.split(" "));
    this.updateGroup(
      group,
      builder -> builder.modifyProcessParameters(parameters -> parameters.removeAll(splittedOptions)));
    source.sendMessage(I18n.trans("command-groups-remove-collection-property",
      "processParameter",
      processParameters,
      group.name()));
  }

  @CommandMethod("groups group <name> clear jvmOptions")
  public void clearJvmOptions(@NonNull CommandSource source, @NonNull @Argument("name") GroupConfiguration group) {
    this.updateGroup(group, builder -> builder.modifyJvmOptions(Collection::clear));
    source.sendMessage(I18n.trans("command-groups-clear-property",
      "jvmOptions",
      group.name()));
  }

  @CommandMethod("groups group <name> clear processParameters")
  public void clearProcessParameters(
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group
  ) {
    this.updateGroup(group, builder -> builder.modifyProcessParameters(Collection::clear));
    source.sendMessage(I18n.trans("command-groups-clear-property",
      "processParameters",
      group.name()));
  }

  private void updateGroup(@NonNull GroupConfiguration group, Consumer<GroupConfiguration.Builder> modifier) {
    modifier
      .andThen(builder -> this.groupProvider().addGroupConfiguration(builder.build()))
      .accept(GroupConfiguration.builder(group));
  }

  private @NonNull GroupConfigurationProvider groupProvider() {
    return Node.instance().groupConfigurationProvider();
  }
}
