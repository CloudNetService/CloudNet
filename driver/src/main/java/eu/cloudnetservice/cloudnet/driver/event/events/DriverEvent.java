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

package eu.cloudnetservice.cloudnet.driver.event.events;

import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.event.Event;
import lombok.NonNull;

/**
 * A super class implemented by any event which is related to the driver. Normally modules and external components
 * should either define their own super class for an event or extend the event class directly.
 */
public abstract class DriverEvent extends Event {

  /**
   * Gets the current driver instance of the component. Can never be null.
   * <p>
   * This method is equivalent to a call of {@link CloudNetDriver#instance()}.
   *
   * @return the current driver instance of the component.
   */
  public final @NonNull CloudNetDriver driver() {
    return CloudNetDriver.instance();
  }
}
