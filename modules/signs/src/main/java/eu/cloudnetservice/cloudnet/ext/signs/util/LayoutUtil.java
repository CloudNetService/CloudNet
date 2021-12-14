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
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignGroupConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayoutsHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Util to resolve the layout of a sign
 */
public final class LayoutUtil {

  private LayoutUtil() {
    throw new UnsupportedOperationException();
  }

  public static SignLayout getLayout(@NotNull SignConfigurationEntry entry, @NotNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot) {
    return getLayoutHolder(entry, sign, snapshot).getCurrentLayout();
  }

  public static SignLayout getLayoutAndTick(
    @NotNull SignConfigurationEntry entry,
    @NotNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot
  ) {
    return getLayoutHolder(entry, sign, snapshot).tick().getCurrentLayout();
  }

  public static SignLayoutsHolder getLayoutHolder(
    @NotNull SignConfigurationEntry entry,
    @NotNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot
  ) {
    // check if no snapshot is used for the check process - return the searchig layout in that case
    if (snapshot == null) {
      return entry.getSearchingLayout();
    }
    // return the correct layout based on the state
    var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(snapshot);
    if (state == ServiceInfoState.STOPPED) {
      return entry.getSearchingLayout();
    } else if (state == ServiceInfoState.STARTING) {
      return entry.getStartingLayout();
    } else {
      // check for an overriding group configuration
      SignGroupConfiguration groupConfiguration = null;
      for (var configuration : entry.getGroupConfigurations()) {
        if (configuration.getTargetGroup() != null
          && configuration.getTargetGroup().equals(sign.getTargetGroup())
          && configuration.getEmptyLayout() != null
          && configuration.getOnlineLayout() != null
          && configuration.getFullLayout() != null
        ) {
          groupConfiguration = configuration;
          break;
        }
      }
      // get the correct layout based on the entry, group layout and state
      switch (state) {
        case EMPTY_ONLINE:
          return groupConfiguration == null ? entry.getEmptyLayout() : groupConfiguration.getEmptyLayout();
        case ONLINE:
          return groupConfiguration == null ? entry.getOnlineLayout() : groupConfiguration.getOnlineLayout();
        case FULL_ONLINE:
          return entry.isSwitchToSearchingWhenServiceIsFull()
            ? entry.getSearchingLayout()
            : groupConfiguration == null ? entry.getFullLayout() : groupConfiguration.getFullLayout();
        default:
          throw new IllegalStateException("Unexpected service state: " + state);
      }
    }
  }
}
