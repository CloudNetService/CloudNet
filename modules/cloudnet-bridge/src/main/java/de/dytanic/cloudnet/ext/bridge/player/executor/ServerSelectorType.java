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

package de.dytanic.cloudnet.ext.bridge.player.executor;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

public enum ServerSelectorType {

  HIGHEST_PLAYERS((o1, o2) -> Integer.compare(o1.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0),
    o2.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0))),
  LOWEST_PLAYERS((o1, o2) -> Integer.compare(o2.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0),
    o1.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0))),
  RANDOM(Comparator.comparingInt(value -> ThreadLocalRandom.current().nextInt(2) - 1));

  private final Comparator<ServiceInfoSnapshot> comparator;

  ServerSelectorType(Comparator<ServiceInfoSnapshot> comparator) {
    this.comparator = comparator;
  }

  public Comparator<ServiceInfoSnapshot> getComparator() {
    return this.comparator;
  }
}
