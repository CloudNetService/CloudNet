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

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;

public final class CommandExit extends CommandDefault {

  private static final long CONFIRMATION_INTERVAL = 10000;
  private long lastExecution = -1;

  public CommandExit() {
    super("exit", "shutdown", "stop");
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
    if (args.length != 0) {
      sender.sendMessage(LanguageManager.getMessage("command-exit-no-args"));
      return;
    }
    if (this.lastExecution == -1 || this.lastExecution + CONFIRMATION_INTERVAL < System.currentTimeMillis()) {
      sender.sendMessage(LanguageManager.getMessage("command-exit-confirm")
        .replace("%seconds%", String.valueOf(CONFIRMATION_INTERVAL / 1000)));
      this.lastExecution = System.currentTimeMillis();
      return;
    }

    this.getCloudNet().stop();
  }
}
