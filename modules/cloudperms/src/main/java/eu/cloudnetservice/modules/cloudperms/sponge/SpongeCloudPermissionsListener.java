/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudperms.sponge;

import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.modules.cloudperms.CloudPermissionsHelper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.util.Tristate;

@Singleton
public final class SpongeCloudPermissionsListener {

  private final PermissionManagement permissionManagement;

  @Inject
  public SpongeCloudPermissionsListener(@NonNull PermissionManagement permissionManagement) {
    this.permissionManagement = permissionManagement;
  }

  @IsCancelled(Tristate.FALSE)
  @Listener(order = Order.LAST)
  public void handle(@NonNull ServerSideConnectionEvent.Login event, @NonNull @Root User user) {
    CloudPermissionsHelper.initPermissionUser(
      this.permissionManagement,
      user.uniqueId(),
      user.name(),
      message -> {
        event.setCancelled(true);
        event.setMessage(Component.text(message));
      },
      Sponge.server().isOnlineModeEnabled());
  }

  @Listener
  public void handle(@NonNull ServerSideConnectionEvent.Disconnect event, @NonNull @Root ServerPlayer player) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionManagement, player.uniqueId());
  }
}
