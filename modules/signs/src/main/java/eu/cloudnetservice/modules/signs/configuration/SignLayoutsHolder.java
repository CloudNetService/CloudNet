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

package eu.cloudnetservice.modules.signs.configuration;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.ToString;

@ToString
public class SignLayoutsHolder {

  private final int animationsPerSecond;
  private final List<SignLayout> signLayouts;

  private final transient AtomicBoolean tickBlocked = new AtomicBoolean();
  private final transient AtomicInteger currentAnimation = new AtomicInteger(-1);

  public SignLayoutsHolder(int animationsPerSecond, List<SignLayout> signLayouts) {
    this.animationsPerSecond = animationsPerSecond;
    this.signLayouts = signLayouts;
  }

  public int animationsPerSecond() {
    return this.animationsPerSecond;
  }

  public List<SignLayout> signLayouts() {
    return this.signLayouts;
  }

  public boolean hasLayouts() {
    return !this.signLayouts.isEmpty();
  }

  public boolean tickBlocked() {
    return this.tickBlocked.get();
  }

  public void enableTickBlock() {
    this.tickBlocked.set(true);
  }

  public SignLayoutsHolder releaseTickBlock() {
    this.tickBlocked.set(false);
    return this;
  }

  public SignLayout currentLayout() {
    return this.signLayouts().get(this.currentAnimation());
  }

  public SignLayoutsHolder tick() {
    if (!this.tickBlocked()) {
      if (this.currentAnimation.incrementAndGet() >= this.signLayouts.size()) {
        this.currentAnimation.set(0);
      }
    }
    return this;
  }

  public int currentAnimation() {
    return this.currentAnimation == null ? 0 : this.currentAnimation.get();
  }
}
