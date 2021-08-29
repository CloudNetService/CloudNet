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

package eu.cloudnetservice.cloudnet.ext.signs.configuration;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.ToString;

@ToString
public class SignLayoutsHolder {

  protected int animationsPerSecond;
  protected List<SignLayout> signLayouts;

  protected transient AtomicBoolean tickBlocked;
  protected transient AtomicInteger currentAnimation;

  public SignLayoutsHolder() {
  }

  public SignLayoutsHolder(int animationsPerSecond, List<SignLayout> signLayouts) {
    this.animationsPerSecond = animationsPerSecond;
    this.signLayouts = signLayouts;
  }

  public int getAnimationsPerSecond() {
    return this.animationsPerSecond;
  }

  public void setAnimationsPerSecond(int animationsPerSecond) {
    this.animationsPerSecond = animationsPerSecond;
  }

  public List<SignLayout> getSignLayouts() {
    return this.signLayouts;
  }

  public void setSignLayouts(List<SignLayout> signLayouts) {
    this.signLayouts = signLayouts;
  }

  public boolean hasLayouts() {
    return !this.signLayouts.isEmpty();
  }

  public boolean isTickedBlocked() {
    return this.tickBlocked != null && this.tickBlocked.get();
  }

  public void enableTickBlock() {
    if (this.tickBlocked == null) {
      this.tickBlocked = new AtomicBoolean();
    }
    this.tickBlocked.set(true);
  }

  public SignLayoutsHolder releaseTickBlock() {
    if (this.tickBlocked != null) {
      this.tickBlocked.set(false);
    }
    return this;
  }

  public SignLayout getCurrentLayout() {
    return this.getSignLayouts().get(this.getCurrentAnimation());
  }

  public SignLayoutsHolder tick() {
    if (!this.isTickedBlocked()) {
      AtomicInteger currentIndex = this.getCurrentAnimationIndexOrInit();
      if (currentIndex.incrementAndGet() >= this.signLayouts.size()) {
        currentIndex.set(0);
      }
    }
    return this;
  }

  public int getCurrentAnimation() {
    return this.currentAnimation == null ? 0 : this.currentAnimation.get();
  }

  protected AtomicInteger getCurrentAnimationIndexOrInit() {
    if (this.currentAnimation == null) {
      return this.currentAnimation = new AtomicInteger(-1);
    } else {
      return this.currentAnimation;
    }
  }
}
