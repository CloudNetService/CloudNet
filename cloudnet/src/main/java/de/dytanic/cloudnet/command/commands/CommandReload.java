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
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import java.util.Arrays;
import java.util.Collection;

public final class CommandReload extends CommandDefault implements ITabCompleter {

  public CommandReload() {
    super("reload", "rl", "rel");
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
    if (args.length == 0) {
      sender.sendMessage(
        "reload confirm",
        "reload config"
      );
    }

    if (args.length == 1) {
      if (args[0].equalsIgnoreCase("confirm") || args[0].equalsIgnoreCase("all")) {
        this.getCloudNet().reload();
        sender.sendMessage(LanguageManager.getMessage("command-reload-confirm-success"));
        return;
      }
      if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("conf")) {
        this.getCloudNet().getConfig().load();
        this.getCloudNet().getConfigurationRegistry().load();
        this.getCloudNet().getServiceTaskProvider().reload();
        this.getCloudNet().getGroupConfigurationProvider().reload();
        this.getCloudNet().getPermissionManagement().reload();
        sender.sendMessage(LanguageManager.getMessage("command-reload-reload-config-success"));
      }
    }
  }

  @Override
  public Collection<String> complete(String commandLine, String[] args, Properties properties) {
    if (args.length > 1) {
      return null;
    }
    return Arrays.asList("confirm", "config");
  }
}
