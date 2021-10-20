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
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import java.util.Collection;
import java.util.Queue;

@CommandAlias({"ask", "?"})
@CommandPermission("cloudnet.command.help")
@Description("Shows all commands and their description")
public final class CommandHelp {

  private final CommandProvider commandProvider;

  public CommandHelp(CommandProvider commandProvider) {
    this.commandProvider = commandProvider;
  }

  @Parser
  public CommandInfo defaultCommandInfoParser(CommandContext<CommandSource> $, Queue<String> input) {
    String command = input.remove();
    CommandInfo commandInfo = this.commandProvider.getCommand(command);
    if (commandInfo == null) {
      throw new ArgumentNotAvailableException(LanguageManager.getMessage("command-not-found"));
    }

    return commandInfo;
  }

  @CommandMethod("help|ask|?")
  public void displayHelp(CommandSource source) {
    Collection<CommandInfo> registeredCommands = this.commandProvider.getCommands();
    for (CommandInfo command : registeredCommands) {
      //TODO: format
      source.sendMessage(
        "Name: " + command.joinNameToAliases(", ") + " | Permission: " + command.getPermission() + " - "
          + command.getDescription());
    }
  }

  @CommandMethod("help|ask|? <command>")
  public void displaySpecificHelp(CommandSource source, @Argument("command") CommandInfo command) {
    source.sendMessage(" ");

    source.sendMessage("Names: " + command.joinNameToAliases(", "));
    source.sendMessage("Description: " + command.getDescription());
    source.sendMessage("Usage: ");
    for (String usage : command.getUsage()) {
      source.sendMessage(" - " + usage);
    }
  }

}
