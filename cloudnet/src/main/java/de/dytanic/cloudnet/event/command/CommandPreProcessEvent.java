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

package de.dytanic.cloudnet.event.command;

import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.ICancelable;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called before the actual processing of the given command is done. To cancel the execution of the
 * backing command use {@link #setCancelled(boolean)} and set it to {@code true}
 */
public class CommandPreProcessEvent extends Event implements ICancelable {

  private final String commandLine;
  private final CommandSource commandSource;
  private boolean cancelled = false;

  public CommandPreProcessEvent(@NotNull String commandLine, @NotNull CommandSource commandSource) {
    this.commandLine = commandLine;
    this.commandSource = commandSource;
  }

  /**
   * @return the command line that will be executed
   */
  @NotNull
  public String getCommandLine() {
    return this.commandLine;
  }

  /**
   * @return the source that executes the command
   */
  @NotNull
  public CommandSource getCommandSource() {
    return this.commandSource;
  }

  /**
   * @return if the command execution if cancelled
   */
  public boolean isCancelled() {
    return this.cancelled;
  }

  /**
   * @param cancelled whether this command execution should be cancelled or not.
   */
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
