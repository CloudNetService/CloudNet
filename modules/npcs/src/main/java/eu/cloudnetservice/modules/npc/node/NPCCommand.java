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

package eu.cloudnetservice.modules.npc.node;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowedFormatter;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.registry.injection.Service;
import eu.cloudnetservice.modules.npc.NPCManagement;
import eu.cloudnetservice.modules.npc.configuration.NPCConfigurationEntry;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Queue;
import lombok.NonNull;

@Singleton
@CommandAlias("npcs")
@CommandPermission("cloudnet.command.npc")
@Description("module-npc-command-description")
public class NPCCommand {

  private static final RowedFormatter<NPCConfigurationEntry> ENTRY_LIST_FORMATTER = RowedFormatter.<NPCConfigurationEntry>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("targetGroup").build())
    .column(NPCConfigurationEntry::targetGroup)
    .build();

  private final NPCManagement npcManagement;
  private final GroupConfigurationProvider groupConfigurationProvider;

  @Inject
  public NPCCommand(
    @NonNull @Service NPCManagement npcManagement,
    @NonNull GroupConfigurationProvider groupConfigurationProvider
  ) {
    this.npcManagement = npcManagement;
    this.groupConfigurationProvider = groupConfigurationProvider;
  }

  @Parser(name = "newConfiguration", suggestions = "newConfiguration")
  public @NonNull String newConfigurationParser(
    @NonNull CommandContext<CommandSource> $,
    @NonNull Queue<String> input
  ) {
    var name = input.remove();
    var configuration = this.groupConfigurationProvider.groupConfiguration(name);
    if (configuration == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-general-group-does-not-exist"));
    }

    if (this.npcManagement.npcConfiguration().entries()
      .stream()
      .anyMatch(entry -> entry.targetGroup().equalsIgnoreCase(name))) {
      throw new ArgumentNotAvailableException(I18n.trans("module-npc-command-create-entry-group-already-exists"));
    }
    return name;
  }

  @Suggestions("newConfiguration")
  public @NonNull List<String> suggestNewConfigurations(
    @NonNull CommandContext<CommandSource> $,
    @NonNull String input
  ) {
    return this.groupConfigurationProvider.groupConfigurations().stream()
      .map(Named::name)
      .filter(group -> this.npcManagement.npcConfiguration()
        .entries()
        .stream()
        .noneMatch(entry -> entry.targetGroup().equals(group)))
      .toList();
  }

  @CommandMethod("npc|npcs list|l")
  public void listConfiguration(@NonNull CommandSource source) {
    source.sendMessage(ENTRY_LIST_FORMATTER.format(this.npcManagement.npcConfiguration().entries()));
  }

  @CommandMethod("npc|npcs create entry <targetGroup>")
  public void createEntry(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "targetGroup", parserName = "newConfiguration") String targetGroup
  ) {
    var entry = NPCConfigurationEntry.builder().targetGroup(targetGroup).build();
    this.npcManagement.npcConfiguration().entries().add(entry);
    this.npcManagement.npcConfiguration(this.npcManagement.npcConfiguration());
    source.sendMessage(I18n.trans("module-npc-command-create-entry-success"));
  }
}
