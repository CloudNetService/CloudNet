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

package eu.cloudnetservice.cloudnet.node.command.defaults;

import cloud.commandframework.CommandManager;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta.Key;
import cloud.commandframework.meta.SimpleCommandMeta;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.driver.command.CommandInfo;
import eu.cloudnetservice.cloudnet.node.command.CommandProvider;
import eu.cloudnetservice.cloudnet.node.command.annotation.CommandAlias;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.exception.CommandExceptionHandler;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandClear;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandCluster;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandConfig;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandCreate;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandDebug;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandExit;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandGroups;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandHelp;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandMe;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandMigrate;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandModules;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandPermissions;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandService;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandTasks;
import eu.cloudnetservice.cloudnet.node.command.sub.CommandTemplate;
import eu.cloudnetservice.cloudnet.node.console.Console;
import eu.cloudnetservice.cloudnet.node.console.handler.ConsoleInputHandler;
import eu.cloudnetservice.cloudnet.node.console.handler.ConsoleTabCompleteHandler;
import io.leangen.geantyref.TypeToken;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@inheritDoc}
 */
public class DefaultCommandProvider implements CommandProvider {

  private static final Key<Set<String>> ALIAS_KEY = Key.of(new TypeToken<Set<String>>() {
  }, "alias");
  private static final Key<String> DESCRIPTION_KEY = Key.of(String.class, "cloudnet:description");

  private final CommandManager<CommandSource> commandManager;
  private final AnnotationParser<CommandSource> annotationParser;
  private final SetMultimap<ClassLoader, CommandInfo> registeredCommands;
  private final CommandExceptionHandler exceptionHandler;
  private final Console console;

  /**
   * Constructs a new default implementation of the {@link CommandProvider}.
   *
   * @param console the console the provider is handling.
   * @throws NullPointerException if console is null.
   */
  public DefaultCommandProvider(@NonNull Console console) {
    this.console = console;
    this.commandManager = new DefaultCommandManager();
    this.annotationParser = new AnnotationParser<>(this.commandManager, CommandSource.class,
      parameters -> SimpleCommandMeta.empty());
    this.registeredCommands = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    // handle our @CommandAlias annotation and apply the found aliases
    this.annotationParser.registerBuilderModifier(CommandAlias.class,
      (alias, builder) -> builder.meta(ALIAS_KEY, new HashSet<>(Arrays.asList(alias.value()))));
    // handle our @Description annotation and apply the found description for the help command
    this.annotationParser.registerBuilderModifier(Description.class, (description, builder) -> {
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
  public @NonNull List<String> suggest(@NonNull CommandSource source, @NonNull String input) {
    return this.commandManager.suggest(source, input);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Task<?> execute(@NonNull CommandSource source, @NonNull String input) {
    return Task.wrapFuture(this.commandManager.executeCommand(source, input).exceptionally(exception -> {
      this.exceptionHandler.handleCommandExceptions(source, exception);
      // ensure that the new future still holds the exception
      throw exception instanceof CompletionException cex ? cex : new CompletionException(exception);
    }));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(@NonNull Object command) {
    var cloudCommand = Iterables.getFirst(this.annotationParser.parse(command), null);
    // just get the first command of the object as we don't want to register each method
    if (cloudCommand != null) {
      // check if there are any arguments, we don't want to register an empty command
      if (cloudCommand.getArguments().isEmpty()) {
        return;
      }

      var permission = cloudCommand.getCommandPermission().toString();
      // retrieve our own description processed by the @Description annotation
      var description = cloudCommand.getCommandMeta().getOrDefault(DESCRIPTION_KEY, "No description provided");
      // retrieve the aliases processed by the @CommandAlias annotation
      var aliases = cloudCommand.getCommandMeta().getOrDefault(ALIAS_KEY, Collections.emptySet());
      // get the name by using the first argument of the command
      var name = cloudCommand.getArguments().get(0).getName().toLowerCase();
      // there is no other command registered with the given name, parse usage and register the command now
      this.registeredCommands.put(cloudCommand.getClass().getClassLoader(),
        new CommandInfo(name, aliases, permission, description, this.commandUsageOfRoot(name)));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(@NonNull ClassLoader classLoader) {
    this.registeredCommands.removeAll(classLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerConsoleHandler(@NonNull Console console) {
    // command handling
    console.addCommandHandler(UUID.randomUUID(), new ConsoleInputHandler() {
      @Override
      public void handleInput(@NonNull String line) {
        // check if the input line is empty
        var trimmedInput = line.trim();
        if (!trimmedInput.isEmpty()) {
          // execute the command
          DefaultCommandProvider.this.execute(CommandSource.console(), trimmedInput);
        }
      }
    });
    // tab complete handling
    console.addTabCompleteHandler(UUID.randomUUID(), new ConsoleTabCompleteHandler() {
      @Override
      public @NonNull Collection<String> completeInput(@NonNull String line) {
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
    this.register(new CommandModules());
    this.register(new CommandHelp(this));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable CommandInfo command(@NonNull String name) {
    var lowerCaseInput = name.toLowerCase();

    for (CommandInfo command : this.registeredCommands.values()) {
      if (command.name().equals(lowerCaseInput) || command.aliases().contains(lowerCaseInput)) {
        return command;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<CommandInfo> commands() {
    return Collections.unmodifiableCollection(this.registeredCommands.values());
  }

  /**
   * Registers the default confirmation handling for commands, that need a confirmation before they are executed.
   */
  protected void registerCommandConfirmation() {
    // create a new confirmation manager
    var confirmationManager = new CommandConfirmationManager<CommandSource>(
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
    this.registeredCommands.put(this.getClass().getClassLoader(),
      new CommandInfo(
        "confirm",
        Set.of(),
        "cloudnet.command.confirm",
        "Confirms command execution of certain commands",
        Collections.emptyList()));
  }

  /**
   * Parses the command usage by the given root command.
   *
   * @param root the command to parse the usage for.
   * @return the formatted and sorted usages for the command root.
   */
  protected @NonNull List<String> commandUsageOfRoot(@NonNull String root) {
    List<String> commandUsage = new ArrayList<>();
    for (var command : this.commandManager.getCommands()) {
      var arguments = command.getArguments();
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
