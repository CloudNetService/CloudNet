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

import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public interface CommandProvider {

  /**
   * Get command suggestions for the "next" argument that would yield a correctly parsing command input.
   *
   * @param source the command source that
   * @param input  the input to get the suggestions for
   * @return the suggestions for the current input
   */
  @NotNull
  List<String> suggest(@NotNull CommandSource source, @NotNull String input);

  /**
   * Executes a command with the given command source and responds to the input.
   *
   * @param source the command source that is used to execute the command
   * @param input  the commandline that is executed
   */
  void execute(@NotNull CommandSource source, @NotNull String input);

  /**
   * Register a command for the node
   *
   * @param command the command to register
   */
  void register(@NotNull Object command);

  @Nullable
  CommandInfo getCommand(@NotNull String input);

  @NotNull
  @UnmodifiableView
  Collection<CommandInfo> getCommands();
}
