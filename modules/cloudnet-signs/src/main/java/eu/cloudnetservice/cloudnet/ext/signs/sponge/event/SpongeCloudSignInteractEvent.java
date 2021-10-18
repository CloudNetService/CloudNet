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

package eu.cloudnetservice.cloudnet.ext.signs.sponge.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.event.impl.AbstractEvent;

public class SpongeCloudSignInteractEvent extends AbstractEvent implements TargetPlayerEvent, Cancellable {

  protected final Cause cause;
  protected final Player player;

  protected final Sign sign;

  protected boolean cancelled;
  protected ServiceInfoSnapshot target;

  public SpongeCloudSignInteractEvent(Cause cause, Player player, Sign sign, boolean cancelled) {
    this.cause = cause;
    this.player = player;
    this.sign = sign;
    this.target = sign.getCurrentTarget();
    this.cancelled = cancelled;
  }

  public Sign getSign() {
    return this.sign;
  }

  public Optional<ServiceInfoSnapshot> getTarget() {
    return Optional.ofNullable(this.target);
  }

  public void setTarget(ServiceInfoSnapshot target) {
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
  public @NotNull Player getTargetEntity() {
    return this.player;
  }

  @Override
  public @NotNull Cause getCause() {
    return this.cause;
  }
}
