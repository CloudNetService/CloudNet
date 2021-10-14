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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.config.JsonConfiguration;

@CommandPermission("cloudnet.command.config")
public class CommandConfig {

  //TODO: reload command and other node config stuff

  @CommandMethod("config reload")
  public void reloadConfigs(CommandSource source) {
    CloudNet.getInstance().setConfig(JsonConfiguration.loadFromFile());
    CloudNet.getInstance().getServiceTaskProvider().reload();
    CloudNet.getInstance().getGroupConfigurationProvider().reload();
    CloudNet.getInstance().getPermissionManagement().reload();
    source.sendMessage(LanguageManager.getMessage("command-reload-reload-config-success"));
  }


}

