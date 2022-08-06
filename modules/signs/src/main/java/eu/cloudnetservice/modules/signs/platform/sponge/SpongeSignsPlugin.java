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

package eu.cloudnetservice.modules.signs.platform.sponge;

import com.google.inject.Inject;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.signs.SharedChannelMessageListener;
import eu.cloudnetservice.modules.signs.platform.PlatformSignManagement;
import eu.cloudnetservice.modules.signs.platform.SignsPlatformListener;
import eu.cloudnetservice.modules.signs.platform.sponge.functionality.SignInteractListener;
import eu.cloudnetservice.modules.signs.platform.sponge.functionality.SignsCommand;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("cloudnet_signs")
public class SpongeSignsPlugin {

  private final PluginContainer plugin;
  private PlatformSignManagement<ServerPlayer, ServerLocation, Component> signManagement;

  @Inject
  public SpongeSignsPlugin(@NonNull PluginContainer plugin) {
    this.plugin = plugin;
  }

  @Listener
  public void handleStart(@NonNull StartedEngineEvent<Server> event) {
    this.signManagement = SpongeSignManagement.newInstance(this.plugin);
    this.signManagement.initialize();
    this.signManagement.registerToServiceRegistry();
    // sponge events
    Sponge.eventManager().registerListeners(this.plugin, new SignInteractListener(this.plugin, this.signManagement));
    // cloudnet events
    CloudNetDriver.instance().eventManager().registerListeners(
      new SharedChannelMessageListener(this.signManagement),
      new SignsPlatformListener(this.signManagement));
  }

  @Listener
  public void handleShutdown(@NonNull StoppingEngineEvent<Server> event) {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }

  @Listener
  public void handleCommandRegister(@NonNull RegisterCommandEvent<Command.Parameterized> event) {
    event.register(
      this.plugin,
      Command.builder()
        .shortDescription(Component.text("Management of the signs"))
        .permission("cloudnet.command.cloudsign")
        .addParameters(
          Parameter.string().key(SignsCommand.ACTION).build(),
          Parameter.string().key(SignsCommand.TARGET_GROUP).optional().build(),
          Parameter.string().key(SignsCommand.TARGET_TEMPLATE).optional().build()
        )
        .executor(new SignsCommand(() -> this.signManagement))
        .build(),
      "cloudsigns",
      "cs", "signs", "cloudsign");
  }
}
