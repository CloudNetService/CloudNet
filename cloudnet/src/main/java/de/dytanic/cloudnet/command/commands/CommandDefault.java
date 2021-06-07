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
import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.common.language.LanguageManager;

abstract class CommandDefault extends Command {

  protected CommandDefault(String... names) {
    this.names = names;
    this.prefix = "cloudnet";
    this.permission = "cloudnet.command." + names[0];
    this.description = LanguageManager.getMessage("command-description-" + names[0]);
    this.usage = names[0];
  }

  protected final CloudNet getCloudNet() {
    return CloudNet.getInstance();
  }
}
