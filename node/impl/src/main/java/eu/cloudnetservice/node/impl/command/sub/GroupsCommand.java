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

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotation.specifier.Quoted;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;
import org.jetbrains.annotations.Nullable;

@Singleton
@Permission("cloudnet.command.groups")
@Description("command-groups-description")
public final class GroupsCommand {

  private final GroupConfigurationProvider groupProvider;

  @Inject
  public GroupsCommand(@NonNull GroupConfigurationProvider groupProvider) {
    this.groupProvider = groupProvider;
  }

  @Parser(suggestions = "groupConfiguration")
  public @NonNull GroupConfiguration defaultGroupParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var name = input.readString();
    var configuration = this.groupProvider.groupConfiguration(name);
    if (configuration == null) {
      throw new ArgumentNotAvailableException(i18n.translate("command-general-group-does-not-exist"));
    }

    return configuration;
  }

  @Suggestions("groupConfiguration")
  public @NonNull Stream<String> suggestGroups() {
    return this.groupProvider.groupConfigurations().stream().map(Named::name);
  }

  @Parser(name = "inclusionCacheStrategy", suggestions = "inclusionCacheStrategy")
  public @NonNull String inclusionCacheStrategyParser(@NonNull @Service I18n i18n, @NonNull CommandInput input) {
    var strategy = input.readString();
    if (strategy.equals(ServiceRemoteInclusion.NO_CACHE_STRATEGY) ||
      strategy.equals(ServiceRemoteInclusion.KEEP_UNTIL_RESTART_STRATEGY)) {
      return strategy;
    }

    throw new ArgumentNotAvailableException(i18n.translate(
      "command-tasks-inclusion-cache-strategy-not-found",
      strategy));
  }

  @Suggestions("inclusionCacheStrategy")
  public @NonNull List<String> inclusionCacheStrategySuggester() {
    return List.of(ServiceRemoteInclusion.NO_CACHE_STRATEGY, ServiceRemoteInclusion.KEEP_UNTIL_RESTART_STRATEGY);
  }

  @Command("groups delete <name>")
  public void deleteGroup(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration configuration
  ) {
    this.groupProvider.removeGroupConfiguration(configuration);
    source.sendMessage(i18n.translate("command-groups-delete-group"));
  }

  @Command("groups create <name>")
  public void createGroup(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") String groupName
  ) {
    if (this.groupProvider.groupConfiguration(groupName) != null) {
      source.sendMessage(i18n.translate("command-groups-group-already-existing", groupName));
    } else {
      this.groupProvider.addGroupConfiguration(GroupConfiguration.builder().name(groupName).build());
      source.sendMessage(i18n.translate("command-groups-create-success", groupName));
    }
  }

  @Command("groups reload")
  public void reloadGroups(@NonNull @Service I18n i18n, @NonNull CommandSource source) {
    this.groupProvider.reload();
    source.sendMessage(i18n.translate("command-groups-reload-success"));
  }

  @Command("groups list")
  public void listGroups(@NonNull CommandSource source) {
    var groups = this.groupProvider.groupConfigurations();
    if (groups.isEmpty()) {
      return;
    }

    source.sendMessage("- Groups");
    source.sendMessage(" ");
    for (var group : groups) {
      source.sendMessage("- " + group.name());
    }
  }

  @Command("groups group <name>")
  public void displayGroup(@NonNull CommandSource source, @NonNull @Argument("name") GroupConfiguration group) {
    Collection<String> messages = new ArrayList<>();
    messages.add("Name: " + group.name());
    messages.add("Environments:" + Arrays.toString(group.targetEnvironments().toArray()));

    TasksCommand.applyServiceConfigurationDisplay(messages, group);
    source.sendMessage(messages);
  }

  @Command("groups rename <oldName> <newName>")
  public void renameGroup(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument(value = "oldName") GroupConfiguration group,
    @NonNull @Argument("newName") String newName
  ) {
    if (this.groupProvider.groupConfiguration(newName) != null) {
      source.sendMessage(i18n.translate("command-groups-group-already-existing", newName));
    } else {
      // create a copy with the new name and remove the old group
      this.groupProvider.removeGroupConfiguration(group);
      this.groupProvider.addGroupConfiguration(GroupConfiguration.builder(group).name(newName).build());
      source.sendMessage(i18n.translate("command-groups-rename-success", group.name(), newName));
    }
  }

  @Command("groups group <name> add environment <environment>")
  public void addEnvironment(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    this.updateGroup(group, builder -> builder.modifyTargetEnvironments(env -> env.add(environmentType.name())));
    source.sendMessage(i18n.translate("command-groups-add-collection-property",
      "environment",
      environmentType.name(),
      group.name()));
  }

  @Command("groups group <name> add deployment <deployment>")
  public void addDeployment(
    @NonNull @Service I18n i18n,
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
    source.sendMessage(i18n.translate("command-groups-add-collection-property",
      "deployment",
      deployment.template(),
      group.name()));
  }

  @Command("groups group <name> add template <template>")
  public void addTemplate(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("template") ServiceTemplate template
  ) {
    this.updateGroup(group, builder -> builder.modifyTemplates(templates -> templates.add(template)));
    source.sendMessage(i18n.translate("command-groups-add-collection-property",
      "template",
      template,
      group.name()));
  }

  @Command("groups group <name> add inclusion <url> <path> [cacheStrategy]")
  public void addInclusion(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("url") String url,
    @NonNull @Argument("path") String path,
    @NonNull @Default(ServiceRemoteInclusion.NO_CACHE_STRATEGY)
    @Argument(
      value = "cacheStrategy",
      parserName = "inclusionCacheStrategy") String cacheStrategy
  ) {
    var inclusion = ServiceRemoteInclusion.builder()
      .url(url)
      .destination(path)
      .cacheStrategy(cacheStrategy)
      .build();
    this.updateGroup(group, builder -> builder.modifyInclusions(inclusions -> inclusions.add(inclusion)));
    source.sendMessage(i18n.translate("command-groups-add-collection-property",
      "inclusion",
      inclusion,
      group.name()));
  }

  @Command("groups group <name> add jvmOption <options>")
  public void addJvmOption(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Greedy @Argument("options") String jvmOptions
  ) {
    var splittedOptions = List.of(jvmOptions.split(" "));
    this.updateGroup(group, builder -> builder.modifyJvmOptions(options -> options.addAll(splittedOptions)));
    source.sendMessage(i18n.translate("command-groups-add-collection-property",
      "jvmOption",
      jvmOptions,
      group.name()));
  }

  @Command("groups group <name> add processParameter <options>")
  public void addProcessParameter(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Greedy @Argument("options") String processParameters
  ) {
    var splittedOptions = List.of(processParameters.split(" "));
    this.updateGroup(
      group,
      builder -> builder.modifyProcessParameters(parameters -> parameters.addAll(splittedOptions)));
    source.sendMessage(i18n.translate("command-groups-add-collection-property",
      "processParameter",
      processParameters,
      group.name()));
  }

  @Command("groups group <name> remove environment <environment>")
  public void removeEnvironment(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("environment") ServiceEnvironmentType environmentType
  ) {
    this.updateGroup(group, builder -> builder.modifyTargetEnvironments(env -> env.remove(environmentType.name())));
    source.sendMessage(i18n.translate("command-groups-remove-collection-property",
      "environment",
      environmentType.name(),
      group.name()));
  }

  @Command("groups group <name> remove deployment <deployment>")
  public void removeDeployment(
    @NonNull @Service I18n i18n,
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
    source.sendMessage(i18n.translate("command-groups-remove-collection-property",
      "deployment",
      deployment.template(),
      group.name()));
  }

  @Command("groups group <name> remove template <template>")
  public void removeTemplate(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("template") ServiceTemplate template
  ) {
    this.updateGroup(group, builder -> builder.modifyTemplates(templates -> templates.remove(template)));
    source.sendMessage(i18n.translate("command-groups-remove-collection-property",
      "template",
      template,
      group.name()));
  }

  @Command("groups group <name> remove inclusion <url> <path>")
  public void removeInclusion(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Argument("url") String url,
    @NonNull @Argument("path") String path
  ) {
    var inclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();
    this.updateGroup(group, builder -> builder.modifyInclusions(inclusions -> inclusions.remove(inclusion)));
    source.sendMessage(i18n.translate("command-groups-remove-collection-property",
      "inclusion",
      inclusion,
      group.name()));
  }

  @Command("groups group <name> remove jvmOption <options>")
  public void removeJvmOption(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Greedy @Argument(value = "options") String jvmOptions
  ) {
    var splittedOptions = List.of(jvmOptions.split(" "));
    this.updateGroup(group, builder -> builder.modifyJvmOptions(options -> options.removeAll(splittedOptions)));
    source.sendMessage(i18n.translate("command-groups-remove-collection-property",
      "jvmOptions",
      jvmOptions,
      group.name()));
  }

  @Command("groups group <name> remove processParameter <options>")
  public void removeProcessParameter(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group,
    @NonNull @Greedy @Argument("options") String processParameters
  ) {
    var splittedOptions = List.of(processParameters.split(" "));
    this.updateGroup(
      group,
      builder -> builder.modifyProcessParameters(parameters -> parameters.removeAll(splittedOptions)));
    source.sendMessage(i18n.translate("command-groups-remove-collection-property",
      "processParameter",
      processParameters,
      group.name()));
  }

  @Command("groups group <name> clear jvmOptions")
  public void clearJvmOptions(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group
  ) {
    this.updateGroup(group, builder -> builder.modifyJvmOptions(Collection::clear));
    source.sendMessage(i18n.translate("command-groups-clear-property",
      "jvmOptions",
      group.name()));
  }

  @Command("groups group <name> clear processParameters")
  public void clearProcessParameters(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group
  ) {
    this.updateGroup(group, builder -> builder.modifyProcessParameters(Collection::clear));
    source.sendMessage(i18n.translate("command-groups-clear-property",
      "processParameters",
      group.name()));
  }

  @Command("groups group <name> clear inclusions")
  public void clearInclusions(
    @NonNull @Service I18n i18n,
    @NonNull CommandSource source,
    @NonNull @Argument("name") GroupConfiguration group
  ) {
    this.updateGroup(group, builder -> builder.modifyInclusions(Collection::clear));
    source.sendMessage(i18n.translate("command-groups-clear-property",
      "inclusions",
      group.name()));
  }

  private void updateGroup(@NonNull GroupConfiguration group, Consumer<GroupConfiguration.Builder> modifier) {
    modifier
      .andThen(builder -> this.groupProvider.addGroupConfiguration(builder.build()))
      .accept(GroupConfiguration.builder(group));
  }
}
