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

package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.logging.LogLevel;

public class CommandDebug extends CommandDefault {

  public CommandDebug() {
    super("debug");
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
    if (CloudNet.getInstance().getLogger().getLevel() == LogLevel.DEBUG.getLevel()) {
      CloudNet.getInstance().setGlobalLogLevel(CloudNet.getInstance().getDefaultLogLevel());
    } else {
      CloudNet.getInstance().setGlobalLogLevel(LogLevel.DEBUG);
    }
  }

}
