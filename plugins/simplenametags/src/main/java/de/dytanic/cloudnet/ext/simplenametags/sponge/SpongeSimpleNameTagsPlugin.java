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

package de.dytanic.cloudnet.ext.simplenametags.sponge;

import com.google.inject.Inject;
import de.dytanic.cloudnet.ext.simplenametags.SimpleNameTagsManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("simple_name_tags")
public final class SpongeSimpleNameTagsPlugin {

  private final SimpleNameTagsManager<ServerPlayer> nameTagsManager;

  @Inject
  public SpongeSimpleNameTagsPlugin(@NotNull PluginContainer pluginContainer) {
    this.nameTagsManager = new SpongeSimpleNameTagsManager(Sponge.server().scheduler().executor(pluginContainer));
  }

  @Listener
  public void handle(@NotNull ServerSideConnectionEvent.Join event, @Root @NotNull ServerPlayer player) {
    this.nameTagsManager.updateNameTagsFor(player);
  }
}
