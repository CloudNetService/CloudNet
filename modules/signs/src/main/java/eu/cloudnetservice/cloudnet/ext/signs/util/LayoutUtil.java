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
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper.ServiceInfoState;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignGroupConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayoutsHolder;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Util to resolve the layout of a sign
 */
public final class LayoutUtil {

  private LayoutUtil() {
    throw new UnsupportedOperationException();
  }

  public static SignLayout layout(@NonNull SignConfigurationEntry entry, @NonNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot) {
    return layoutHolder(entry, sign, snapshot).currentLayout();
  }

  public static SignLayout layoutAndTick(
    @NonNull SignConfigurationEntry entry,
    @NonNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot
  ) {
    return layoutHolder(entry, sign, snapshot).tick().currentLayout();
  }

  public static SignLayoutsHolder layoutHolder(
    @NonNull SignConfigurationEntry entry,
    @NonNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot
  ) {
    // check if no snapshot is used for the check process - return the searchig layout in that case
    if (snapshot == null) {
      return entry.searchingLayout();
    }
    // return the correct layout based on the state
    var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(snapshot);
    if (state == ServiceInfoState.STOPPED) {
      return entry.searchingLayout();
    } else if (state == ServiceInfoState.STARTING) {
      return entry.startingLayout();
    } else {
      // check for an overriding group configuration
      SignGroupConfiguration groupConfiguration = null;
      for (var configuration : entry.groupConfigurations()) {
        if (configuration.targetGroup() != null
          && configuration.targetGroup().equals(sign.targetGroup())
          && configuration.emptyLayout() != null
          && configuration.onlineLayout() != null
          && configuration.fullLayout() != null
        ) {
          groupConfiguration = configuration;
          break;
        }
      }
      // get the correct layout based on the entry, group layout and state
      return switch (state) {
        case EMPTY_ONLINE -> groupConfiguration == null ? entry.emptyLayout() : groupConfiguration.emptyLayout();
        case ONLINE -> groupConfiguration == null ? entry.onlineLayout() : groupConfiguration.onlineLayout();
        case FULL_ONLINE -> entry.switchToSearchingWhenServiceIsFull()
          ? entry.searchingLayout()
          : groupConfiguration == null ? entry.fullLayout() : groupConfiguration.fullLayout();
        default -> throw new IllegalStateException("Unexpected service state: " + state);
      };
    }
  }
}
