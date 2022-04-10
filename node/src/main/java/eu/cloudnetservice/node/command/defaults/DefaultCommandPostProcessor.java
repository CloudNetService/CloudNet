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

package eu.cloudnetservice.node.command.defaults;

import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext;
import cloud.commandframework.execution.postprocessor.CommandPostprocessor;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.event.command.CommandPostProcessEvent;
import lombok.NonNull;

/**
 * {@inheritDoc}
 */
final class DefaultCommandPostProcessor implements CommandPostprocessor<CommandSource> {

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(@NonNull CommandPostprocessingContext<CommandSource> context) {
    var commandContext = context.getCommandContext();
    var source = commandContext.getSender();

    Node.instance().eventManager()
      .callEvent(new CommandPostProcessEvent(commandContext.getRawInputJoined(), source));
  }
}
