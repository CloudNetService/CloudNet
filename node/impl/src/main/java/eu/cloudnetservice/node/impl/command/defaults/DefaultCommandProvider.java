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

package eu.cloudnetservice.node.impl.command.defaults;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.command.CommandInfo;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.annotation.Documentation;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.impl.command.exception.CommandExceptionHandler;
import eu.cloudnetservice.node.impl.command.sub.ClearCommand;
import eu.cloudnetservice.node.impl.command.sub.ClusterCommand;
import eu.cloudnetservice.node.impl.command.sub.ConfigCommand;
import eu.cloudnetservice.node.impl.command.sub.CreateCommand;
import eu.cloudnetservice.node.impl.command.sub.DevCommand;
import eu.cloudnetservice.node.impl.command.sub.ExitCommand;
import eu.cloudnetservice.node.impl.command.sub.GroupsCommand;
import eu.cloudnetservice.node.impl.command.sub.HelpCommand;
import eu.cloudnetservice.node.impl.command.sub.MeCommand;
import eu.cloudnetservice.node.impl.command.sub.MigrateCommand;
import eu.cloudnetservice.node.impl.command.sub.ModulesCommand;
import eu.cloudnetservice.node.impl.command.sub.ServiceCommand;
import eu.cloudnetservice.node.impl.command.sub.TasksCommand;
import eu.cloudnetservice.node.impl.command.sub.TemplateCommand;
import eu.cloudnetservice.node.impl.command.sub.VersionCommand;
import eu.cloudnetservice.node.impl.console.Console;
import eu.cloudnetservice.node.impl.console.handler.ConsoleInputHandler;
import eu.cloudnetservice.node.impl.console.handler.ConsoleTabCompleteHandler;
import eu.cloudnetservice.utils.base.StringUtil;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.meta.CommandMeta;
import org.incendo.cloud.processors.cache.CaffeineCache;
import org.incendo.cloud.processors.confirmation.ConfirmationConfiguration;
import org.incendo.cloud.processors.confirmation.ConfirmationManager;
import org.incendo.cloud.processors.confirmation.annotation.ConfirmationBuilderModifier;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.Nullable;

/**
 * {@inheritDoc}
 */
@Singleton
@Provides(CommandProvider.class)
public final class DefaultCommandProvider implements CommandProvider {

  private static final CloudKey<Set<String>> ALIAS_KEY = CloudKey.of("cloudnet:alias", new TypeToken<Set<String>>() {
  });
  private static final CloudKey<String> DESCRIPTION_KEY = CloudKey.of("cloudnet:description", String.class);
  private static final CloudKey<String> DOCUMENTATION_KEY = CloudKey.of("cloudnet:documentation", String.class);

  private final I18n i18n;
  private final CommandExceptionHandler exceptionHandler;
  private final CommandManager<CommandSource> commandManager;
  private final AnnotationParser<CommandSource> annotationParser;
  private final SetMultimap<ClassLoader, CommandInfo> registeredCommands;

