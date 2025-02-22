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

package eu.cloudnetservice.modules.bridge.impl.platform.sponge;

import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.platform.helper.ServerPlatformHelper;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.plugin.PluginContainer;

@Singleton
public final class SpongePlayerManagementListener {

  private final ServiceInfoHolder serviceInfoHolder;
  private final TaskExecutorService executorService;
  private final ServerPlatformHelper serverPlatformHelper;
  private final PlatformBridgeManagement<ServerPlayer, NetworkPlayerServerInfo> management;

  @Inject
  public SpongePlayerManagementListener(
    @NonNull PluginContainer plugin,
    @NonNull @Named("sync") Scheduler scheduler,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull ServerPlatformHelper serverPlatformHelper,
    @NonNull SpongeBridgeManagement management
  ) {
    this.serviceInfoHolder = serviceInfoHolder;
    this.serverPlatformHelper = serverPlatformHelper;
    this.management = management;
    this.executorService = scheduler.executor(plugin);
  }

  @Listener
  public void handle(@NonNull ServerSideConnectionEvent.Login event, @First @NonNull User user) {
    var task = this.management.selfTask();
    // check if the current task is present
    if (task != null) {
      // check if maintenance is activated
      if (task.maintenance() && !user.hasPermission("cloudnet.bridge.maintenance")) {
        event.setCancelled(true);
        this.management.configuration().handleMessage(
          Locale.ENGLISH,
          "server-join-cancel-because-maintenance",
          ComponentFormats.BUNGEE_TO_ADVENTURE::convert,
          event::setMessage);
        return;
      }
      // check if a custom permission is required to join
      var permission = task.propertyHolder().getString("requiredPermission");
      if (permission != null && !user.hasPermission(permission)) {
        event.setCancelled(true);
        this.management.configuration().handleMessage(
          Locale.ENGLISH,
          "server-join-cancel-because-permission",
          ComponentFormats.BUNGEE_TO_ADVENTURE::convert,
          event::setMessage);
      }
    }
  }

  @Listener
  public void handle(@NonNull ServerSideConnectionEvent.Join event, @First @NonNull ServerPlayer player) {
    this.serverPlatformHelper.sendChannelMessageLoginSuccess(
      player.uniqueId(),
      this.management.createPlayerInformation(player));
    // update service info
    this.executorService.schedule(() -> this.serviceInfoHolder.publishServiceInfoUpdate(), 50, TimeUnit.MILLISECONDS);
  }

  @Listener
  public void handle(@NonNull ServerSideConnectionEvent.Disconnect event, @First @NonNull ServerPlayer player) {
    this.serverPlatformHelper.sendChannelMessageDisconnected(
      player.uniqueId(),
      this.management.ownNetworkServiceInfo());
    // update service info
    this.executorService.schedule(() -> this.serviceInfoHolder.publishServiceInfoUpdate(), 50, TimeUnit.MILLISECONDS);
  }
}
