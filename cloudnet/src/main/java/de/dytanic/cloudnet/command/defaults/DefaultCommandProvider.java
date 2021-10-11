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
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.exceptions.NoSuchCommandException;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    this.registeredCommands = new ArrayList<>();
  }

  @Override
  public @NotNull List<String> suggest(@NotNull CommandSource source, @NotNull String input) {
    return this.commandManager.suggest(source, input);
  }

  @Override
  public void execute(@NotNull CommandSource source, @NotNull String input) {
    try {
      this.commandManager.executeCommand(source, input).join();
    } catch (Exception exception) {
      if (exception instanceof ArgumentNotAvailableException) {
        source.sendMessage(exception.getMessage());
      } else if (exception instanceof NoSuchCommandException) {
        source.sendMessage(LanguageManager.getMessage("command-not-found"));
      } else if (exception instanceof NoPermissionException) {
        source.sendMessage(LanguageManager.getMessage("command-sub-no-permission"));
      }
    }
  }

  @Override
  public @NotNull Collection<CommandInfo> register(@NotNull Object command) {
    Collection<CommandInfo> commandInfos = new ArrayList<>();
    for (Command<CommandSource> cloudCommand : this.annotationParser.parse(command)) {
      String permission = cloudCommand.getCommandPermission().toString();
      String description = cloudCommand.getCommandMeta().get(CommandMeta.DESCRIPTION).orElse("No description provided");
      //TODO: sort name and aliases
    }

    this.registeredCommands.addAll(commandInfos);

    return commandInfos;
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
}
