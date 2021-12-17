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

package de.dytanic.cloudnet.ext.bridge.node.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.config.ProxyFallbackConfiguration;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import org.jetbrains.annotations.NotNull;

@CommandPermission("cloudnet.command.bridge")
@Description("Management for the config of the bridge module")
public class CommandBridge {

  private final BridgeManagement bridgeManagement;
  private final GroupConfigurationProvider groupConfigurationProvider;

  public CommandBridge(@NotNull BridgeManagement bridgeManagement) {
    this.bridgeManagement = bridgeManagement;
    this.groupConfigurationProvider = CloudNet.instance().groupConfigurationProvider();
  }

  @Parser(name = "bridgeGroups", suggestions = "bridgeGroups")
  public GroupConfiguration bridgeGroupParser(@NotNull CommandContext<?> $, @NotNull Queue<String> input) {
    var name = input.remove();
    var group = this.groupConfigurationProvider.groupConfiguration(name);
    if (group == null) {
      throw new ArgumentNotAvailableException(I18n.trans("module-bridge-command-create-entry-group-not-found"));
    }
    var fallbacks = this.bridgeManagement.configuration().fallbackConfigurations()
      .stream();
    // don't allow duplicated entries
    if (fallbacks.anyMatch(fallback -> fallback.targetGroup().equals(group.name()))) {
      throw new ArgumentNotAvailableException(I18n.trans("module-bridge-command-entry-already-exists"));
    }
    return group;
  }

  @Suggestions("bridgeGroups")
  public List<String> suggestBridgeGroups(@NotNull CommandContext<?> $, String input) {
    return this.groupConfigurationProvider.groupConfigurations().stream()
      .map(INameable::name)
      .filter(group -> this.bridgeManagement.configuration().fallbackConfigurations().stream()
        .noneMatch(fallback -> fallback.targetGroup().equals(group)))
      .toList();
  }

  @CommandMethod("bridge create entry <targetGroup>")
  public void createBridgeEntry(
    @NotNull CommandSource source,
    @NotNull @Argument(value = "targetGroup", parserName = "bridgeGroups") GroupConfiguration group
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
    source.sendMessage(I18n.trans("module-bridge-command-create-entry-success"));
  }
}
