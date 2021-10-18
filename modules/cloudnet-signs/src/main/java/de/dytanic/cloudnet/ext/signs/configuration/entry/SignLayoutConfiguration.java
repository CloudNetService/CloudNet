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

package de.dytanic.cloudnet.ext.signs.configuration.entry;

import de.dytanic.cloudnet.ext.signs.SignLayout;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SignLayoutConfiguration {

  protected List<SignLayout> signLayouts;

  protected int animationsPerSecond;

  public SignLayoutConfiguration(List<SignLayout> signLayouts, int animationsPerSecond) {
    this.signLayouts = signLayouts;
    this.animationsPerSecond = animationsPerSecond;
  }

  public SignLayoutConfiguration() {
  }

  public List<SignLayout> getSignLayouts() {
    return this.signLayouts;
  }

  public void setSignLayouts(List<SignLayout> signLayouts) {
    this.signLayouts = signLayouts;
  }

  public int getAnimationsPerSecond() {
    return this.animationsPerSecond;
  }

  public void setAnimationsPerSecond(int animationsPerSecond) {
    this.animationsPerSecond = animationsPerSecond;
  }

}
