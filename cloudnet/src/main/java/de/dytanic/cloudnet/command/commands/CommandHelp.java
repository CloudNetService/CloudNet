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

package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.driver.util.ColumnTextFormatter;
import de.dytanic.cloudnet.driver.util.PrefixedMessageMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public final class CommandHelp extends CommandDefault implements ITabCompleter {

  public CommandHelp() {
    super("help", "ask");
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
    switch (args.length) {
      case 0:
        String[] messages = ColumnTextFormatter.mapToEqual(this.getCloudNet().getCommandMap().getCommandInfos(), ' ',
          new PrefixedMessageMapper<>("Aliases: ", info -> Arrays.toString(info.getNames())),
          new PrefixedMessageMapper<>(" | Permission: ", CommandInfo::getPermission),
          new PrefixedMessageMapper<>(" - ", CommandInfo::getDescription)
        );

        sender.sendMessage(messages);
        sender.sendMessage(LanguageManager.getMessage("command-help-info"));

        break;
      case 1:

        if (this.getCloudNet().getCommandMap().getCommandNames().contains(args[0].toLowerCase())) {
          Command commandInfo = this.getCloudNet().getCommandMap().getCommand(args[0].toLowerCase());

          if (commandInfo != null) {
            sender.sendMessage(" ", "Aliases: " + Arrays.toString(commandInfo.getNames()));
            String description = commandInfo.getDescription();
            String usage = commandInfo.getUsage();

            if (description != null) {
              sender.sendMessage("Description: " + description);
            }
            if (usage != null) {
              String[] usages = ("Usage: " + usage).split("\n");
              for (String line : usages) {
                sender.sendMessage(line);
              }
            }
          }
        }
        break;
      default:
        break;
    }
  }

  @Override
  public Collection<String> complete(String commandLine, String[] args, Properties properties) {
    Collection<String> x = new ArrayList<>();

    for (CommandInfo commandInfo : this.getCloudNet().getCommandMap().getCommandInfos()) {
      x.addAll(Arrays.asList(commandInfo.getNames()));
    }

    return x;
  }
}
