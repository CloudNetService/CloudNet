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

package eu.cloudnetservice.modules.npc.node;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowBasedFormatter;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.modules.npc.NPCManagement;
import eu.cloudnetservice.modules.npc.configuration.NPCConfigurationEntry;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.List;
import java.util.Queue;
import lombok.NonNull;

@CommandAlias("npcs")
@CommandPermission("cloudnet.command.npc")
@Description("Create new npc configurations")
public record NPCCommand(@NonNull NPCManagement npcManagement, @NonNull Node node) {

  private static final RowBasedFormatter<NPCConfigurationEntry> ENTRY_LIST_FORMATTER = RowBasedFormatter.<NPCConfigurationEntry>
      builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("targetGroup").build())
    .column(NPCConfigurationEntry::targetGroup)
    .build();

  @Parser(name = "newConfiguration", suggestions = "newConfiguration")
  public @NonNull String newConfigurationParser(
    @NonNull CommandContext<CommandSource> $,
    @NonNull Queue<String> input
  ) {
    var name = input.remove();
    var configuration = this.node.groupConfigurationProvider()
      .groupConfiguration(name);
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
    return this.node.groupConfigurationProvider().groupConfigurations().stream()
      .map(Nameable::name)
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
