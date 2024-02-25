/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.event.command;

import eu.cloudnetservice.driver.command.CommandInfo;
import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.Collection;
import lombok.NonNull;

class CommandProcessEvent extends Event {

  private final Collection<String> tokenizedCommandInput;
  private final CommandInfo command;
  private final CommandSource commandSource;
  private final CommandProvider commandProvider;

  public CommandProcessEvent(
    @NonNull Collection<String> tokenizedCommandInput,
    @NonNull CommandInfo command,
    @NonNull CommandSource commandSource,
    @NonNull CommandProvider commandProvider
  ) {
    this.tokenizedCommandInput = tokenizedCommandInput;
    this.command = command;
    this.commandSource = commandSource;
    this.commandProvider = commandProvider;
  }

  /**
   * Gets the tokenized command input as a collection of strings.
   *
   * @return the command line as a list.
   */
  public @NonNull Collection<String> tokenizedCommandInput() {
    return this.tokenizedCommandInput;
  }

  /**
   * Gets the root command info of the command that lead to this event call.
   *
   * @return the command info to the event.
   */
  public @NonNull CommandInfo command() {
    return this.command;
  }

  /**
   * Gets the command source that executed the command and lead to this event call.
   *
   * @return the command executor.
   */
  public @NonNull CommandSource commandSource() {
    return this.commandSource;
  }

  /**
   * Gets the command provider that was used to register the executed command.
   *
   * @return the command provider instance.
   */
  public @NonNull CommandProvider commandProvider() {
    return this.commandProvider;
  }
}
