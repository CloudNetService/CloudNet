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
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowBasedFormatter;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.command.CommandInfo;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.List;
import java.util.Queue;
import lombok.NonNull;

@CommandAlias({"ask", "?"})
@CommandPermission("cloudnet.command.help")
@Description("commnad-help-description")
public final class HelpCommand {

  private static final RowBasedFormatter<CommandInfo> HELP_LIST_FORMATTER = RowBasedFormatter.<CommandInfo>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name(s)", "Description", "Permission").build())
    .column(info -> info.joinNameToAliases(", "))
    .column(CommandInfo::description)
    .column(CommandInfo::permission)
    .build();

  private final CommandProvider commandProvider;

  public HelpCommand(@NonNull CommandProvider commandProvider) {
    this.commandProvider = commandProvider;
  }

  @Parser(suggestions = "commands")
  public @NonNull CommandInfo defaultCommandInfoParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var command = input.remove();
    var commandInfo = this.commandProvider.command(command);
    if (commandInfo == null) {
      throw new ArgumentNotAvailableException(I18n.trans("no-such-command"));
    }

    return commandInfo;
  }

  @Suggestions("commands")
  public @NonNull List<String> suggestCommands(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.commandProvider.commands().stream().map(Nameable::name).toList();
  }

  @CommandMethod("help|ask|?")
  public void displayHelp(@NonNull CommandSource source) {
    source.sendMessage(HELP_LIST_FORMATTER.format(this.commandProvider.commands()));
  }

  @CommandMethod("help|ask|? <command>")
  public void displaySpecificHelp(@NonNull CommandSource source, @NonNull @Argument("command") CommandInfo command) {
    source.sendMessage("Names: " + command.joinNameToAliases(", "));
    source.sendMessage("Description: " + command.description());
    source.sendMessage("Usage: ");
    for (var usage : command.usage()) {
      source.sendMessage(" - " + usage);
    }
  }

  @CommandMethod("help|ask|? docs <command>")
  public void displayCommandDocs(@NonNull CommandSource source, @NonNull @Argument("command") CommandInfo commandInfo) {
    if (commandInfo.docsUrl() == null) {
      source.sendMessage(I18n.trans("command-help-docs-no-url", commandInfo.name()));
    } else {
      source.sendMessage(I18n.trans("command-help-docs", commandInfo.name(), commandInfo.docsUrl()));
    }
  }

}
