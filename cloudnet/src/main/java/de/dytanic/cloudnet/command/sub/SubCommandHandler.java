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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.language.LanguageManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class SubCommandHandler extends Command implements ITabCompleter {

  private Collection<SubCommand> subCommands = new ArrayList<>();

  public SubCommandHandler(String... names) {
    super(names);
  }

  public SubCommandHandler(Collection<SubCommand> subCommands, String... names) {
    super(names);
    this.subCommands = subCommands;
  }

  public SubCommandHandler(String[] names, String permission, Collection<SubCommand> subCommands) {
    super(names, permission);
    this.subCommands = subCommands;
  }

  public SubCommandHandler(String[] names, String permission, String description, Collection<SubCommand> subCommands) {
    super(names, permission, description);
    this.subCommands = subCommands;
  }

  public SubCommandHandler(String[] names, String permission, String description, String usage, String prefix,
    Collection<SubCommand> subCommands) {
    super(names, permission, description, usage, prefix);
    this.subCommands = subCommands;
  }

  @Override
  public String getUsage() {
    Collection<String> messages = new ArrayList<>();
    for (SubCommand subCommand : this.subCommands) {
      String message = super.getNames()[0] + " " + subCommand.getArgsAsString() + subCommand.getExtendedUsage();

      if (subCommand.getPermission() != null) {
        message += " | " + subCommand.getPermission();
      }

      if (subCommand.getDescription() != null) {
        message += " | " + subCommand.getDescription();
      }
      messages.add(message);
    }
    if (messages.isEmpty()) {
      return null;
    }
    if (messages.size() == 1) {
      return messages.iterator().next();
    }
    return "\n - " + String.join("\n - ", messages);
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
    SubCommand subCommand = null;
    SubCommandArgument<?>[] arguments = null;

    for (SubCommand registeredCommand : this.subCommands) {
      arguments = registeredCommand.parseArgs(args);
      if (arguments != null && arguments.length > 0) {
        subCommand = registeredCommand;
        break;
      }
    }

    if (subCommand == null) {
      for (SubCommand registeredCommand : this.subCommands) {
        Pair<String, Integer> invalidArgumentMessage = registeredCommand.getInvalidArgumentMessage(args);
        if (invalidArgumentMessage != null && invalidArgumentMessage.getSecond() == 0) {
          sender.sendMessage(invalidArgumentMessage.getFirst());
          return;
        }
      }

      this.sendHelp(sender);
      return;
    }

    this.executeCommand(sender, command, args, commandLine, subCommand, arguments);
  }

  protected void executeCommand(ICommandSender sender, String command, String[] args, String commandLine,
    SubCommand subCommand, SubCommandArgument<?>[] parsedArgs) {
    if (subCommand.isOnlyConsole() && !(sender instanceof ConsoleCommandSender)) {
      sender.sendMessage(LanguageManager.getMessage("command-sub-only-console"));
      return;
    }

    if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
      sender.sendMessage(LanguageManager.getMessage("command-sub-no-permission"));
      return;
    }

    if (subCommand.isAsync()) {
      CloudNet.getInstance().getTaskExecutor().execute(() ->
        subCommand.execute(
          subCommand, sender, command, new SubCommandArgumentWrapper(parsedArgs),
          commandLine, subCommand.parseProperties(args), new HashMap<>()
        )
      );
    } else {
      subCommand.execute(
        subCommand, sender, command, new SubCommandArgumentWrapper(parsedArgs),
        commandLine, subCommand.parseProperties(args), new HashMap<>()
      );
    }
  }

  protected void sendHelp(ICommandSender sender) {
    for (String usageLine : this.getUsage().split("\n")) {
      sender.sendMessage(usageLine);
    }
  }

  @Override
  public Collection<String> complete(String commandLine, String[] args, Properties properties) {
    return this.subCommands.stream()
      .map(subCommand -> subCommand.getNextPossibleArgumentAnswers(args))
      .filter(Objects::nonNull)
      .filter(responses -> !responses.isEmpty())
      .flatMap(Collection::parallelStream)
      .collect(Collectors.toSet());
  }

  protected void setSubCommands(Collection<SubCommand> subCommands) {
    this.subCommands = subCommands;
  }
}
