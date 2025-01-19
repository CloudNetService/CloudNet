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
import java.util.List;
import lombok.NonNull;

public final class CommandInvalidSyntaxEvent extends Event {

  private final CommandSource source;
  private final String correctSyntax;
  private final List<String> fullCommandTree;
  private List<String> response;

  public CommandInvalidSyntaxEvent(
    @NonNull CommandSource source,
    @NonNull String correctSyntax,
    @NonNull List<String> fullCommandTree,
    @NonNull List<String> response
  ) {
    this.source = source;
    this.correctSyntax = correctSyntax;
    this.fullCommandTree = fullCommandTree;
    this.response = response;
  }

  /**
   * @return the command source that executed the commandline.
   */
  public @NonNull CommandSource commandSource() {
    return this.source;
  }

  /**
   * @return the correct syntax for the executed command
   */
  public @NonNull String correctSyntax() {
    return this.correctSyntax;
  }

  /**
   * Gets the full command tree for the input that lead to this event.
   *
   * @return the command tree.
   */
  public @NonNull List<String> fullCommandTree() {
    return this.fullCommandTree;
  }

  /**
   * @return the translated invalid syntax message that is the user will receive
   */
  public @NonNull List<String> response() {
    return this.response;
  }

  /**
   * Set the translated invalid syntax message that is the user will receive
   *
   * @param response the message that the user will receive
   */
  public void response(@NonNull List<String> response) {
    this.response = response;
  }
}
