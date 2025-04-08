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

package eu.cloudnetservice.modules.smart.impl.util;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.smart.SmartServiceTaskConfig;
import lombok.NonNull;

public final class SmartUtil {

  private SmartUtil() {
    throw new UnsupportedOperationException();
  }

  public static boolean canStopNow(
    @NonNull ServiceTask task,
    @NonNull SmartServiceTaskConfig config,
    int runningServices
  ) {
    // get the min service count
    var minServiceCount = Math.max(task.minServiceCount(), config.smartMinServiceCount());
    // check if stopping the service would instantly cause a new service to start - beware
    return (runningServices - 1) >= minServiceCount;
  }

  public static double playerPercentage(@NonNull ServiceInfoSnapshot snapshot) {
    var onlinePlayers = snapshot.readProperty(BridgeDocProperties.ONLINE_COUNT);
    var maxPlayers = snapshot.readProperty(BridgeDocProperties.MAX_PLAYERS);
    // get the player percentage
    return percentage(onlinePlayers, maxPlayers);
  }

  public static double percentage(double value, double max) {
    return ((value * 100) / max);
  }
}
