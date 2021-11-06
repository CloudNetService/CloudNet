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
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.CommandExceptionHandler;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.command.sub.CommandClear;
import de.dytanic.cloudnet.command.sub.CommandCluster;
import de.dytanic.cloudnet.command.sub.CommandConfig;
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
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.handler.ConsoleInputHandler;
import de.dytanic.cloudnet.console.handler.ConsoleTabCompleteHandler;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import io.leangen.geantyref.TypeToken;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of the {@link CommandProvider} for the cloud command framework.
 */
public class DefaultCommandProvider implements CommandProvider {

  private static final Key<Collection<String>> ALIAS_KEY = Key.of(new TypeToken<Collection<String>>() {
  }, "alias");
  private static final Key<String> DESCRIPTION_KEY = Key.of(String.class, "cloudnet:description");

  private final CommandManager<CommandSource> commandManager;
  private final AnnotationParser<CommandSource> annotationParser;
  private final SetMultimap<ClassLoader, CommandInfo> registeredCommands;
  private final CommandExceptionHandler exceptionHandler;
  private final IConsole console;

  public DefaultCommandProvider(IConsole console) {
    this.console = console;
    this.commandManager = new DefaultCommandManager();
    this.annotationParser = new AnnotationParser<>(this.commandManager, CommandSource.class,
      parameters -> SimpleCommandMeta.empty());
    this.registeredCommands = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    // handle our @CommandAlias annotation and apply the found aliases
    this.annotationParser.registerBuilderModifier(CommandAlias.class,
      (alias, builder) -> builder.meta(ALIAS_KEY, new HashSet<>(Arrays.asList(alias.value()))));
    // handle our @Description annotation and apply the found description for the help command
    this.annotationParser.registerBuilderModifier(Description.class,
      (description, builder) -> {
        if (!description.value().trim().isEmpty()) {
          return builder.meta(DESCRIPTION_KEY, description.value());
        }
        return builder;
      });
    // register pre- and post-processor to call our events
    this.commandManager.registerCommandPreProcessor(new DefaultCommandPreProcessor());
    this.commandManager.registerCommandPostProcessor(new DefaultCommandPostProcessor());
    this.commandManager.setCommandSuggestionProcessor(new DefaultSuggestionProcessor(this));
    // register the command confirmation handling
    this.registerCommandConfirmation();
    this.exceptionHandler = new CommandExceptionHandler(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull List<String> suggest(@NotNull CommandSource source, @NotNull String input) {
    return this.commandManager.suggest(source, input);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NotNull CommandSource source, @NotNull String input) {
    this.commandManager.executeCommand(source, input)
      .whenComplete((result, throwable) -> this.exceptionHandler.handleCommandExceptions(source, throwable));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(@NotNull Object command) {
    Command<CommandSource> cloudCommand = Iterables.getFirst(this.annotationParser.parse(command), null);
    // just get the first command of the object as we don't want to register each method
    if (cloudCommand != null) {
      // check if there are any arguments, we don't want to register an empty command
      if (cloudCommand.getArguments().isEmpty()) {
        return;
      }

      String permission = cloudCommand.getCommandPermission().toString();
      // retrieve our own description processed by the @Description annotation
      String description = cloudCommand.getCommandMeta().getOrDefault(DESCRIPTION_KEY, "No description provided");
      // retrieve the aliases processed by the @CommandAlias annotation
      Collection<String> aliases = cloudCommand.getCommandMeta().getOrDefault(ALIAS_KEY, Collections.emptySet());
      // get the name by using the first argument of the command
      String name = cloudCommand.getArguments().get(0).getName();
      // there is no other command registered with the given name, parse usage and register the command now
      this.registeredCommands.put(cloudCommand.getClass().getClassLoader(),
        new CommandInfo(name, aliases, permission, description, this.getCommandUsageByRoot(name)));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(@NotNull ClassLoader classLoader) {
    this.registeredCommands.removeAll(classLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerConsoleHandler(@NotNull IConsole console) {
    // command handling
    console.addCommandHandler(UUID.randomUUID(), new ConsoleInputHandler() {
      @Override
      public void handleInput(@NotNull String line) {
        // check if the input line is empty
        String trimmedInput = line.trim();
        if (!trimmedInput.isEmpty()) {
          // execute the command
          DefaultCommandProvider.this.execute(CommandSource.console(), trimmedInput);
        }
      }
    });
    // tab complete handling
    console.addTabCompleteHandler(UUID.randomUUID(), new ConsoleTabCompleteHandler() {
      @Override
      public @NotNull Collection<String> completeInput(@NotNull String line) {
        return DefaultCommandProvider.this.commandManager.suggest(CommandSource.console(), line);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerDefaultCommands() {
    this.register(new CommandTemplate());
    this.register(new CommandExit());
    this.register(new CommandGroups());
    this.register(new CommandTasks(this.console));
    this.register(new CommandCreate());
    this.register(new CommandMe());
    this.register(new CommandService());
    this.register(new CommandPermissions());
    this.register(new CommandClear());
    this.register(new CommandDebug());
    this.register(new CommandMigrate());
    this.register(new CommandCluster());
    this.register(new CommandConfig());
    this.register(new CommandHelp(this));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable CommandInfo getCommand(@NotNull String name) {
    String lowerCaseInput = name.toLowerCase();
    return this.registeredCommands.values().stream()
      .filter(commandInfo ->
        commandInfo.getAliases().contains(lowerCaseInput) || commandInfo.getName().equals(lowerCaseInput))
      .findFirst()
      .orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<CommandInfo> getCommands() {
    return Collections.unmodifiableCollection(this.registeredCommands.values());
  }

  /**
   * Registers the default confirmation handling for commands, that need a confirmation before they are executed.
   */
  protected void registerCommandConfirmation() {
    // create a new confirmation manager
    CommandConfirmationManager<CommandSource> confirmationManager = new CommandConfirmationManager<>(
      30L,
      TimeUnit.SECONDS,
      context -> context.getCommandContext().getSender()
        .sendMessage(I18n.trans("command-confirmation-required")),
      sender -> sender.sendMessage(I18n.trans("command-confirmation-no-requests"))
    );
    // register the confirmation manager to the command manager
    confirmationManager.registerConfirmationProcessor(this.commandManager);
    // register the command that is used for confirmations
    this.commandManager.command(this.commandManager.commandBuilder("confirm")
      .handler(confirmationManager.createConfirmationExecutionHandler()));
  }

  /**
   * Parses the command usage by the given root command.
   *
   * @param root the command to parse the usage for.
   * @return the formatted and sorted usages for the command root.
   */
  protected @NotNull List<String> getCommandUsageByRoot(@NotNull String root) {
    List<String> commandUsage = new ArrayList<>();
    for (Command<CommandSource> command : this.commandManager.getCommands()) {
      List<CommandArgument<CommandSource, ?>> arguments = command.getArguments();
      // the first argument is the root, check if it matches
      if (arguments.isEmpty() || !arguments.get(0).getName().equalsIgnoreCase(root)) {
        continue;
      }

      commandUsage.add(this.commandManager.getCommandSyntaxFormatter().apply(arguments, null));
    }

    Collections.sort(commandUsage);
    return commandUsage;
  }
}
