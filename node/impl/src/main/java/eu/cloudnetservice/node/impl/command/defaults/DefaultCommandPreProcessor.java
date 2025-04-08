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

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.event.command.CommandPreProcessEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.incendo.cloud.execution.preprocessor.CommandPreprocessingContext;
import org.incendo.cloud.execution.preprocessor.CommandPreprocessor;
import org.incendo.cloud.services.type.ConsumerService;

/**
 * {@inheritDoc}
 */
@Singleton
final class DefaultCommandPreProcessor implements CommandPreprocessor<CommandSource> {

  private final CommandProvider provider;
  private final EventManager eventManager;

  @Inject
  private DefaultCommandPreProcessor(@NonNull CommandProvider provider, @NonNull EventManager eventManager) {
    this.provider = provider;
    this.eventManager = eventManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(@NonNull CommandPreprocessingContext<CommandSource> context) {
    var commandContext = context.commandContext();
    var source = commandContext.sender();

    // we only process command executions and not the tab complete handling
    if (commandContext.isSuggestions()) {
      return;
    }

    // get the first argument and retrieve the command info using it
    var rawInput = commandContext.rawInput();
    var firstArgument = rawInput.peekString();
    var commandInfo = this.provider.command(firstArgument);

    List<String> input = new ArrayList<>();
    while (rawInput.hasRemainingInput(true)) {
      input.add(rawInput.readString());
    }

    // should never happen - just make sure
    if (commandInfo != null) {
      var event = this.eventManager.callEvent(new CommandPreProcessEvent(input, commandInfo, source, this.provider));
      if (event.cancelled()) {
        ConsumerService.interrupt();
      }
    }
  }
}
