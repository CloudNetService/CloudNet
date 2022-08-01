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

package eu.cloudnetservice.node.command.sub;

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Confirmation;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.source.ConsoleCommandSource;

@CommandAlias({"shutdown", "stop"})
@CommandPermission("cloudnet.command.exit")
@Description("command-exit-description")
public final class ExitCommand {

  @Confirmation
  @CommandMethod(value = "exit|shutdown|stop", requiredSender = ConsoleCommandSource.class)
  public void exit() {
    Node.instance().stop();
  }
}
