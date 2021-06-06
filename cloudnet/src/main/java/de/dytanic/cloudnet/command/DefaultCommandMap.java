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

package de.dytanic.cloudnet.command;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class DefaultCommandMap implements ICommandMap {

  private final Map<String, Command> registeredCommands = new ConcurrentHashMap<>();

  @Override
  public void registerCommand(Command command) {
    if (command != null && command.isValid()) {
      for (String name : command.getNames()) {
        this.registeredCommands.put(name.toLowerCase(), command);

        if (command.getPrefix() != null && !command.getPrefix().isEmpty()) {
          this.registeredCommands.put(command.getPrefix().toLowerCase() + ":" + name.toLowerCase(), command);
        }
      }
    }
  }

  @Override
  public void unregisterCommand(String command) {
    this.registeredCommands.remove(command);
  }

  @Override
  public void unregisterCommand(Class<? extends Command> command) {
    Preconditions.checkNotNull(command);

    for (Command commandEntry : this.registeredCommands.values()) {
      if (commandEntry.getClass().equals(command)) {
        for (String commandName : commandEntry.getNames()) {
          this.registeredCommands.remove(commandName.toLowerCase());

          if (commandEntry.getPrefix() != null && !commandEntry.getPrefix().isEmpty()) {
            this.registeredCommands.remove(commandEntry.getPrefix().toLowerCase() + ":" + commandName.toLowerCase());
          }
        }
      }
    }
  }

  @Override
  public void unregisterCommands(ClassLoader classLoader) {
    Preconditions.checkNotNull(classLoader);

    for (Command commandEntry : this.registeredCommands.values()) {
      if (commandEntry.getClass().getClassLoader().equals(classLoader)) {
        for (String commandName : commandEntry.getNames()) {
          this.registeredCommands.remove(commandName.toLowerCase());

          if (commandEntry.getPrefix() != null && !commandEntry.getPrefix().isEmpty()) {
            this.registeredCommands.remove(commandEntry.getPrefix().toLowerCase() + ":" + commandName.toLowerCase());
          }
        }
      }
    }
  }

  @Override
  public void unregisterCommands() {
    this.registeredCommands.clear();
  }

  @Override
  public List<String> tabCompleteCommand(String commandLine) {
    if (commandLine.isEmpty() || commandLine.indexOf(' ') == -1) {
      return this.getCommandNames().stream()
        .filter(name -> name != null && name.toLowerCase().startsWith(commandLine.toLowerCase()))
        .collect(Collectors.toList());
    } else {
      Command command = this.getCommandFromLine(commandLine);

      if (command instanceof ITabCompleter) {
        String[] args = this.parseArgs(commandLine);
        String testString =
          args.length <= 1 || commandLine.endsWith(" ") ? "" : args[args.length - 1].toLowerCase().trim();
        if (commandLine.endsWith(" ")) {
          args = Arrays.copyOfRange(args, 1, args.length + 1);
          args[args.length - 1] = "";
        } else {
          args = Arrays.copyOfRange(args, 1, args.length);
        }

        Collection<String> responses = ((ITabCompleter) command)
          .complete(commandLine, args, Properties.parseLine(args));
        return this.filterResponses(testString, responses);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public List<String> tabCompleteCommand(String[] args, Properties properties) {
    if (args.length == 0) {
      return new ArrayList<>(this.getCommandNames());
    }

    Command command = this.getCommand(args[0]);

    if (command == null) {
      return this.getCommandNames().stream()
        .filter(name -> name != null && name.toLowerCase().startsWith(args[0].toLowerCase()))
        .collect(Collectors.toList());
    }

    if (command instanceof ITabCompleter) {

      String testString = args[args.length - 1].toLowerCase().trim();

      Collection<String> responses = ((ITabCompleter) command)
        .complete(String.join(" ", args), Arrays.copyOfRange(args, 1, args.length), properties);
      return this.filterResponses(testString, responses);
    }

    return Collections.emptyList();
  }

  private List<String> filterResponses(String testString, Collection<String> responses) {
    if (responses == null || responses.isEmpty()) {
      return Collections.emptyList();
    }
    return responses.stream()
      .filter(response -> response != null && (testString.isEmpty() || response.toLowerCase().startsWith(testString)))
      .map(response -> response.contains(" ") ? "\"" + response + "\"" : response)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<CommandInfo> getCommandInfos() {
    Collection<Command> commands = new ArrayList<>();

    for (Command command : this.registeredCommands.values()) {
      if (!commands.contains(command)) {
        commands.add(command);
      }
    }

    return commands.stream().map(this::commandInfoFilter).collect(Collectors.toList());
  }

  @Override
  public Command getCommand(String name) {
    if (name == null) {
      return null;
    }

    return this.registeredCommands.get(name.toLowerCase());
  }

  @Override
  public Command getCommandFromLine(String commandLine) {
    if (commandLine == null || commandLine.isEmpty()) {
      return null;
    }

    String[] args = this.parseArgs(commandLine);
    return args.length >= 1 ? this.registeredCommands.get(args[0].toLowerCase()) : null;
  }

  @Override
  public Collection<String> getCommandNames() {
    return this.registeredCommands.keySet();
  }

  @Override
  public boolean dispatchCommand(ICommandSender commandSender, String commandLine) {
    if (commandSender == null || commandLine == null || commandLine.trim().isEmpty()) {
      return false;
    }

    boolean response = true;

    String[] commands = commandLine.split(" && ");

    for (String command : commands) {
      response = response && this.dispatchCommand0(commandSender, command);
    }

    return response;
  }

  public boolean dispatchCommand0(ICommandSender commandSender, String commandLine) {
    String[] args = this.parseArgs(commandLine);

    if (args.length == 0 || !this.registeredCommands.containsKey(args[0].toLowerCase())) {
      return false;
    }

    String commandName = args[0].toLowerCase();
    Command command = this.registeredCommands.get(commandName);

    if (command.getPermission() != null && !commandSender.hasPermission(command.getPermission())) {
      return false;
    }

    args = Arrays.copyOfRange(args, 1, args.length);

    try {
      command.execute(commandSender, commandName, args, commandLine, Properties.parseLine(args));
      return true;

    } catch (Throwable throwable) {
      throw new CommandExecutionException(commandLine, throwable);
    }
  }

  private String[] parseArgs(String line) {
    if (line.trim().isEmpty()) {
      return new String[0];
    }

    Collection<String> args = new ArrayList<>();
    StringBuilder builder = new StringBuilder();
    char[] chars = line.toCharArray();
    boolean inQuote = false;

    for (char c : chars) {
      if (c == '"') {
        inQuote = !inQuote;
        continue;
      }

      if (!inQuote && c == ' ') {
        args.add(builder.toString());
        builder.setLength(0);
        continue;
      }

      builder.append(c);
    }

    if (inQuote) {
      builder.append('"');
    }

    if (builder.length() != 0) {
      args.add(builder.toString());
    }

    return args.toArray(new String[0]);
  }


  private CommandInfo commandInfoFilter(Command command) {
    return command.getInfo();
  }

}
