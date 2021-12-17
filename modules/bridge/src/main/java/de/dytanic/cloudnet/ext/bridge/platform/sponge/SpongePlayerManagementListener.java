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

package de.dytanic.cloudnet.ext.bridge.platform.sponge;

import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.platform.helper.ServerPlatformHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.plugin.PluginContainer;

public final class SpongePlayerManagementListener {

  private final TaskExecutorService executorService;
  private final PlatformBridgeManagement<?, ?> management;

  public SpongePlayerManagementListener(
    @NotNull PluginContainer plugin,
    @NotNull PlatformBridgeManagement<?, ?> management
  ) {
    this.management = management;
    this.executorService = Sponge.server().scheduler().executor(plugin);
  }

  @Listener
  public void handle(@NotNull ServerSideConnectionEvent.Login event, @First @NotNull User user) {
    var task = this.management.selfTask();
    // check if the current task is present
    if (task != null) {
      // check if maintenance is activated
      if (task.maintenance() && !user.hasPermission("cloudnet.bridge.maintenance")) {
        event.setCancelled(true);
        event.setMessage(Component.text(this.management.configuration().message(
          Locale.ENGLISH,
          "server-join-cancel-because-maintenance")));
        return;
      }
      // check if a custom permission is required to join
      var permission = task.properties().getString("requiredPermission");
      if (permission != null && !user.hasPermission(permission)) {
        event.setCancelled(true);
        event.setMessage(Component.text(this.management.configuration().message(
          Locale.ENGLISH,
          "server-join-cancel-because-permission")));
      }
    }
  }

  @Listener
  public void handle(@NotNull ServerSideConnectionEvent.Join event, @First @NotNull ServerPlayer player) {
    ServerPlatformHelper.sendChannelMessageLoginSuccess(
      player.uniqueId(),
      this.management.ownNetworkServiceInfo());
    // update service info
    this.executorService.schedule(() -> Wrapper.getInstance().publishServiceInfoUpdate(), 50, TimeUnit.MILLISECONDS);
  }

  @Listener
  public void handle(@NotNull ServerSideConnectionEvent.Disconnect event, @First @NotNull ServerPlayer player) {
    ServerPlatformHelper.sendChannelMessageDisconnected(
      player.uniqueId(),
      this.management.ownNetworkServiceInfo());
    // update service info
    this.executorService.schedule(() -> Wrapper.getInstance().publishServiceInfoUpdate(), 50, TimeUnit.MILLISECONDS);
  }
}
