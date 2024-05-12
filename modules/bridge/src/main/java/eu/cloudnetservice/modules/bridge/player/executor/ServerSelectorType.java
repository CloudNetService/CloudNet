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

package eu.cloudnetservice.modules.bridge.player.executor;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;

/**
 * The server selector type provides several constants to compare services based conditions.
 *
 * @since 4.0
 */
public enum ServerSelectorType {

  /**
   * The service with the lowest player count is preferred.
   */
  LOWEST_PLAYERS(Comparator.comparingInt(ser -> ser.readProperty(BridgeDocProperties.ONLINE_COUNT))),
  /**
   * The service with the highest player count is preferred.
   */
  HIGHEST_PLAYERS(LOWEST_PLAYERS.comparator.reversed()),
  /**
   * A random service is chosen.
   */
  RANDOM(Comparator.comparingInt(value -> ThreadLocalRandom.current().nextInt(-1, 2)));

  private final Comparator<ServiceInfoSnapshot> comparator;

  /**
   * Creates a new server selector type enum constant with the given service comparator.
   *
   * @param comparator the comparator to apply to the services.
   * @throws NullPointerException if the comparator is null.
   */
  ServerSelectorType(@NonNull Comparator<ServiceInfoSnapshot> comparator) {
    this.comparator = comparator;
  }

  /**
   * Gets the comparator used to sort the services infos according to the server selector type.
   *
   * @return the comparator for the server selector.
   */
  public @NonNull Comparator<ServiceInfoSnapshot> comparator() {
    return this.comparator;
  }
}
