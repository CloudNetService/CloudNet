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

package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.Server;
import cn.nukkit.event.Event;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;

/**
 * All CloudNet events will mostly fire asynchronously, depending on how they were triggered. Check {@link
 * Server#isPrimaryThread()} and treat the event appropriately.
 */
abstract class NukkitCloudNetEvent extends Event {

  public final CloudNetDriver getDriver() {
    return CloudNetDriver.getInstance();
  }

  public final Wrapper getWrapper() {
    return Wrapper.getInstance();
  }

}
