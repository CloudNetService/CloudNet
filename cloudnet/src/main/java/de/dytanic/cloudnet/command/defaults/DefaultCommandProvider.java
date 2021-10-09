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

import cloud.commandframework.CommandManager;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.internal.CommandRegistrationHandler;
import cloud.commandframework.meta.SimpleCommandMeta;
import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.command.source.CommandSource;
import java.util.concurrent.TimeUnit;

public class DefaultCommandProvider implements CommandProvider {

  private final CommandManager<CommandSource> commandManager;
  private final AnnotationParser<CommandSource> annotationParser;
  private final CommandConfirmationManager<CommandSource> confirmationManager;

  public DefaultCommandProvider() {
    this.commandManager = new DefaultCommandManager(CommandExecutionCoordinator.simpleCoordinator(),
      CommandRegistrationHandler.nullCommandRegistrationHandler());
    this.annotationParser = new AnnotationParser<>(this.commandManager, CommandSource.class,
      parameters -> SimpleCommandMeta.empty());
    this.confirmationManager = new CommandConfirmationManager<>(
      30L,
      TimeUnit.SECONDS,
      context -> context.getCommandContext().getSender().sendMessage("Confirmation required."),
      //TODO: message configurable
      sender -> sender.sendMessage("No requests.")  //TODO: message configurable
    );
  }

  @Override
  public CommandManager<CommandSource> commandManager() {
    return this.commandManager;
  }

  @Override
  public AnnotationParser<CommandSource> annotationParser() {
    return this.annotationParser;
  }

  @Override
  public CommandConfirmationManager<CommandSource> defaultCommandConfirmation() {
    return this.confirmationManager;
  }
}
