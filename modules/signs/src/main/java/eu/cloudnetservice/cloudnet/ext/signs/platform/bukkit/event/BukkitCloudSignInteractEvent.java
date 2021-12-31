/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.ext.signs.platform.bukkit.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class BukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

  public static final HandlerList HANDLER_LIST = new HandlerList();

  private final Sign clickedSign;

  private boolean cancelled;
  private ServiceInfoSnapshot target;

  public BukkitCloudSignInteractEvent(Player who, Sign clickedSign, boolean cancelled) {
    super(who);
    this.clickedSign = clickedSign;
    this.cancelled = cancelled;
    this.target = clickedSign.currentTarget();
  }

  public Sign clickedSign() {
    return this.clickedSign;
  }

  public Optional<ServiceInfoSnapshot> target() {
    return Optional.ofNullable(this.target);
  }

  public void target(ServiceInfoSnapshot target) {
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
