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

package eu.cloudnetservice.node.command.exception;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.compound.FlagArgument;
import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoSuchCommandException;
import eu.cloudnetservice.CaptionedCommandException;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.command.CommandInfo;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.event.command.CommandInvalidSyntaxEvent;
import eu.cloudnetservice.node.event.command.CommandNotFoundEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * This exception handler provides a default handling of exceptions that can occur on command execution.
 *
 * @since 4.0
 */
@Singleton
public class CommandExceptionHandler {

  private static final Logger LOGGER = LogManager.logger(CommandExceptionHandler.class);

  private final CommandProvider commandProvider;
  private final EventManager eventManager;

  @Inject
  public CommandExceptionHandler(@NonNull CommandProvider commandProvider, @NonNull EventManager eventManager) {
    this.commandProvider = commandProvider;
    this.eventManager = eventManager;
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
    if (cause instanceof CaptionedCommandException) {
      if (cause instanceof InvalidSyntaxException invalidSyntaxException) {
        // build the full command tree for the given input
        var commandTree = this.collectCommandHelp(invalidSyntaxException.getCurrentChain());
        // call the event to allow an own response
        var event = this.eventManager.callEvent(new CommandInvalidSyntaxEvent(
          source,
          invalidSyntaxException.getCorrectSyntax(),
          commandTree,
          // by default, we display the whole tree if we just have one argument
          invalidSyntaxException.getCurrentChain().size() <= 1
            ? commandTree
            : List.of(invalidSyntaxException.getMessage())));
        // send the response of the event
        source.sendMessage(event.response());
      } else if (cause instanceof NoSuchCommandException noSuchCommandException) {
        // call the command not found event for own responses
        var event = this.eventManager.callEvent(new CommandNotFoundEvent(
          source,
          noSuchCommandException.getSuppliedCommand(),
          cause.getMessage()));
        // send the response of the event
        source.sendMessage(event.response());
      } else {
        // just send the message
        source.sendMessage(cause.getMessage());
      }
    } else if (cause instanceof ArgumentParseException parseException) {
      var deepCause = cause.getCause();
      if (deepCause instanceof ArgumentNotAvailableException) {
        source.sendMessage(deepCause.getMessage());
      } else if (deepCause instanceof CaptionedCommandException) {
        // we need to handle this exception extra
        if (deepCause instanceof FlagArgument.FlagParseException flagParseException) {
          // if no flag is supplied we should reply with the command tree
          if (flagParseException.failureReason() == FlagArgument.FailureReason.NO_FLAG_STARTED) {
            source.sendMessage(this.collectCommandHelp(parseException.getCurrentChain()));
            return;
          }
        }
        source.sendMessage(deepCause.getMessage());
      } else {
        LOGGER.severe("Exception during command argument parsing", cause);
      }
    } else {
      LOGGER.severe("Exception during command execution", cause);
    }
  }

  /**
   * Collects the default command help for the given command chain.
   *
   * @param currentChain the current chain of entered commands.
   * @return a list containing the response for the source.
   * @throws NullPointerException if the current chain is null.
   */
  protected @NonNull List<String> collectCommandHelp(@NonNull List<CommandArgument<?, ?>> currentChain) {
    if (currentChain.isEmpty()) {
      // the command chain is empty, let the user handle the response
      return List.of();
    }
    var root = currentChain.get(0).getName();
    var commandInfo = this.commandProvider.command(root);
    if (commandInfo == null) {
      // we can't find a matching command, let the user handle the response
      return List.of();
    }
    // if the chain length is 1 we can just print usage for every sub command
    List<String> results = new ArrayList<>();
    if (currentChain.size() == 1) {
      this.collectDefaultUsage(results, commandInfo);
    } else {
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
        this.collectDefaultUsage(results, commandInfo);
      }
    }
    return results;
  }

  /**
   * Formats and collects the default command usage for the give command info.
   *
   * @param collector   the list to collect the messages to.
   * @param commandInfo the command to print the usage for.
   * @throws NullPointerException if collector or commandInfo is null.
   */
  protected void collectDefaultUsage(@NonNull List<String> collector, @NonNull CommandInfo commandInfo) {
    for (var usage : commandInfo.usage()) {
      collector.add("- " + usage);
    }
  }
}
