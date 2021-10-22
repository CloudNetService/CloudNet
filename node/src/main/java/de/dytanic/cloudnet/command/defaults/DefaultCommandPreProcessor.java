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

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.preprocessor.CommandPreprocessingContext;
import cloud.commandframework.execution.preprocessor.CommandPreprocessor;
import cloud.commandframework.services.types.ConsumerService;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.event.command.CommandPreProcessEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class DefaultCommandPreProcessor implements CommandPreprocessor<CommandSource> {

  @Override
  public void accept(@NonNull CommandPreprocessingContext<CommandSource> context) {
    CommandContext<CommandSource> commandContext = context.getCommandContext();
    CommandSource source = context.getCommandContext().getSender();

    String firstArgument = commandContext.getRawInput().getFirst();

    CommandInfo commandInfo = CloudNet.getInstance().getCommandProvider()
      .getCommand(firstArgument);
    // if there is no command, the command was unregistered, ignore confirm as the command is not registered.
    if (commandInfo == null && !firstArgument.equalsIgnoreCase("confirm")) {
      ConsumerService.interrupt();
    }

    CommandPreProcessEvent preProcessEvent = CloudNet.getInstance().getEventManager()
      .callEvent(new CommandPreProcessEvent(commandContext.getRawInputJoined(), source));
    if (preProcessEvent.isCancelled()) {
      ConsumerService.interrupt();
    }
  }
}
