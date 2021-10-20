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

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.specifier.Greedy;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.Collection;

@CommandAlias("scr")
@CommandPermission("cloudnet.command.screen")
@Description("Toggles the automatic output of console messages from a service")
public final class CommandScreen {

  @CommandMethod("screen|ser list|l")
  public void listEnabledScreens(CommandSource source) {

  }

  @CommandMethod("screen|ser disableAll")
  public void disableAll(CommandSource source) {

  }

  @CommandMethod("screen|ser toggle|t <service>")
  public void toggleScreen(CommandSource source, @Argument("service") ServiceInfoSnapshot serviceInfoSnapshot) {

  }

  @CommandMethod("screen|ser write|w <command>")
  public void writeScreen(CommandSource source, @Argument("command") @Greedy String command) {

  }

  private Collection<ICloudService> allServices() {
    return CloudNet.getInstance().getCloudServiceProvider().getLocalCloudServices();
  }

}
