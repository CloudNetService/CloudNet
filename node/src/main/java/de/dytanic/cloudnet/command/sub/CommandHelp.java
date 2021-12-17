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
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.column.ColumnFormatter;
import de.dytanic.cloudnet.common.column.RowBasedFormatter;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import java.util.Queue;

@CommandAlias({"ask", "?"})
@CommandPermission("cloudnet.command.help")
@Description("Shows all commands and their description")
public final class CommandHelp {

  private static final RowBasedFormatter<CommandInfo> HELP_LIST_FORMATTER = RowBasedFormatter.<CommandInfo>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name(s)", "Description", "Permission").build())
    .column(info -> info.joinNameToAliases(", "))
    .column(CommandInfo::description)
    .column(CommandInfo::permission)
    .build();

  private final CommandProvider commandProvider;

  public CommandHelp(CommandProvider commandProvider) {
    this.commandProvider = commandProvider;
  }

  @Parser
  public CommandInfo defaultCommandInfoParser(CommandContext<CommandSource> $, Queue<String> input) {
    var command = input.remove();
    var commandInfo = this.commandProvider.getCommand(command);
    if (commandInfo == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-not-found"));
    }

    return commandInfo;
  }

  @CommandMethod("help|ask|?")
  public void displayHelp(CommandSource source) {
    source.sendMessage(HELP_LIST_FORMATTER.format(this.commandProvider.getCommands()));
  }

  @CommandMethod("help|ask|? <command>")
  public void displaySpecificHelp(CommandSource source, @Argument("command") CommandInfo command) {
    source.sendMessage(" ");

    source.sendMessage("Names: " + command.joinNameToAliases(", "));
    source.sendMessage("Description: " + command.description());
    source.sendMessage("Usage: ");
    for (var usage : command.usage()) {
      source.sendMessage(" - " + usage);
    }
  }

}
