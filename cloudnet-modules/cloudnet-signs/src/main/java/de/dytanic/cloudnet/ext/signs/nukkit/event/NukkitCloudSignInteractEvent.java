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

package de.dytanic.cloudnet.ext.signs.nukkit.event;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import de.dytanic.cloudnet.ext.signs.Sign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  private final Sign clickedSign;

  private String targetServer;

  public NukkitCloudSignInteractEvent(@NotNull Player who, @NotNull Sign clickedSign, @Nullable String targetServer) {
    super.player = who;
    this.clickedSign = clickedSign;
    this.targetServer = targetServer;
  }

  public static HandlerList getHandlers() {
    return HANDLER_LIST;
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

}
