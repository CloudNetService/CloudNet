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

package eu.cloudnetservice.modules.signs.platform.sponge.event;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.signs.Sign;
import java.util.Optional;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.Event;

public class SpongeCloudSignInteractEvent implements Event, Cancellable {

  protected final Cause cause;
  protected final ServerPlayer player;

  protected final Sign sign;

  protected boolean cancelled;
  protected ServiceInfoSnapshot target;

  public SpongeCloudSignInteractEvent(Cause cause, ServerPlayer player, Sign sign, boolean cancelled) {
    this.cause = cause;
    this.player = player;
    this.sign = sign;
    this.target = sign.currentTarget();
    this.cancelled = cancelled;
  }

  public @NonNull ServerPlayer player() {
    return this.player;
  }

  public @NonNull Sign sign() {
    return this.sign;
  }

  public @NonNull Optional<ServiceInfoSnapshot> target() {
    return Optional.ofNullable(this.target);
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

  @Override
  public Cause cause() {
    return this.cause;
  }
}
