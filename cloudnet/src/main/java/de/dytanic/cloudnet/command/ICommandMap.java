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

import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import java.util.Collection;
import java.util.List;

/**
 * Represents a map from that all commands will manage.
 *
 * @see Command
 * @see CommandInfo
 * @see ICommandExecutor
 */
public interface ICommandMap {

  /**
   * Register a new command instance with all names that are is configured
   *
   * @param command the command that should registered
   */
  void registerCommand(Command command);

  /**
   * Unregister all commands from the map, that has the following string as name
   *
   * @param command the command name that should remove from the map
   */
  void unregisterCommand(String command);

  /**
   * Unregister all commands from the map, that command instance class is like the argument
   *
   * @param command the class reference that should remove from the map
   */
  void unregisterCommand(Class<? extends Command> command);

  /**
   * Unregister all commands from the classLoader instance.
   *
   * @param classLoader the classLoader from that all commands, that are contain will remove
   */
  void unregisterCommands(ClassLoader classLoader);

  /**
   * Remove all commands from the command map
   */
  void unregisterCommands();

  /**
   * Gets all tab complete results for the specific command line. If the line contains at least one space, it will get
   * the command and then the tab complete results out of it. If the line doesn't contain any spaces, it will return the
   * names of all registered commands that begin with the {@code commandLine} (case-insensitive).
   *
   * @param commandLine the command with arguments to get the results from
   * @return a list containing all unsorted results
   */
  List<String> tabCompleteCommand(String commandLine);

  /**
   * Gets all tab complete results for the specific command arguments and properties. If the length of the args is 0, it
   * will return the names of all registered commands. If the length of the args is 1, but the given command doesn't
   * exist, it will return the names of all registered commands that begin with {@code args[0]} (case-insensitive)
   *
   * @param args       the command arguments
   * @param properties the properties for the tab completer
   * @return a list containing all unsorted results
   */
  List<String> tabCompleteCommand(String[] args, Properties properties);

  /**
   * Transform all commands instances that are contain in the map into a CommandInfo object
   */
  Collection<CommandInfo> getCommandInfos();

  /**
   * Returns all command names that are contain in the map
   */
  Collection<String> getCommandNames();

  /**
   * Returns the command in the map with a specific name. If the name is more than one exist. The first command instance
   * that could be found will return
   *
   * @param name the name, from that the command should resolve
   * @return first command instance that could be found
   */
  Command getCommand(String name);

  /**
   * Returns the command object from the custom command line
   */
  Command getCommandFromLine(String commandLine);

  /**
   * Invokes a command execute() method when there is contain in the map and parsed from the command line the additional
   * properties.
   *
   * @param commandSender the command sender of the commandLine that should use
   * @param commandLine   the following commandline that should dispatch
   * @return true if the command will successful executed or false when the command cannot be found in the map or an
   * exception was thrown
   */
  boolean dispatchCommand(ICommandSender commandSender, String commandLine);


  /**
   * Register new command instances with all names that are is configured
   *
   * @param commands the commands that should registered
   */
  default void registerCommand(Command... commands) {
    if (commands != null) {
      for (Command command : commands) {
        if (command != null) {
          this.registerCommand(command);
        }
      }
    }
  }

  /**
   * Unregister all commands from the map, that has the following string as name
   *
   * @param commands the command names that should remove from the map
   */
  default void unregisterCommand(String... commands) {
    if (commands != null) {
      for (String command : commands) {
        if (command != null) {
          this.unregisterCommand(command);
        }
      }
    }
  }

  /**
   * Unregister all commands from the map, that command instance classes is like the argument
   *
   * @param commands the class references that should remove from the map
   */
  default void unregisterCommand(Class<? extends Command>... commands) {
    if (commands != null) {
      for (Class<? extends Command> c : commands) {
        if (c != null) {
          this.unregisterCommand(c);
        }
      }
    }
  }
}
