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

package eu.cloudnetservice.modules.cloudperms;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.permission.PermissionUpdateGroupEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionUpdateUserEvent;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;

public final class PermissionsUpdateListener<P> {

  private final Executor syncTaskExecutor;
  private final Consumer<P> commandTreeUpdater;
  private final Function<P, UUID> uniqueIdLookup;
  private final Function<UUID, P> onlinePlayerLookup;
  private final PermissionManagement permissionManagement;
  private final Supplier<Collection<? extends P>> onlinePlayerSupplier;

  public PermissionsUpdateListener(
    @NonNull Executor syncTaskExecutor,
    @NonNull Consumer<P> commandTreeUpdater,
    @NonNull Function<P, UUID> uniqueIdLookup,
    @NonNull Function<UUID, P> onlinePlayerLookup,
    @NonNull PermissionManagement permissionManagement,
    @NonNull Supplier<Collection<? extends P>> onlinePlayerSupplier
  ) {
    this.syncTaskExecutor = syncTaskExecutor;
    this.commandTreeUpdater = commandTreeUpdater;
    this.uniqueIdLookup = uniqueIdLookup;
    this.onlinePlayerLookup = onlinePlayerLookup;
    this.permissionManagement = permissionManagement;
    this.onlinePlayerSupplier = onlinePlayerSupplier;
  }

  @EventListener
  public void handle(@NonNull PermissionUpdateUserEvent event) {
    this.syncTaskExecutor.execute(() -> {
      // get the player if online
      var player = this.onlinePlayerLookup.apply(event.permissionUser().uniqueId());
      if (player != null) {
        // update the command tree of the player
        this.commandTreeUpdater.accept(player);
      }
    });
  }

  @EventListener
  public void handle(@NonNull PermissionUpdateGroupEvent event) {
    this.syncTaskExecutor.execute(() -> {
      // find all matching players
      for (P player : this.onlinePlayerSupplier.get()) {
        var playerUniqueId = this.uniqueIdLookup.apply(player);
        // get the associated user
        var user = this.permissionManagement.user(playerUniqueId);
        if (user != null && user.inGroup(event.permissionGroup().name())) {
          // update the command tree of the player
          this.commandTreeUpdater.accept(player);
        }
      }
    });
  }
}
