/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.command.defaults;

import cloud.commandframework.CommandManager;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.common.StringUtil;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.command.CommandInfo;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.annotation.Documentation;
import eu.cloudnetservice.node.command.exception.CommandExceptionHandler;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.command.sub.ClearCommand;
import eu.cloudnetservice.node.command.sub.ClusterCommand;
import eu.cloudnetservice.node.command.sub.ConfigCommand;
import eu.cloudnetservice.node.command.sub.CreateCommand;
import eu.cloudnetservice.node.command.sub.DebugCommand;
import eu.cloudnetservice.node.command.sub.ExitCommand;
import eu.cloudnetservice.node.command.sub.GroupsCommand;
import eu.cloudnetservice.node.command.sub.HelpCommand;
import eu.cloudnetservice.node.command.sub.MeCommand;
import eu.cloudnetservice.node.command.sub.MigrateCommand;
import eu.cloudnetservice.node.command.sub.ModulesCommand;
import eu.cloudnetservice.node.command.sub.PermissionsCommand;
import eu.cloudnetservice.node.command.sub.ServiceCommand;
import eu.cloudnetservice.node.command.sub.TasksCommand;
import eu.cloudnetservice.node.command.sub.TemplateCommand;
import eu.cloudnetservice.node.command.sub.VersionCommand;
import eu.cloudnetservice.node.console.Console;
import eu.cloudnetservice.node.console.handler.ConsoleInputHandler;
import eu.cloudnetservice.node.console.handler.ConsoleTabCompleteHandler;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
@Singleton
@Provides(CommandProvider.class)
public final class DefaultCommandProvider implements CommandProvider {

  private static final CommandMeta.Key<Set<String>> ALIAS_KEY = CommandMeta.Key.of(new TypeToken<Set<String>>() {
  }, "cloudnet:alias");
  private static final CommandMeta.Key<String> DESCRIPTION_KEY = CommandMeta.Key.of(
    String.class,
    "cloudnet:description");
  private static final CommandMeta.Key<String> DOCUMENTATION_KEY = CommandMeta.Key.of(
    String.class,
    "cloudnet:documentation");

  private final CommandExceptionHandler exceptionHandler;
  private final CommandManager<CommandSource> commandManager;
  private final AnnotationParser<CommandSource> annotationParser;
  private final SetMultimap<ClassLoader, CommandInfo> registeredCommands;

  @Inject
  private DefaultCommandProvider(
    @NonNull DefaultCommandManager commandManager,
    @NonNull AerogelInjectionService injectionService,
    @NonNull CommandExceptionHandler exceptionHandler,
    @NonNull DefaultSuggestionProcessor suggestionProcessor,
    @NonNull DefaultCommandPreProcessor commandPreProcessor,
    @NonNull DefaultCommandPostProcessor commandPostProcessor,
    @NonNull DefaultCaptionVariableReplacementHandler captionVariableReplacementHandler
  ) {
    // init command manager and annotation parser
    this.commandManager = commandManager;
    this.commandManager.captionVariableReplacementHandler(captionVariableReplacementHandler);
    this.commandManager.parameterInjectorRegistry().registerInjectionService(injectionService);
    this.annotationParser = new AnnotationParser<>(
      this.commandManager,
      CommandSource.class,
      parameters -> SimpleCommandMeta.empty());

    // handle our @CommandAlias annotation and apply the found aliases
    this.annotationParser.registerBuilderModifier(
      CommandAlias.class,
      (alias, builder) -> builder.meta(ALIAS_KEY, new HashSet<>(Arrays.asList(alias.value()))));
    // handle our @Description annotation and apply the found description for the help command
    this.annotationParser.registerBuilderModifier(Description.class, (description, builder) -> {
      if (!description.value().trim().isEmpty()) {
        // check if we have to translate the value
        if (description.translatable()) {
          return builder.meta(DESCRIPTION_KEY, I18n.trans(description.value()));
        }
        // just the raw description
        return builder.meta(DESCRIPTION_KEY, description.value());
      }
      return builder;
    });
    // handle our @Documentation annotation
    this.annotationParser.registerBuilderModifier(Documentation.class, (documentation, builder) -> {
      if (!documentation.value().trim().isEmpty()) {
        return builder.meta(DOCUMENTATION_KEY, documentation.value());
      }
      return builder;
    });

    // register pre- and post-processor to call our events
    this.commandManager.commandSuggestionProcessor(suggestionProcessor);
    this.commandManager.registerCommandPreProcessor(commandPreProcessor);
    this.commandManager.registerCommandPostProcessor(commandPostProcessor);

    // internal handling
    this.exceptionHandler = exceptionHandler;
    this.registeredCommands = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);

