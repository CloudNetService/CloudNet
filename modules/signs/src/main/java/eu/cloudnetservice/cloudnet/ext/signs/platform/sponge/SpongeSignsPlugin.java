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

package eu.cloudnetservice.cloudnet.ext.signs.platform.sponge;

import com.google.inject.Inject;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.platform.AbstractPlatformSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.platform.SignsPlatformListener;
import eu.cloudnetservice.cloudnet.ext.signs.platform.sponge.functionality.CommandSigns;
import eu.cloudnetservice.cloudnet.ext.signs.platform.sponge.functionality.SignInteractListener;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("cloudnet_signs")
public class SpongeSignsPlugin {

  private final PluginContainer plugin;
  private AbstractPlatformSignManagement<Sign> signManagement;

  @Inject
  public SpongeSignsPlugin(@NonNull PluginContainer plugin) {
    this.plugin = plugin;
  }

  @Listener
  public void handleStart(@NonNull ConstructPluginEvent event) {
    this.signManagement = new SpongeSignManagement(this.plugin);
    this.signManagement.initialize();
    this.signManagement.registerToServiceRegistry();
    // sponge events
    Sponge.eventManager().registerListeners(this.plugin, new SignInteractListener(this.plugin, this.signManagement));
    // cloudnet events
    CloudNetDriver.instance().eventManager().registerListeners(
      new GlobalChannelMessageListener(this.signManagement),
      new SignsPlatformListener(this.signManagement));
  }

  @Listener
  public void handleShutdown(@NonNull StoppingEngineEvent<Server> event) {
    SpongeSignManagement.defaultInstance().unregisterFromServiceRegistry();
    CloudNetDriver.instance().eventManager().unregisterListeners(this.getClass().getClassLoader());
  }

  @Listener
  public void handleCommandRegister(@NonNull RegisterCommandEvent<Command.Parameterized> event) {
    event.register(
      this.plugin,
      Command.builder()
        .shortDescription(Component.text("Management of the signs"))
        .permission("cloudnet.command.cloudsign")
        .addParameters(
          Parameter.string().key(CommandSigns.ACTION).build(),
          Parameter.string().key(CommandSigns.TARGET_GROUP).optional().build(),
          Parameter.string().key(CommandSigns.TARGET_TEMPLATE).optional().build()
        )
        .executor(new CommandSigns(this.signManagement))
        .build(),
      "cloudsigns",
      "cs", "signs", "cloudsign");
  }
}
