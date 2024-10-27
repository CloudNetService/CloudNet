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

package eu.cloudnetservice.node.command;

import eu.cloudnetservice.driver.command.CommandInfo;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The CommandProvider allows access to the command execution and handling of a node. All node commands must be
 * registered here in order to use them in the console or using the api, see {@link #execute(CommandSource, String)}.
 * <p>
 * Commands are registered on a per-class basis therefore every class should only contain one root command. With this
 * one "root command" is meant that all commands in one class must start with a prefix that all have in common. The
 * reason for this is that there is no other way to properly parse and register the commands than this one.
 * <p>
 * Note: Commands that have a requiredSender in their {@link org.incendo.cloud.annotations.Command} annotation set are
 * only executable using the console or the api.
 *
 * @see org.incendo.cloud.annotations.Command
 * @see CommandInfo
 * @since 4.0
 */
public interface CommandProvider {

  /**
   * Resolves the next correct command suggestions that would result in a successful command execution, while only
   * suggesting commands that the source is allowed to execute.
   *
   * @param source the commandSource for the suggestions. Mostly for permission checks.
   * @param input  the command chain that suggestions are needed for.
   * @return the suggestions for the current command chain.
   * @throws NullPointerException if separator or input is null.
   */
  @NonNull
  List<String> suggest(@NonNull CommandSource source, @NonNull String input);

  /**
   * Executes a command with the given command source and sends all responses to the given source.
   * <p>
   * Note: The command is executed asynchronously in a cached thread pool. If synchronous execution is necessary, then
   * you should consider blocking for the command execution.
   *
   * @param source the command source that is used to execute the command.
   * @param input  the commandline that is executed.
   * @return a task wrapping the command execution.
   * @throws NullPointerException if source or input is null.
   */
  @NonNull
  CompletableFuture<?> execute(@NonNull CommandSource source, @NonNull String input);

  /**
   * Registers a command on a per-class basis. All methods annotated with {@link org.incendo.cloud.annotations.Command}
   * are parsed into a command and only one common {@link CommandInfo}.
   * <p>
   * This method takes a class instead of an instance and creates the instance using our dependency injection framework.
   * Make sure that the given class supports the instantiation using dependency injection.
   *
   * @param commandClass the class to instantiate and register the commands for afterwards.
   * @throws NullPointerException if object is null.
   */
  void register(@NonNull Class<?> commandClass);

  /**
   * Registers a command on a per-class basis. All methods annotated with {@link org.incendo.cloud.annotations.Command}
   * are parsed into a command and only one common {@link CommandInfo}.
   *
   * @param command the instance of the class to register all commands for.
   * @throws NullPointerException if object is null.
   */
  void register(@NonNull Object command);

  /**
   * Unregisters the command with the given name or alias.
   *
   * @param name the name or alias of the command to unregister.
   * @throws NullPointerException if the given name is null.
   */
  void unregister(@NonNull String name);

  /**
   * Unregisters every command that was registered by the given classloader.
   *
   * @param classLoader the classloader that was used to register the command.
   * @throws NullPointerException if classLoader is null.
   */
  void unregister(@NonNull ClassLoader classLoader);

  /**
   * Looks for a registered command with the given root name or alias.
   *
   * @param name the command root name or an alias of the root
   * @return the command with the given name - null if no command was found with the given name / alias
   * @throws NullPointerException if name is null.
   */
  @Nullable CommandInfo command(@NonNull String name);

  /**
   * Collects all previously registered commands that are registered in a node and returns them in an unmodifiable
   * collection.
   *
   * @return all registered commands.
   */
  @UnmodifiableView
  @NonNull
  Collection<CommandInfo> commands();
}
