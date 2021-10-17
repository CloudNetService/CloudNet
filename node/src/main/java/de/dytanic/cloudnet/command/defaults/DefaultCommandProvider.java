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
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta.Key;
import cloud.commandframework.meta.SimpleCommandMeta;
import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.CommandExceptionHandler;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.command.sub.CommandClear;
import de.dytanic.cloudnet.command.sub.CommandCopy;
import de.dytanic.cloudnet.command.sub.CommandCreate;
import de.dytanic.cloudnet.command.sub.CommandDebug;
import de.dytanic.cloudnet.command.sub.CommandExit;
import de.dytanic.cloudnet.command.sub.CommandGroups;
import de.dytanic.cloudnet.command.sub.CommandHelp;
import de.dytanic.cloudnet.command.sub.CommandMe;
import de.dytanic.cloudnet.command.sub.CommandMigrate;
import de.dytanic.cloudnet.command.sub.CommandPermissions;
import de.dytanic.cloudnet.command.sub.CommandService;
import de.dytanic.cloudnet.command.sub.CommandTasks;
import de.dytanic.cloudnet.command.sub.CommandTemplate;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import io.leangen.geantyref.TypeToken;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultCommandProvider implements CommandProvider {

  private static final Key<Collection<String>> ALIAS_KEY = Key.of(new TypeToken<Collection<String>>() {
  }, "alias");
  private static final Key<String> DESCRIPTION_KEY = Key.of(String.class, "cloudnet:description");

  private final CommandManager<CommandSource> commandManager;
  private final AnnotationParser<CommandSource> annotationParser;
  private final Collection<CommandInfo> registeredCommands;
  private final CommandExceptionHandler exceptionHandler;

  public DefaultCommandProvider() {
    this.commandManager = new DefaultCommandManager();
    this.annotationParser = new AnnotationParser<>(this.commandManager, CommandSource.class,
      parameters -> SimpleCommandMeta.empty());
    this.registeredCommands = new ArrayList<>();
    // handle our @CommandAlias annotation and apply the found aliases
    this.annotationParser.registerBuilderModifier(CommandAlias.class,
      (alias, builder) -> builder.meta(ALIAS_KEY, Arrays.asList(alias.value())));
    // handle our @Description annotation and apply the found description for the help command
    this.annotationParser.registerBuilderModifier(Description.class,
      (description, builder) -> builder.meta(DESCRIPTION_KEY, description.value()));
    // register pre- and post-processor to call our events
    this.commandManager.registerCommandPreProcessor(new DefaultCommandPreProcessor());
    this.commandManager.registerCommandPostProcessor(new DefaultCommandPostProcessor());
    // register the command confirmation handling
    this.registerCommandConfirmation();
    this.exceptionHandler = new CommandExceptionHandler(this);
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
      this.exceptionHandler.handleCompletionException(source, exception);
    }
  }

  @Override
  public void register(@NotNull Object command) {
    Iterator<Command<CommandSource>> cloudCommands = this.annotationParser.parse(command).iterator();
    // just get the first command of the object as we don't want to register each method
    if (cloudCommands.hasNext()) {
      Command<CommandSource> parsedCommand = cloudCommands.next();
      // check if there are any arguments, we don't want to register an empty command
      if (parsedCommand.getArguments().isEmpty()) {
        return;
      }

      String permission = parsedCommand.getCommandPermission().toString();
      String description = parsedCommand.getCommandMeta().getOrDefault(DESCRIPTION_KEY, "No description provided");

      // retrieve the aliases processed by the @CommandAlias annotation
      Collection<String> aliases = parsedCommand.getCommandMeta().getOrDefault(ALIAS_KEY, Collections.emptyList());

      // get the name by using the first argument of the command
      String name = parsedCommand.getArguments().get(0).getName();

      this.registeredCommands.add(
        new CommandInfo(name, aliases, permission, description, this.getCommandUsageByRoot(name)));
    }
  }

  @Override
  public void registerConsoleHandler(IConsole console) {
    console.addCommandHandler(UUID.randomUUID(), input -> {
      String trimmedInput = input.trim();
      if (trimmedInput.isEmpty()) {
        return;
      }
      this.execute(CommandSource.console(), trimmedInput);
    });
  }

  @Override
  public void registerDefaultCommands() {
    this.register(new CommandTemplate());
    this.register(new CommandExit());
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
    this.register(new CommandHelp(this));
  }

  @Override
  public @Nullable CommandInfo getCommand(@NotNull String input) {
    String lowerCaseInput = input.toLowerCase();
    return this.registeredCommands.stream()
      .filter(commandInfo -> commandInfo.getAliases().contains(lowerCaseInput) || commandInfo.getName()
        .equals(lowerCaseInput))
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NotNull Collection<CommandInfo> getCommands() {
    return Collections.unmodifiableCollection(this.registeredCommands);
  }

  private void registerCommandConfirmation() {
    // create a new confirmation manager
    CommandConfirmationManager<CommandSource> confirmationManager = new CommandConfirmationManager<>(
      30L,
      TimeUnit.SECONDS,
      context -> context.getCommandContext().getSender()
        .sendMessage(LanguageManager.getMessage("command-confirmation-required")),
      sender -> sender.sendMessage(LanguageManager.getMessage("command-confirmation-no-requests"))
    );
    // register the confirmation manager to the command manager
    confirmationManager.registerConfirmationProcessor(this.commandManager);
    // register the command that is used for confirmations
    this.commandManager.command(this.commandManager.commandBuilder("confirm")
      .handler(confirmationManager.createConfirmationExecutionHandler()));
  }

  private @NotNull List<String> getCommandUsageByRoot(@NotNull String root) {
    List<String> commandUsage = new ArrayList<>();
    for (Command<CommandSource> command : this.commandManager.getCommands()) {
      List<CommandArgument<CommandSource, ?>> arguments = command.getArguments();
      // the fist argument is the root, check if it matches
      if (arguments.isEmpty() || !arguments.get(0).getName().equalsIgnoreCase(root)) {
        continue;
      }

      commandUsage.add(this.commandManager.getCommandSyntaxFormatter().apply(arguments, null));
    }

    Collections.sort(commandUsage);
    return commandUsage;
  }

  @Internal
  public boolean replyWithCommandHelp(@NotNull CommandSource source,
    @NotNull List<CommandArgument<?, ?>> currentChain) {
    if (currentChain.isEmpty()) {
      // the command chain is empty, let the user handle the response
      return false;
    }
    String root = currentChain.get(0).getName();
    CommandInfo commandInfo = this.getCommand(root);
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
          results.add(" - " + usage);
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

  private void printDefaultUsage(@NotNull CommandSource source, @NotNull CommandInfo commandInfo) {
    for (String usage : commandInfo.getUsage()) {
      source.sendMessage(" - " + usage);
    }
  }
}
