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
import eu.cloudnetservice.driver.event.Cancelable;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.Collection;
import lombok.NonNull;

/**
 * This event is called before the actual processing of the given command is done. To cancel the execution of the
 * backing command use {@link #cancelled(boolean)} and set it to true
 */
public final class CommandPreProcessEvent extends CommandProcessEvent implements Cancelable {

  private boolean cancelled;

  public CommandPreProcessEvent(
    @NonNull Collection<String> tokenizedCommandInput,
    @NonNull CommandInfo command,
    @NonNull CommandSource commandSource,
    @NonNull CommandProvider commandProvider
  ) {
    super(tokenizedCommandInput, command, commandSource, commandProvider);
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
