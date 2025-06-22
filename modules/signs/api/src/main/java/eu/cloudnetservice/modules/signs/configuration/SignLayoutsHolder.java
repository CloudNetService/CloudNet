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

package eu.cloudnetservice.modules.signs.configuration;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
public class SignLayoutsHolder {

  private static final VarHandle LAST_UPDATE_TICK;

  static {
    try {
      var lookup = MethodHandles.lookup();
      LAST_UPDATE_TICK = lookup.findVarHandle(SignLayoutsHolder.class, "lastUpdateTick", long.class);
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private final int animationsPerSecond;
  private final List<SignLayout> signLayouts;

  @SuppressWarnings("unused") // accessed/changed by LAST_UPDATE_TICK
  private transient long lastUpdateTick;

  public SignLayoutsHolder(int animationsPerSecond, @NonNull List<SignLayout> signLayouts) {
    this.animationsPerSecond = animationsPerSecond;
    this.signLayouts = signLayouts;
  }

  public static @NonNull SignLayoutsHolder singleLayout(@NonNull SignLayout layout) {
    return new SignLayoutsHolder(1, List.of(layout));
  }

  public int animationsPerSecond() {
    return this.animationsPerSecond;
  }

  public @NonNull List<SignLayout> signLayouts() {
    return this.signLayouts;
  }

  public boolean hasLayouts() {
    return !this.signLayouts.isEmpty();
  }

  /* == apis only accessed by platforms, not for external use == */

  @ApiStatus.Internal
  public @Nullable SignLayout currentLayout(int tps) {
    var layouts = this.signLayouts;
    var layoutEntryCount = layouts.size();
    return switch (layoutEntryCount) {
      case 0 -> null;
      case 1 -> layouts.getFirst();
      default -> {
        var lastUpdateTick = (long) LAST_UPDATE_TICK.getAcquire(this);
        var animationsPerSecond = Math.min(tps, this.animationsPerSecond); // cannot display more animations per second
        var animationIntervalTicks = tps / animationsPerSecond;
        var steps = lastUpdateTick / animationIntervalTicks;
        var layoutIndex = (int) (steps % layoutEntryCount);
        yield layouts.get(layoutIndex);
      }
    };
  }

  @ApiStatus.Internal
  public void tick(long currentTick) {
    LAST_UPDATE_TICK.setRelease(this, currentTick);
  }
}
