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

package eu.cloudnetservice.modules.signs.impl.platform;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.impl.util.LayoutUtil;
import eu.cloudnetservice.modules.signs.impl.util.PriorityUtil;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class PlatformSign<P, C> implements Comparable<PlatformSign<P, C>> {

  protected final Sign base;
  protected final ServiceRegistry serviceRegistry;
  protected final Function<String, C> lineMapper;
  protected volatile ServiceInfoSnapshot target;
  protected PlayerManager playerManager;

  public PlatformSign(
    @NonNull Sign base,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull Function<String, C> lineMapper
  ) {
    this.base = base;
    this.serviceRegistry = serviceRegistry;
    this.lineMapper = lineMapper;
  }

  public @NonNull Sign base() {
    return this.base;
  }

  public @Nullable ServiceInfoSnapshot currentTarget() {
    return this.target;
  }

  public void currentTarget(@Nullable ServiceInfoSnapshot snapshot) {
    this.target = snapshot;
  }

  public void handleInteract(@NonNull UUID playerUniqueId, @NonNull P playerInstance) {
    // keep a local copy of the target as the view might change due to concurrent update calls
    var target = this.target;
    if (target == null) {
      return;
    }

    // get the current state from the service snapshot, ignore if the target is not yet ready to accept players
    var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(target);
    if (state == BridgeServiceHelper.ServiceInfoState.STOPPED
      || state == BridgeServiceHelper.ServiceInfoState.STARTING) {
      return;
    }

    // get the target to connect the player to, null indicates that the event was cancelled
    target = this.callSignInteractEvent(playerInstance);
    if (target == null) {
      return;
    }

    this.playerManager().playerExecutor(playerUniqueId).connect(target.name());
  }

  public int priority() {
    return this.priority(false);
  }

  public int priority(@Nullable SignConfigurationEntry entry) {
    // check if the service has a snapshot
    var target = this.currentTarget();
    // no target has the lowest priority
    return target == null ? 0 : PriorityUtil.priority(target, entry);
  }

  public int priority(boolean lowerFullToSearching) {
    // check if the service has a snapshot
    var target = this.currentTarget();
    // no target has the lowest priority
    return target == null ? 0 : PriorityUtil.priority(target, lowerFullToSearching);
  }

  protected void changeSignLines(@NonNull SignLayout layout, @NonNull BiConsumer<Integer, C> lineSetter) {
    LayoutUtil.updateSignLines(layout, this.base.targetGroup(), this.target, this.lineMapper, lineSetter);
  }

  protected @NonNull PlayerManager playerManager() {
    if (this.playerManager == null) {
      this.playerManager = this.serviceRegistry.defaultInstance(PlayerManager.class);
    }

    return this.playerManager;
  }

  @Override
  public int compareTo(@NonNull PlatformSign<P, C> sign) {
    return Integer.compare(this.priority(), sign.priority());
  }

  public abstract boolean exists();

  public abstract boolean needsUpdates();

  public abstract void updateSign(@NonNull SignLayout layout);

  public abstract @Nullable ServiceInfoSnapshot callSignInteractEvent(@NonNull P player);
}
