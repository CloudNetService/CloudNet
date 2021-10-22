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
import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext;
import cloud.commandframework.execution.postprocessor.CommandPostprocessor;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.event.command.CommandPostProcessEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

final class DefaultCommandPostProcessor implements CommandPostprocessor<CommandSource> {

  @Override
  public void accept(@NonNull CommandPostprocessingContext<CommandSource> context) {
    CommandContext<CommandSource> commandContext = context.getCommandContext();
    CommandSource source = commandContext.getSender();

    CloudNet.getInstance().getEventManager()
      .callEvent(new CommandPostProcessEvent(commandContext.getRawInputJoined(), source));
  }
}
