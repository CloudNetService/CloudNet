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

package eu.cloudnetservice.modules.signs.impl.util;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.signs.configuration.SignConfigurationEntry;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Util for priorities of signs on a sign wall
 */
public final class PriorityUtil {

  private PriorityUtil() {
    throw new UnsupportedOperationException();
  }

  public static int priority(@NonNull ServiceInfoSnapshot snapshot) {
    // Get the state of the service
    return priority(snapshot, false);
  }

  public static int priority(@NonNull ServiceInfoSnapshot snapshot, @Nullable SignConfigurationEntry entry) {
    return priority(snapshot, LayoutUtil.switchToSearching(snapshot, entry));
  }

  public static int priority(@NonNull ServiceInfoSnapshot snapshot, boolean lowerFullToSearching) {
    // Get the state of the service
    var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(snapshot);
    return switch (state) {
      // full (premium) service are preferred
      case FULL_ONLINE -> lowerFullToSearching ? 1 : 4;
      // online has the second-highest priority as full is preferred
      case ONLINE -> 3;
      // empty services are not the first choice for a sign wall
      case EMPTY_ONLINE -> 2;
      // this sign should only be on the wall when there is no other service
      case STARTING, STOPPED -> 1;
    };
  }
}