  @Inject
  private DefaultCommandProvider(
    @NonNull @Service I18n i18n,
    @NonNull DefaultCommandManager commandManager,
    @NonNull AerogelInjectionService injectionService,
    @NonNull CommandExceptionHandler exceptionHandler,
    @NonNull DefaultSuggestionProcessor suggestionProcessor,
    @NonNull DefaultCommandPreProcessor commandPreProcessor,
    @NonNull DefaultCommandPostProcessor commandPostProcessor,
    @NonNull DefaultCaptionHandler captionHandler
  ) {
    this.i18n = i18n;
    this.commandManager = commandManager;
    this.commandManager.captionRegistry().registerProvider(captionHandler);
    this.commandManager.captionFormatter(captionHandler);
    this.commandManager.parameterInjectorRegistry().registerInjectionService(injectionService);
    this.annotationParser = new AnnotationParser<>(
      this.commandManager,
      CommandSource.class,
      _ -> CommandMeta.empty());

    // handle our @CommandAlias annotation and apply the found aliases
    this.annotationParser.registerBuilderModifier(
      CommandAlias.class,
      (alias, builder) -> builder.meta(ALIAS_KEY, new HashSet<>(Arrays.asList(alias.value()))));
    // handle our @Description annotation and apply the found description for the help command
    this.annotationParser.registerBuilderModifier(Description.class, (description, builder) -> {
      if (!description.value().trim().isEmpty()) {
        // check if we have to translate the value
        if (description.translatable()) {
          return builder.meta(DESCRIPTION_KEY, this.i18n.translate(description.value()));
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
    this.commandManager.suggestionProcessor(suggestionProcessor);
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
    return this.commandManager.suggestionFactory().suggest(source, input).join().list().stream()
      .map(Suggestion::suggestion)
      .toList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<?> execute(@NonNull CommandSource source, @NonNull String input) {
    return this.commandManager.commandExecutor().executeCommand(source, input).exceptionally(exception -> {
      this.exceptionHandler.handleCommandExceptions(source, exception);
      // ensure that the new future still holds the exception
      throw exception instanceof CompletionException cex ? cex : new CompletionException(exception);
    });
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
      if (cloudCommand.nonFlagArguments().isEmpty()) {
        return;
      }

      var permission = cloudCommand.commandPermission().permissionString();
      // retrieve our own description processed by the @Description annotation
      var description = cloudCommand.commandMeta().getOrSupplyDefault(
        DESCRIPTION_KEY,
        () -> this.i18n.translate("command-no-description"));
      // retrieve the aliases processed by the @CommandAlias annotation
      var aliases = cloudCommand.commandMeta().getOrDefault(ALIAS_KEY, Collections.emptySet());
      // retrieve the documentation url processed by the @Documentation annotation
      var documentation = cloudCommand.commandMeta().getOrDefault(DOCUMENTATION_KEY, null);
      // get the name by using the first argument of the command
      var name = StringUtil.toLower(cloudCommand.nonFlagArguments().getFirst().name());
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
        return DefaultCommandProvider.this.commandManager.suggestionFactory().suggest(CommandSource.console(), line)
          .join()
          .list()
          .stream()
          .map(Suggestion::suggestion)
          .toList();
      }
    });
  }

  public void registerDefaultCommands() {
    this.register(TemplateCommand.class);
    this.register(VersionCommand.class);
    this.register(ExitCommand.class);
    this.register(GroupsCommand.class);
    this.register(TasksCommand.class);
    this.register(CreateCommand.class);
    this.register(MeCommand.class);
    this.register(ServiceCommand.class);
    this.register(ClearCommand.class);
    this.register(DevCommand.class);
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
    ConfirmationBuilderModifier.install(this.annotationParser);
    var configuration = ConfirmationConfiguration.<CommandSource>builder()
      .cache(CaffeineCache.of(Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build()))
      .noPendingCommandNotifier(sender -> sender.sendMessage(this.i18n.translate("command-confirmation-no-requests")))
      .confirmationRequiredNotifier(
        (sender, _) -> sender.sendMessage(this.i18n.translate("command-confirmation-required")))
      .build();
    var confirmationManager = ConfirmationManager.confirmationManager(configuration);
    this.commandManager.registerCommandPostProcessor(confirmationManager.createPostprocessor());
    // register the command that is used for confirmations
    this.commandManager.command(this.commandManager.commandBuilder("confirm")
      .handler(confirmationManager.createExecutionHandler()));

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
      var arguments = command.components();
      if (arguments.isEmpty() || !arguments.getFirst().name().equalsIgnoreCase(root)) {
        continue;
      }

      commandUsage.add(this.commandManager.commandSyntaxFormatter().apply(null, arguments, null));
    }

    Collections.sort(commandUsage);
    return commandUsage;
  }
}
