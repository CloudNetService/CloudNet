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

import eu.cloudnetservice.cloudnet.driver.event.Event;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import lombok.NonNull;

public class CommandInvalidSyntaxEvent extends Event {

  private final CommandSource source;
  private final String correctSyntax;
  private String response;

  public CommandInvalidSyntaxEvent(
    @NonNull CommandSource source,
    @NonNull String correctSyntax,
    @NonNull String response
  ) {
    this.source = source;
    this.correctSyntax = correctSyntax;
    this.response = response;
  }

  /**
   * @return the command source that executed the commandline.
   */
  @NonNull
  public CommandSource commandSource() {
    return this.source;
  }

  /**
   * @return the correct syntax for the executed command
   */
  @NonNull
  public String correctSyntax() {
    return this.correctSyntax;
  }

  /**
   * @return the translated invalid syntax message that is the user will receive
   */
  @NonNull
  public String response() {
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
