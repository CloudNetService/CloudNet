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
import de.dytanic.cloudnet.command.defaults.DefaultCommandProvider;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.command.source.ConsoleCommandSource;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.event.command.CommandInvalidSyntaxEvent;
import de.dytanic.cloudnet.event.command.CommandNotFoundEvent;
import java.util.concurrent.CompletionException;

public class CommandExceptionHandler {

  private static final Logger LOGGER = LogManager.getLogger(CommandExceptionHandler.class);

  private final DefaultCommandProvider commandProvider;

  public CommandExceptionHandler(DefaultCommandProvider commandProvider) {
    this.commandProvider = commandProvider;
  }

  public void handleCompletionException(CommandSource source, CompletionException exception) {
    Throwable cause = exception.getCause();
    // the completable future wraps exceptions, so this shouldn't be null
    if (cause == null) {
      return;
    }
    // determine the exception type and apply the specific handler
    if (cause instanceof InvalidSyntaxException) {
      this.handleInvalidSyntaxException(source, (InvalidSyntaxException) cause);
    } else if (cause instanceof NoPermissionException) {
      this.handleNoPermissionException(source, (NoPermissionException) cause);
    } else if (cause instanceof NoSuchCommandException) {
      this.handleNoSuchCommandException(source, (NoSuchCommandException) cause);
    } else if (cause instanceof InvalidCommandSenderException) {
      this.handleInvalidCommandSourceException(source, (InvalidCommandSenderException) cause);
    } else if (cause instanceof ArgumentParseException) {
      Throwable deepCause = cause.getCause();
      if (deepCause instanceof ArgumentNotAvailableException) {
        this.handleArgumentNotAvailableException(source, (ArgumentNotAvailableException) deepCause);
      } else {
        this.handleArgumentParseException(source, (ArgumentParseException) cause);
      }
    } else {
      LOGGER.severe("Exception during command execution", exception.getCause());
    }
  }

  public void handleArgumentParseException(CommandSource source, ArgumentParseException exception) {
    source.sendMessage(exception.getMessage());
  }

  public void handleArgumentNotAvailableException(CommandSource source, ArgumentNotAvailableException exception) {
    source.sendMessage(exception.getMessage());
  }

  public void handleInvalidSyntaxException(CommandSource source, InvalidSyntaxException exception) {
    if (this.commandProvider.replyWithCommandHelp(source, exception.getCurrentChain())) {
      return;
    }

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
