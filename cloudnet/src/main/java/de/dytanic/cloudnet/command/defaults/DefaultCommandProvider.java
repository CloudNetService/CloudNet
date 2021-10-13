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

package de.dytanic.cloudnet.command.defaults;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.exceptions.NoSuchCommandException;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.command.source.ConsoleCommandSource;
import de.dytanic.cloudnet.command.sub.CommandClear;
import de.dytanic.cloudnet.command.sub.CommandCopy;
import de.dytanic.cloudnet.command.sub.CommandCreate;
import de.dytanic.cloudnet.command.sub.CommandDebug;
import de.dytanic.cloudnet.command.sub.CommandExit;
import de.dytanic.cloudnet.command.sub.CommandGroups;
import de.dytanic.cloudnet.command.sub.CommandMe;
import de.dytanic.cloudnet.command.sub.CommandMigrate;
import de.dytanic.cloudnet.command.sub.CommandPermissions;
import de.dytanic.cloudnet.command.sub.CommandService;
import de.dytanic.cloudnet.command.sub.CommandServiceConfiguration;
import de.dytanic.cloudnet.command.sub.CommandTasks;
import de.dytanic.cloudnet.command.sub.CommandTemplate;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.event.command.CommandInvalidSyntaxEvent;
import de.dytanic.cloudnet.event.command.CommandNotFoundEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultCommandProvider implements CommandProvider {

  private final CommandManager<CommandSource> commandManager;
  private final AnnotationParser<CommandSource> annotationParser;
  private final CommandConfirmationManager<CommandSource> confirmationManager;
  private final Collection<CommandInfo> registeredCommands;

  public DefaultCommandProvider() {
    this.commandManager = new DefaultCommandManager();
    this.annotationParser = new AnnotationParser<>(this.commandManager, CommandSource.class,
      parameters -> SimpleCommandMeta.empty());
    this.confirmationManager = new CommandConfirmationManager<>(
      30L,
      TimeUnit.SECONDS,
      context -> context.getCommandContext().getSender().sendMessage("Confirmation required."),
      //TODO: message configurable
      sender -> sender.sendMessage("No requests.")  //TODO: message configurable
    );
    this.confirmationManager.registerConfirmationProcessor(this.commandManager);
    this.registeredCommands = new ArrayList<>();
    //TODO: maybe this should be moved too
    this.commandManager.registerCommandPreProcessor(new DefaultCommandPreProcessor());
    this.commandManager.registerCommandPostProcessor(new DefaultCommandPostProcessor());

    //TODO move this to another class
    this.register(new CommandTemplate());
    this.register(new CommandExit());
    this.register(new CommandServiceConfiguration());
    this.register(new CommandGroups());
    this.register(new CommandTasks());
    this.register(new CommandCreate());
    this.register(new CommandMe());
    this.register(new CommandService());
    this.register(new CommandPermissions());
    this.register(new CommandClear());
    this.register(new CommandDebug());
    this.register(new CommandCopy());
    this.register(new CommandMigrate());
  }

  @Override
  public @NotNull List<String> suggest(@NotNull CommandSource source, @NotNull String input) {
    return this.commandManager.suggest(source, input);
  }

  @Override
  public void execute(@NotNull CommandSource source, @NotNull String input) {
    try {
      //join the future to handle the occurring exceptions
      this.commandManager.executeCommand(source, input).join();
    } catch (CompletionException exception) {
      Throwable cause = exception.getCause();

      // cloud wraps the exception in the cause, if no cause is found we can't handle the exception
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
      }
    }
  }

  @Override
  public void register(@NotNull Object command) {
    Collection<CommandInfo> commandInfos = new ArrayList<>();
    for (Command<CommandSource> cloudCommand : this.annotationParser.parse(command)) {
      String permission = cloudCommand.getCommandPermission().toString();
      String description = cloudCommand.getCommandMeta().get(CommandMeta.DESCRIPTION).orElse("No description provided");
      //TODO: sort name and aliases
    }

    this.registeredCommands.addAll(commandInfos);
  }

  @Override
  public @Nullable CommandInfo getCommand(@NotNull String input) {
    return this.registeredCommands.stream()
      .filter(commandInfo -> Arrays.binarySearch(commandInfo.getNames(), input) >= 0)
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NotNull Collection<CommandInfo> getCommands() {
    return Collections.unmodifiableCollection(this.registeredCommands);
  }

  private void handleArgumentParseException(CommandSource source, ArgumentParseException exception) {
    source.sendMessage(exception.getMessage());
  }

  private void handleArgumentNotAvailableException(CommandSource source, ArgumentNotAvailableException exception) {
    source.sendMessage(exception.getMessage());
  }

  private void handleInvalidSyntaxException(CommandSource source, InvalidSyntaxException exception) {
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

  private void handleNoSuchCommandException(CommandSource source, NoSuchCommandException exception) {
    CommandNotFoundEvent notFoundEvent = CloudNet.getInstance().getEventManager().callEvent(
      new CommandNotFoundEvent(
        source,
        exception.getSuppliedCommand(),
        LanguageManager.getMessage("command-not-found")
      )
    );
    source.sendMessage(notFoundEvent.getResponse());
  }

  private void handleNoPermissionException(CommandSource source, NoPermissionException exception) {
    source.sendMessage(LanguageManager.getMessage("command-sub-no-permission"));
  }

  private void handleInvalidCommandSourceException(CommandSource source, InvalidCommandSenderException exception) {
    if (exception.getRequiredSender() == ConsoleCommandSource.class) {
      source.sendMessage(LanguageManager.getMessage("command-console-only"));
    } else {
      source.sendMessage(LanguageManager.getMessage("command-driver-only"));
    }
  }
}
