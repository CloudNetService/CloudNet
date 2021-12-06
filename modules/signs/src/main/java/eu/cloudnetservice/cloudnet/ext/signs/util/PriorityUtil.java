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

package eu.cloudnetservice.cloudnet.ext.signs.util;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper.ServiceInfoState;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Util for priorities of signs on a sign wall
 */
@Internal
public final class PriorityUtil {

  private PriorityUtil() {
    throw new UnsupportedOperationException();
  }

  public static int getPriority(@NotNull ServiceInfoSnapshot snapshot) {
    // Get the state of the service
    return getPriority(snapshot, false);
  }

  public static int getPriority(@NotNull ServiceInfoSnapshot snapshot, @Nullable SignConfigurationEntry entry) {
    // Get the state of the service
    return getPriority(snapshot, entry != null && entry.isSwitchToSearchingWhenServiceIsFull());
  }

  public static int getPriority(@NotNull ServiceInfoSnapshot snapshot, boolean lowerFullToSearching) {
    // Get the state of the service
    ServiceInfoState state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(snapshot);
    switch (state) {
      case FULL_ONLINE:
        // full (premium) service are preferred
        return lowerFullToSearching ? 1 : 4;
      case ONLINE:
        // online has the second-highest priority as full is preferred
        return 3;
      case EMPTY_ONLINE:
        // empty services are not the first choice for a sign wall
        return 2;
      case STARTING:
      case STOPPED:
        // this sign should only be on the wall when there is no other service
        return 1;
      default:
        return 0;
    }
  }
}
