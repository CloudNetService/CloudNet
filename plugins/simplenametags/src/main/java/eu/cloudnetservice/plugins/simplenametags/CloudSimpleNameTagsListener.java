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

package eu.cloudnetservice.plugins.simplenametags;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.permission.PermissionUpdateGroupEvent;
import eu.cloudnetservice.driver.event.events.permission.PermissionUpdateUserEvent;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import java.util.concurrent.Executor;
import lombok.NonNull;

public final class CloudSimpleNameTagsListener<P> {

  private final Executor syncTaskExecutor;
  private final SimpleNameTagsManager<P> nameTagsManager;
  private final PermissionManagement permissionManagement;

  public CloudSimpleNameTagsListener(
    @NonNull Executor syncTaskExecutor,
    @NonNull SimpleNameTagsManager<P> nameTagsManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.syncTaskExecutor = syncTaskExecutor;
    this.nameTagsManager = nameTagsManager;
    this.permissionManagement = permissionManagement;
  }

  @EventListener
  public void handle(@NonNull PermissionUpdateUserEvent event) {
    this.syncTaskExecutor.execute(() -> {
      // get the player if online
      var player = this.nameTagsManager.onlinePlayer(event.permissionUser().uniqueId());
      if (player != null) {
        // update the name tag of the player
        this.nameTagsManager.updateNameTagsFor(player);
      }
    });
  }

  @EventListener
  public void handle(@NonNull PermissionUpdateGroupEvent event) {
    this.syncTaskExecutor.execute(() -> {
      // find all matching players
      for (P player : this.nameTagsManager.onlinePlayers()) {
        var playerUniqueId = this.nameTagsManager.playerUniqueId(player);
        // get the associated user
        var user = this.permissionManagement.user(playerUniqueId);
        if (user != null && user.inGroup(event.permissionGroup().name())) {
          this.nameTagsManager.updateNameTagsFor(player);
        }
      }
    });
  }
}
