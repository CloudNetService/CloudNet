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

package de.dytanic.cloudnet.command.exception;

import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.exceptions.NoSuchCommandException;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.command.source.ConsoleCommandSource;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.event.command.CommandInvalidSyntaxEvent;
import de.dytanic.cloudnet.event.command.CommandNotFoundEvent;

public class CommandExceptionHandler {

  public void handleArgumentParseException(CommandSource source, ArgumentParseException exception) {
    source.sendMessage(exception.getMessage());
  }

  public void handleArgumentNotAvailableException(CommandSource source, ArgumentNotAvailableException exception) {
    source.sendMessage(exception.getMessage());
  }

  public void handleInvalidSyntaxException(CommandSource source, InvalidSyntaxException exception) {
    CommandInvalidSyntaxEvent invalidSyntaxEvent = CloudNet.getInstance().getEventManager().callEvent(
      new CommandInvalidSyntaxEvent(
        source,
        exception.getCorrectSyntax(),
        LanguageManager.getMessage("command-invalid-syntax")
          .replace("%syntax%", exception.getCorrectSyntax())
      )
    );
    source.sendMessage(invalidSyntaxEvent.getResponse());
  }

  public void handleNoSuchCommandException(CommandSource source, NoSuchCommandException exception) {
    CommandNotFoundEvent notFoundEvent = CloudNet.getInstance().getEventManager().callEvent(
      new CommandNotFoundEvent(
        source,
        exception.getSuppliedCommand(),
        LanguageManager.getMessage("command-not-found")
      )
    );
    source.sendMessage(notFoundEvent.getResponse());
  }

  public void handleNoPermissionException(CommandSource source, NoPermissionException exception) {
    source.sendMessage(LanguageManager.getMessage("command-sub-no-permission"));
  }

  public void handleInvalidCommandSourceException(CommandSource source, InvalidCommandSenderException exception) {
    if (exception.getRequiredSender() == ConsoleCommandSource.class) {
      source.sendMessage(LanguageManager.getMessage("command-console-only"));
    } else {
      source.sendMessage(LanguageManager.getMessage("command-driver-only"));
    }
  }

}
