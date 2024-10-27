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

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.node.command.source.CommandSource;
import lombok.NonNull;

public final class CommandNotFoundEvent extends Event {

  private final CommandSource commandSource;
  private final String commandLine;
  private String response;

  public CommandNotFoundEvent(
    @NonNull CommandSource commandSource,
    @NonNull String commandLine,
    @NonNull String response
  ) {
    this.commandSource = commandSource;
    this.commandLine = commandLine;
    this.response = response;
  }

  /**
   * @return the command source that executed the command.
   */
  public @NonNull CommandSource commandSource() {
    return this.commandSource;
  }

  /**
   * @return the executed commandline.
   */
  public @NonNull String commandLine() {
    return this.commandLine;
  }

  /**
   * @return the translated invalid syntax message that is the user will receive
   */
  public @NonNull String response() {
    return this.response;
  }

  /**
   * Set the translated invalid syntax message that is the user will receive
   *
   * @param response the message that the user will receive
   */
  public void response(@NonNull String response) {
    this.response = response;
  }
}
