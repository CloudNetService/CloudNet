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

package eu.cloudnetservice.modules.signs.impl.platform.nukkit.event;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSign;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class NukkitCloudSignInteractEvent extends PlayerEvent implements Cancellable {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  protected final PlatformSign<Player, String> sign;
  protected ServiceInfoSnapshot target;

  public NukkitCloudSignInteractEvent(@NonNull Player who, @NonNull PlatformSign<Player, String> sign) {
    this.player = who;
    this.sign = sign;
    this.target = sign.currentTarget();
  }

  public static HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  public @NonNull PlatformSign<Player, String> sign() {
    return this.sign;
  }

  public @Nullable ServiceInfoSnapshot target() {
    return this.target;
  }

  public void target(@Nullable ServiceInfoSnapshot target) {
    this.target = target;
  }
}
