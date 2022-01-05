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

import cloud.commandframework.execution.preprocessor.CommandPreprocessingContext;
import cloud.commandframework.execution.preprocessor.CommandPreprocessor;
import cloud.commandframework.services.types.ConsumerService;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.event.command.CommandPreProcessEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * {@inheritDoc}
 */
final class DefaultCommandPreProcessor implements CommandPreprocessor<CommandSource> {

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(@NonNull CommandPreprocessingContext<CommandSource> context) {
    var commandContext = context.getCommandContext();
    var source = context.getCommandContext().getSender();
    // we only process command executions and not the tab complete handling
    if (commandContext.isSuggestions()) {
      return;
    }

    var firstArgument = commandContext.getRawInput().getFirst();

    var commandInfo = CloudNet.instance().commandProvider()
      .command(firstArgument);
    // if there is no command, the command was unregistered, ignore confirm as the command is not registered.
    if (commandInfo == null && !firstArgument.equalsIgnoreCase("confirm")) {
      return;
    }

    var preProcessEvent = CloudNet.instance().eventManager()
      .callEvent(new CommandPreProcessEvent(commandContext.getRawInputJoined(), source));
    if (preProcessEvent.cancelled()) {
      ConsumerService.interrupt();
    }
  }
}
