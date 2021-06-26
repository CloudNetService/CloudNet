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

package de.dytanic.cloudnet.ext.bridge.proxy;

import com.google.common.collect.ComparisonChain;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import org.jetbrains.annotations.NotNull;

public class PlayerFallback implements Comparable<PlayerFallback> {

  private final int priority;
  private final ServiceInfoSnapshot targetServiceInfoSnapshot;

  public PlayerFallback(int priority, ServiceInfoSnapshot targetServiceInfoSnapshot) {
    this.priority = priority;
    this.targetServiceInfoSnapshot = targetServiceInfoSnapshot;
  }

  public int getPriority() {
    return this.priority;
  }

  public ServiceInfoSnapshot getTarget() {
    return this.targetServiceInfoSnapshot;
  }


  public int getOnlineCount() {
    return this.targetServiceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(-1);
  }

  @Override
  public int compareTo(@NotNull PlayerFallback fallback) {
    return ComparisonChain.start()
      .compare(fallback.priority, this.priority)
      .compare(this.getOnlineCount(), fallback.getOnlineCount())
      .result();
  }
}
