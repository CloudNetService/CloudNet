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

package eu.cloudnetservice.modules.signs.impl.platform.bukkit.event;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSign;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

public class BukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

  public static final HandlerList HANDLER_LIST = new HandlerList();

  private final PlatformSign<Player, String> clickedSign;

  private boolean cancelled;
  private ServiceInfoSnapshot target;

  public BukkitCloudSignInteractEvent(@NonNull Player who, @NonNull PlatformSign<Player, String> clickedSign) {
    super(who);
    this.clickedSign = clickedSign;
    this.target = clickedSign.currentTarget();
  }

  public @NonNull PlatformSign<Player, String> clickedSign() {
    return this.clickedSign;
  }

  public @Nullable ServiceInfoSnapshot target() {
    return this.target;
  }

  public void target(@Nullable ServiceInfoSnapshot target) {
    this.target = target;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }
}
