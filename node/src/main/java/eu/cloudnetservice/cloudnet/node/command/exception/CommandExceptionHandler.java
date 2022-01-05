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

package eu.cloudnetservice.cloudnet.node.command.exception;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.compound.FlagArgument;
import cloud.commandframework.arguments.compound.FlagArgument.FlagParseException;
import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.exceptions.NoSuchCommandException;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.command.CommandInfo;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.command.defaults.DefaultCommandProvider;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.command.source.ConsoleCommandSource;
import eu.cloudnetservice.cloudnet.node.event.command.CommandInvalidSyntaxEvent;
import eu.cloudnetservice.cloudnet.node.event.command.CommandNotFoundEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * This exception handler provides a default handling of exceptions that can occur on command execution.
 * <p>
 * Handled exceptions:
 * <ul>
 *   <li> {@link InvalidSyntaxException}
 *   <li> {@link NoPermissionException}
 *   <li> {@link NoSuchCommandException}
 *   <li> {@link InvalidCommandSenderException}
 *   <li> {@link ArgumentParseException}
 *   <li> {@link ArgumentNotAvailableException}
 *   <li> {@link FlagParseException}
 * </ul>
 *
 * @author Aldin S. (0utplay@cloudnetservice.eu)
 * @author Pasqual Koschmieder (derklaro@cloudnetservice.eu)
 * @since 4.0
 */
public class CommandExceptionHandler {

  protected static final Logger LOGGER = LogManager.logger(CommandExceptionHandler.class);

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
   * @throws NullPointerException if source is null.
   */
  public void handleCommandExceptions(@NonNull CommandSource source, @Nullable Throwable cause) {
    // there is no cause if no exception occurred
    if (cause == null) {
      return;
    }
    // the exception might be wrapped in a CompletionException
    if (cause instanceof CompletionException) {
      cause = cause.getCause();
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
      var deepCause = cause.getCause();
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

  protected void handleArgumentParseException(
    @NonNull CommandSource source,
    @NonNull ArgumentParseException exception
  ) {
    source.sendMessage(exception.getMessage());
  }

  protected void handleFlagParseException(
    @NonNull CommandSource source,
    @NonNull FlagParseException flagParseException
  ) {
    // we just ignore this as we can't really handle this due to cloud
  }

  protected void handleArgumentNotAvailableException(
    @NonNull CommandSource source,
    @NonNull ArgumentNotAvailableException exception
  ) {
    source.sendMessage(exception.getMessage());
  }

  protected void handleInvalidSyntaxException(
    @NonNull CommandSource source,
    @NonNull InvalidSyntaxException exception
  ) {
    if (this.replyWithCommandHelp(source, exception.getCurrentChain())) {
      return;
    }

    var invalidSyntaxEvent = CloudNet.instance().eventManager().callEvent(
      new CommandInvalidSyntaxEvent(
        source,
        exception.getCorrectSyntax(),
        I18n.trans("command-invalid-syntax")
          .replace("%syntax%", exception.getCorrectSyntax())
      )
    );
    source.sendMessage(invalidSyntaxEvent.response());
  }

  /**
   * Calls the {@link CommandNotFoundEvent} when the executed command is unknown and sends the resulting message of the
   * event to the command source.
   *
   * @param source    the source causing the exception.
   * @param exception the exception that needs to be handled.
   * @throws NullPointerException if source or exception is null.
   */
  protected void handleNoSuchCommandException(
    @NonNull CommandSource source,
    @NonNull NoSuchCommandException exception
  ) {
    var notFoundEvent = CloudNet.instance().eventManager().callEvent(
      new CommandNotFoundEvent(
        source,
        exception.getSuppliedCommand(),
        I18n.trans("command-not-found")
      )
    );
    source.sendMessage(notFoundEvent.response());
  }

  /**
   * Sends the command source a message regarding missing permissions.
   *
   * @param source    the source causing the exception.
   * @param exception the exception that needs to be handled.
   * @throws NullPointerException if source or exception is null.
   */
  protected void handleNoPermissionException(@NonNull CommandSource source, @NonNull NoPermissionException exception) {
    source.sendMessage(I18n.trans("command-sub-no-permission"));
  }

  /**
   * This notifies the {@link CommandSource} about the fact that the command is only allowed by the {@link
   * ConsoleCommandSource} if it is a {@link eu.cloudnetservice.cloudnet.node.command.source.DriverCommandSource} and
   * vice-versa.
   *
   * @param source    the source causing the exception.
   * @param exception the exception that needs to be handled.
   * @throws NullPointerException if source or exception is null.
   */
  protected void handleInvalidCommandSourceException(
    @NonNull CommandSource source,
    @NonNull InvalidCommandSenderException exception
  ) {
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
   * @throws NullPointerException if source or currentChain is null.
   */
  protected boolean replyWithCommandHelp(
    @NonNull CommandSource source,
    @NonNull List<CommandArgument<?, ?>> currentChain
  ) {
    if (currentChain.isEmpty()) {
      // the command chain is empty, let the user handle the response
      return false;
    }
    var root = currentChain.get(0).getName();
    var commandInfo = this.commandProvider.command(root);
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
      var commandChain = currentChain.stream().map(CommandArgument::getName).collect(Collectors.joining(" "));
      // check if we can find any chain specific usages
      for (var usage : commandInfo.usage()) {
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
   * @throws NullPointerException if source or commandInfo is null.
   */
  protected void printDefaultUsage(@NonNull CommandSource source, @NonNull CommandInfo commandInfo) {
    for (var usage : commandInfo.usage()) {
      source.sendMessage("- " + usage);
    }
  }
}