    // register the command confirmation handling
    this.registerCommandConfirmation();
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
  public void register(@NonNull Class<?> commandClass) {
    var injectionLayer = InjectionLayer.findLayerOf(commandClass);
    this.register(injectionLayer.instance(commandClass));
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
      var description = cloudCommand.getCommandMeta().get(DESCRIPTION_KEY)
        .orElseGet(() -> I18n.trans("command-no-description"));
      // retrieve the aliases processed by the @CommandAlias annotation
      var aliases = cloudCommand.getCommandMeta().getOrDefault(ALIAS_KEY, Collections.emptySet());
      // retrieve the documentation url processed by the @Documentation annotation
      var documentation = cloudCommand.getCommandMeta().get(DOCUMENTATION_KEY).orElse(null);
      // get the name by using the first argument of the command
      var name = StringUtil.toLower(cloudCommand.getArguments().get(0).getName());
      // there is no other command registered with the given name, parse usage and register the command now
      this.registeredCommands.put(
        command.getClass().getClassLoader(),
        new CommandInfo(name, aliases, permission, description, documentation, this.commandUsageOfRoot(name)));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(@NonNull String name) {
    var commands = this.registeredCommands.entries();
    for (var entry : commands) {
      var commandInfo = entry.getValue();
      if (commandInfo.name().equals(name) || commandInfo.aliases().contains(name)) {
        // remove the command from the manager & from our registered entries
        this.commandManager.deleteRootCommand(commandInfo.name());
        this.registeredCommands.remove(entry.getKey(), entry.getValue());

        // stop here - there can only be one command with the name
        break;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(@NonNull ClassLoader classLoader) {
    var unregisteredCommands = this.registeredCommands.removeAll(classLoader);
    for (var unregisteredCommand : unregisteredCommands) {
      this.commandManager.deleteRootCommand(unregisteredCommand.name());
    }
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
    this.register(TemplateCommand.class);
    this.register(VersionCommand.class);
    this.register(ExitCommand.class);
    this.register(GroupsCommand.class);
    this.register(TasksCommand.class);
    this.register(CreateCommand.class);
    this.register(MeCommand.class);
    this.register(ServiceCommand.class);
    this.register(PermissionsCommand.class);
    this.register(ClearCommand.class);
    this.register(DebugCommand.class);
    this.register(MigrateCommand.class);
    this.register(ClusterCommand.class);
    this.register(ConfigCommand.class);
    this.register(ModulesCommand.class);
    this.register(HelpCommand.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable CommandInfo command(@NonNull String name) {
    var lowerCaseInput = StringUtil.toLower(name);
    for (var command : this.registeredCommands.values()) {
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
  private void registerCommandConfirmation() {
    // create a new confirmation manager
    var confirmationManager = new CommandConfirmationManager<CommandSource>(
      30L,
      TimeUnit.SECONDS,
      context -> context.getCommandContext().getSender().sendMessage(I18n.trans("command-confirmation-required")),
      sender -> sender.sendMessage(I18n.trans("command-confirmation-no-requests")));
    // register the confirmation manager to the command manager
    confirmationManager.registerConfirmationProcessor(this.commandManager);
    // register the command that is used for confirmations
    this.commandManager.command(this.commandManager.commandBuilder("confirm")
      .handler(confirmationManager.createConfirmationExecutionHandler()));
    this.registeredCommands.put(
      this.getClass().getClassLoader(),
      new CommandInfo(
        "confirm",
        Set.of(),
        "cloudnet.command.confirm",
        "Confirms command execution of certain commands",
        null,
        Collections.emptyList()));
  }

  /**
   * Parses the command usage by the given root command.
   *
   * @param root the command to parse the usage for.
   * @return the formatted and sorted usages for the command root.
   */
  private @NonNull List<String> commandUsageOfRoot(@NonNull String root) {
    List<String> commandUsage = new ArrayList<>();
    for (var command : this.commandManager.commands()) {
      // the first argument is the root, check if it matches
      var arguments = command.getArguments();
      if (arguments.isEmpty() || !arguments.get(0).getName().equalsIgnoreCase(root)) {
        continue;
      }

      commandUsage.add(this.commandManager.commandSyntaxFormatter().apply(arguments, null));
    }

    Collections.sort(commandUsage);
    return commandUsage;
  }
}
