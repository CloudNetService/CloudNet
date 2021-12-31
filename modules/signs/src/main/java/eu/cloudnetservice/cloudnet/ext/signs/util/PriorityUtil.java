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

package eu.cloudnetservice.cloudnet.ext.signs.util;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * Util for priorities of signs on a sign wall
 */
@Internal
public final class PriorityUtil {

  private PriorityUtil() {
    throw new UnsupportedOperationException();
  }

  public static int priority(@NonNull ServiceInfoSnapshot snapshot) {
    // Get the state of the service
    return priority(snapshot, false);
  }

  public static int priority(@NonNull ServiceInfoSnapshot snapshot, @Nullable SignConfigurationEntry entry) {
    // Get the state of the service
    return priority(snapshot, entry != null && entry.switchToSearchingWhenServiceIsFull());
  }

  public static int priority(@NonNull ServiceInfoSnapshot snapshot, boolean lowerFullToSearching) {
    // Get the state of the service
    var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(snapshot);
    return switch (state) {
      case FULL_ONLINE ->
          // full (premium) service are preferred
          lowerFullToSearching ? 1 : 4;
      case ONLINE ->
          // online has the second-highest priority as full is preferred
          3;
      case EMPTY_ONLINE ->
          // empty services are not the first choice for a sign wall
          2;
      case STARTING, STOPPED ->
          // this sign should only be on the wall when there is no other service
          1;
    };
  }
}
