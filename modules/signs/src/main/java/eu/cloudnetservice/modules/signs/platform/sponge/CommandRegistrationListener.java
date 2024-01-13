/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.platforminject.api.mapping.Container;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.platform.sponge.functionality.SignsCommand;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.plugin.PluginContainer;

final class CommandRegistrationListener {

  private final PluginContainer plugin;

  public CommandRegistrationListener(@NonNull Container<PluginContainer> platformData) {
    this.plugin = platformData.container();
    Sponge.eventManager().registerListeners(platformData.container(), this);
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
        .executor(new SignsCommand(() -> {
          var layer = InjectionLayer.findLayerOf(SignManagement.class);
          return layer.instance(SpongeSignManagement.class);
        }))
        .build(),
      "cloudsigns",
      "cs", "signs", "cloudsign");
  }
}
