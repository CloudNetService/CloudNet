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

package eu.cloudnetservice.cloudnet.node.event.command;

import eu.cloudnetservice.cloudnet.driver.event.Cancelable;
import eu.cloudnetservice.cloudnet.driver.event.Event;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import lombok.NonNull;

/**
 * This event is called before the actual processing of the given command is done. To cancel the execution of the
 * backing command use {@link #cancelled(boolean)} and set it to {@code true}
 */
public class CommandPreProcessEvent extends Event implements Cancelable {

  private final String commandLine;
  private final CommandSource commandSource;
  private boolean cancelled = false;

  public CommandPreProcessEvent(@NonNull String commandLine, @NonNull CommandSource commandSource) {
    this.commandLine = commandLine;
    this.commandSource = commandSource;
  }

  /**
   * @return the command line that will be executed
   */
  public @NonNull String commandLine() {
    return this.commandLine;
  }

  /**
   * @return the source that executes the command
   */
  public @NonNull CommandSource commandSource() {
    return this.commandSource;
  }

  /**
   * @return if the command execution if cancelled
   */
  public boolean cancelled() {
    return this.cancelled;
  }

  /**
   * @param cancelled whether this command execution should be cancelled or not.
   */
  public void cancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
