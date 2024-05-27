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

package eu.cloudnetservice.node.command.defaults;

import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext;
import cloud.commandframework.execution.postprocessor.CommandPostprocessor;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.event.command.CommandPostProcessEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

/**
 * {@inheritDoc}
 */
@Singleton
final class DefaultCommandPostProcessor implements CommandPostprocessor<CommandSource> {

  private final CommandProvider provider;
  private final EventManager eventManager;

  @Inject
  private DefaultCommandPostProcessor(@NonNull CommandProvider provider, @NonNull EventManager eventManager) {
    this.provider = provider;
    this.eventManager = eventManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(@NonNull CommandPostprocessingContext<CommandSource> context) {
    var commandContext = context.getCommandContext();
    var source = commandContext.getSender();

    // we only process command executions and not the tab complete handling
    if (commandContext.isSuggestions()) {
      return;
    }

    // get the first argument and retrieve the command info using it
    var rawInput = commandContext.getRawInput();
    var firstArgument = rawInput.getFirst();
    var commandInfo = this.provider.command(firstArgument);

    // should not happen - just make sure
    if (commandInfo != null) {
      this.eventManager.callEvent(new CommandPostProcessEvent(rawInput, commandInfo, source, this.provider));
    }
  }
}
