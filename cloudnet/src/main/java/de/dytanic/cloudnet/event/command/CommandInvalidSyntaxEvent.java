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
import org.jetbrains.annotations.NotNull;

public class CommandInvalidSyntaxEvent extends Event {

  private final CommandSource source;
  private final String correctSyntax;
  private String response;

  public CommandInvalidSyntaxEvent(CommandSource source, String correctSyntax, String response) {
    this.source = source;
    this.correctSyntax = correctSyntax;
    this.response = response;
  }

  @NotNull
  public CommandSource getCommandSource() {
    return this.source;
  }

  @NotNull
  public String getCorrectSyntax() {
    return this.correctSyntax;
  }

  @NotNull
  public String getResponse() {
    return this.response;
  }

  public void setResponse(@NotNull String response) {
    this.response = response;
  }
}
