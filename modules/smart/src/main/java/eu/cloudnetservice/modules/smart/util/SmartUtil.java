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

package eu.cloudnetservice.modules.smart.util;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperties;
import eu.cloudnetservice.modules.smart.SmartServiceTaskConfig;
import org.jetbrains.annotations.NotNull;

public final class SmartUtil {

  private SmartUtil() {
    throw new UnsupportedOperationException();
  }

  public static boolean canStopNow(
    @NotNull ServiceTask task,
    @NotNull SmartServiceTaskConfig config,
    int runningServices
  ) {
    // get the min service count
    var minServiceCount = Math.max(task.getMinServiceCount(), config.smartMinServiceCount());
    // check if stopping the service would instantly cause a new service to start - beware
    return (runningServices - 1) > minServiceCount;
  }

  public static double getPlayerPercentage(@NotNull ServiceInfoSnapshot snapshot) {
    int onlinePlayers = BridgeServiceProperties.ONLINE_COUNT.get(snapshot).orElse(0);
    int maxPlayers = BridgeServiceProperties.MAX_PLAYERS.get(snapshot).orElse(1);
    // get the player percentage
    return getPercentage(onlinePlayers, maxPlayers);
  }

  public static double getPercentage(double value, double max) {
    return ((value * 100) / max);
  }
}
