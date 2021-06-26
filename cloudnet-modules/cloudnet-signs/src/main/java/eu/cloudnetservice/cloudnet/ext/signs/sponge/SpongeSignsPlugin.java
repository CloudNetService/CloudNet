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

package eu.cloudnetservice.cloudnet.ext.signs.sponge;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.service.AbstractServiceSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.service.SignsServiceListener;
import eu.cloudnetservice.cloudnet.ext.signs.sponge.functionality.CommandSigns;
import eu.cloudnetservice.cloudnet.ext.signs.sponge.functionality.SignInteractListener;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

@Plugin(
  id = "cloudnet_signs",
  name = "CloudNetSigns",
  version = "2.0",
  authors = "CloudNetService",
  url = "https://cloudnetservice.eu",
  description = "Sponge extension for the CloudNet runtime which adds sign connector support",
  dependencies = @Dependency(id = "cloudnet_bridge")
)
public class SpongeSignsPlugin {

  @Listener
  public void handleStart(GameStartedServerEvent event) {
    AbstractServiceSignManagement<Sign> signManagement = new SpongeSignManagement(this);
    signManagement.initialize();
    signManagement.registerToServiceRegistry();
    // command
    CommandSpec signsCommand = CommandSpec.builder()
      .description(Text.of("Management of the signs"))
      .permission("cloudnet.command.cloudsign")
      .arguments(
        GenericArguments.string(Text.of("Type")),
        GenericArguments.optional(GenericArguments.string(Text.of("Target Group"))),
        GenericArguments.optional(GenericArguments.string(Text.of("Template Path")))
      )
      .executor(new CommandSigns(signManagement))
      .build();
    Sponge.getCommandManager().register(this, signsCommand, "cloudsign", "cs");
    // sponge events
    Sponge.getEventManager().registerListeners(this, new SignInteractListener(this, signManagement));
    // cloudnet events
    CloudNetDriver.getInstance().getEventManager().registerListeners(
      new GlobalChannelMessageListener(signManagement), new SignsServiceListener(signManagement));
  }

  @Listener
  public void handleShutdown(GameStoppingServerEvent event) {
    SpongeSignManagement.getDefaultInstance().unregisterFromServiceRegistry();
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
  }
}
