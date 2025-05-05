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

package eu.cloudnetservice.modules.signs.impl.platform.minestom.event;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSign;
import lombok.NonNull;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import org.jetbrains.annotations.Nullable;

public class MinestomCloudSignInteractEvent implements CancellableEvent {

  private final Player player;
  private final PlatformSign<Player, String> sign;

  private boolean cancelled;
  private ServiceInfoSnapshot target;

  public MinestomCloudSignInteractEvent(@NonNull Player player, @NonNull PlatformSign<Player, String> sign) {
    this.player = player;
    this.sign = sign;
    this.target = sign.currentTarget();
  }

  public @NonNull Player player() {
    return this.player;
  }

  public @NonNull PlatformSign<Player, String> clickedSign() {
    return this.sign;
  }

  public @Nullable ServiceInfoSnapshot target() {
    return this.target;
  }

  public void target(@Nullable ServiceInfoSnapshot target) {
    this.target = target;
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
