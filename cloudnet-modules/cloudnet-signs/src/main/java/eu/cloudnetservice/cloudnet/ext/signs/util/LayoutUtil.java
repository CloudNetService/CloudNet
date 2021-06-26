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
import de.dytanic.cloudnet.ext.bridge.ServiceInfoStateWatcher;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignGroupConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayoutsHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LayoutUtil {

  private LayoutUtil() {
    throw new UnsupportedOperationException();
  }

  public static SignLayout getLayout(@NotNull SignConfigurationEntry entry, @NotNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot) {
    return getLayoutHolder(entry, sign, snapshot).getCurrentLayout();
  }

  public static SignLayout getLayoutAndTick(@NotNull SignConfigurationEntry entry, @NotNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot) {
    return getLayoutHolder(entry, sign, snapshot).tick().getCurrentLayout();
  }

  public static SignLayoutsHolder getLayoutHolder(@NotNull SignConfigurationEntry entry, @NotNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot) {
    ServiceInfoStateWatcher.ServiceInfoState state = snapshot == null
      ? ServiceInfoStateWatcher.ServiceInfoState.STOPPED
      : ServiceInfoStateWatcher.stateFromServiceInfoSnapshot(snapshot);
    if (state == ServiceInfoStateWatcher.ServiceInfoState.STOPPED) {
      return entry.getSearchingLayout();
    } else if (state == ServiceInfoStateWatcher.ServiceInfoState.STARTING) {
      return entry.getStartingLayout();
    } else {
      SignGroupConfiguration groupConfiguration = null;
      for (SignGroupConfiguration configuration : entry.getGroupConfigurations()) {
        if (configuration.getTargetGroup() != null && configuration.getTargetGroup().equals(sign.getTargetGroup())
          && configuration.getEmptyLayout() != null && configuration.getOnlineLayout() != null
          && configuration.getFullLayout() != null) {
          groupConfiguration = configuration;
          break;
        }
      }

      switch (state) {
        case EMPTY_ONLINE:
          return groupConfiguration == null ? entry.getEmptyLayout() : groupConfiguration.getEmptyLayout();
        case ONLINE:
          return groupConfiguration == null ? entry.getOnlineLayout() : groupConfiguration.getOnlineLayout();
        case FULL_ONLINE:
          return entry.isSwitchToSearchingWhenServiceIsFull() ? entry.getSearchingLayout()
            : groupConfiguration == null ? entry.getFullLayout() : groupConfiguration.getFullLayout();
        default:
          throw new IllegalStateException("Unexpected service state: " + state);
      }
    }
  }
}
