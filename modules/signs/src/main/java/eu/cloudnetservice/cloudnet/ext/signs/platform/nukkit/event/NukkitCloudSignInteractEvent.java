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

package eu.cloudnetservice.cloudnet.ext.signs.platform.nukkit.event;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import java.util.Optional;
import lombok.NonNull;

public class NukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  protected final Sign sign;
  protected ServiceInfoSnapshot target;

  public NukkitCloudSignInteractEvent(@NonNull Player who, @NonNull Sign sign, boolean cancelled) {
    this.player = who;
    this.sign = sign;
    this.target = sign.currentTarget();
    this.setCancelled(cancelled);
  }

  public static HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  public Sign sign() {
    return this.sign;
  }

  public Optional<ServiceInfoSnapshot> target() {
    return Optional.ofNullable(this.target);
  }
}
