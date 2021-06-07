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

package de.dytanic.cloudnet.ext.signs.bukkit.event;

import de.dytanic.cloudnet.ext.signs.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

  private static final HandlerList HANDLER_LIST = new HandlerList();
  private final Sign clickedSign;
  private boolean cancelled;
  private String targetServer;

  public BukkitCloudSignInteractEvent(@NotNull Player who, @NotNull Sign clickedSign, @Nullable String targetServer) {
    super(who);
    this.clickedSign = clickedSign;
    this.targetServer = targetServer;
  }

  @NotNull
  public Sign getClickedSign() {
    return this.clickedSign;
  }

  @Nullable
  public String getTargetServer() {
    return this.targetServer;
  }

  public void setTargetServer(String targetServer) {
    this.targetServer = targetServer;
  }

  @NotNull
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
