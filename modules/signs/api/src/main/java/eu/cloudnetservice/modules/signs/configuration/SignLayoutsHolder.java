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

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SignLayoutsHolder {

  private final int animationsPerSecond;
  private final List<SignLayout> signLayouts;

  private transient boolean tickBlocked;
  private transient int currentAnimation = -1;

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

  public boolean tickBlocked() {
    return this.tickBlocked;
  }

  public void enableTickBlock() {
    this.tickBlocked = true;
  }

  public @NonNull SignLayoutsHolder releaseTickBlock() {
    this.tickBlocked = false;
    return this;
  }

  public @NonNull SignLayout currentLayout() {
    return this.signLayouts().get(this.currentAnimation());
  }

  public @NonNull SignLayoutsHolder tick() {
    if (!this.tickBlocked()) {
      if (++this.currentAnimation >= this.signLayouts.size()) {
        this.currentAnimation = 0;
      }
    }
    return this;
  }

  public int currentAnimation() {
    return Math.max(0, this.currentAnimation);
  }
}
