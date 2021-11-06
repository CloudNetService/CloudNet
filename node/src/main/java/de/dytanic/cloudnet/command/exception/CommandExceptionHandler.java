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

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.compound.FlagArgument;
import cloud.commandframework.arguments.compound.FlagArgument.FlagParseException;
import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.exceptions.NoSuchCommandException;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.defaults.DefaultCommandProvider;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.command.source.ConsoleCommandSource;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.event.command.CommandInvalidSyntaxEvent;
import de.dytanic.cloudnet.event.command.CommandNotFoundEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class CommandExceptionHandler {

  protected static final Logger LOGGER = LogManager.getLogger(CommandExceptionHandler.class);

  private final DefaultCommandProvider commandProvider;

  public CommandExceptionHandler(DefaultCommandProvider commandProvider) {
    this.commandProvider = commandProvider;
  }

  /**
   * Handles occurring exceptions when executing a command and waiting for the result of the future. All relevant
   * exceptions are handled, other exceptions are printed into the console.
   *
   * @param source the source of the command.
   * @param cause  the exception that occurred during the execution.
   */
  public void handleCommandExceptions(CommandSource source, Throwable cause) {
    // there is no cause if no exception occurred
    if (cause == null) {
      return;
    }
    // determine the cause type and apply the specific handler
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
      } else if (deepCause instanceof FlagArgument.FlagParseException) {
        this.handleFlagParseException(source, (FlagParseException) deepCause);
      } else {
        this.handleArgumentParseException(source, (ArgumentParseException) cause);
      }
    } else {
      LOGGER.severe("Exception during command execution", cause);
    }
  }

  protected void handleArgumentParseException(CommandSource source, ArgumentParseException exception) {
    source.sendMessage(exception.getMessage());
  }

  protected void handleFlagParseException(CommandSource source, FlagParseException flagParseException) {
    // we just ignore this as we can't really handle this due to cloud
  }

  protected void handleArgumentNotAvailableException(CommandSource source, ArgumentNotAvailableException exception) {
    source.sendMessage(exception.getMessage());
  }

  protected void handleInvalidSyntaxException(CommandSource source, InvalidSyntaxException exception) {
    if (this.replyWithCommandHelp(source, exception.getCurrentChain())) {
      return;
    }

    CommandInvalidSyntaxEvent invalidSyntaxEvent = CloudNet.getInstance().getEventManager().callEvent(
      new CommandInvalidSyntaxEvent(
        source,
        exception.getCorrectSyntax(),
        I18n.trans("command-invalid-syntax")
          .replace("%syntax%", exception.getCorrectSyntax())
      )
    );
    source.sendMessage(invalidSyntaxEvent.getResponse());
  }

  protected void handleNoSuchCommandException(CommandSource source, NoSuchCommandException exception) {
    CommandNotFoundEvent notFoundEvent = CloudNet.getInstance().getEventManager().callEvent(
      new CommandNotFoundEvent(
        source,
        exception.getSuppliedCommand(),
        I18n.trans("command-not-found")
      )
    );
    source.sendMessage(notFoundEvent.getResponse());
  }

  protected void handleNoPermissionException(CommandSource source, NoPermissionException exception) {
    source.sendMessage(I18n.trans("command-sub-no-permission"));
  }

  protected void handleInvalidCommandSourceException(CommandSource source, InvalidCommandSenderException exception) {
    if (exception.getRequiredSender() == ConsoleCommandSource.class) {
      source.sendMessage(I18n.trans("command-console-only"));
    } else {
      source.sendMessage(I18n.trans("command-driver-only"));
    }
  }

  /**
   * Checks if the cloud itself can handle the help reply
   *
   * @param source       the source of the command
   * @param currentChain the current chain of entered commands
   * @return whether the cloud can handle the input or not
   */
  protected boolean replyWithCommandHelp(
    @NotNull CommandSource source,
    @NotNull List<CommandArgument<?, ?>> currentChain
  ) {
    if (currentChain.isEmpty()) {
      // the command chain is empty, let the user handle the response
      return false;
    }
    String root = currentChain.get(0).getName();
    CommandInfo commandInfo = this.commandProvider.getCommand(root);
    if (commandInfo == null) {
      // we can't find a matching command, let the user handle the response
      return false;
    }
    // if the chain length is 1 we can just print usage for every sub command
    if (currentChain.size() == 1) {
      this.printDefaultUsage(source, commandInfo);
    } else {
      List<String> results = new ArrayList<>();
      // rebuild the input of the user
      String commandChain = currentChain.stream().map(CommandArgument::getName).collect(Collectors.joining(" "));
      // check if we can find any chain specific usages
      for (String usage : commandInfo.getUsage()) {
        if (usage.startsWith(commandChain)) {
          results.add("- " + usage);
        }
      }

      if (results.isEmpty()) {
        // no results found, just print the default usages
        this.printDefaultUsage(source, commandInfo);
      } else {
        // we have chain specific results
        source.sendMessage(results);
      }
    }

    return true;
  }

  /**
   * Formats and prints the command usage of the given CommandInfo
   *
   * @param source      the source to send the usages to
   * @param commandInfo the command to print the usage for
   */
  protected void printDefaultUsage(@NotNull CommandSource source, @NotNull CommandInfo commandInfo) {
    for (String usage : commandInfo.getUsage()) {
      source.sendMessage("- " + usage);
    }
  }
}
