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

package eu.cloudnetservice.modules.bridge.event;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.modules.bridge.config.BridgeConfiguration;
import lombok.NonNull;

/**
 * Called when the bridge configuration was updated. This event is called both on all nodes in the cluster and all
 * services running the bridge.
 *
 * @since 4.0
 */
public final class BridgeConfigurationUpdateEvent extends Event {

  private final BridgeConfiguration bridgeConfiguration;

  /**
   * Constructs a new configuration update event with the given configuration.
   *
   * @param bridgeConfiguration the updated configuration.
   * @throws NullPointerException if the given configuration is null.
   */
  public BridgeConfigurationUpdateEvent(@NonNull BridgeConfiguration bridgeConfiguration) {
    this.bridgeConfiguration = bridgeConfiguration;
  }

  /**
   * Gets the updated configuration that was received on this component.
   *
   * @return the updated configuration.
   */
  public @NonNull BridgeConfiguration bridgeConfiguration() {
    return this.bridgeConfiguration;
  }
}
