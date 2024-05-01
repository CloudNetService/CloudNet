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

package eu.cloudnetservice.modules.signs.util;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.component.ComponentFormat;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.modules.signs.configuration.SignGroupConfiguration;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.configuration.SignLayoutsHolder;
import java.util.HashMap;
import java.util.function.BiConsumer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Util to resolve the layout of a sign
 */
public final class LayoutUtil {

  private LayoutUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull SignLayout layout(@NonNull SignConfigurationEntry entry, @NonNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot) {
    return layoutHolder(entry, sign, snapshot).currentLayout();
  }

  public static @NonNull SignLayout layoutAndTick(
    @NonNull SignConfigurationEntry entry,
    @NonNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot
  ) {
    return layoutHolder(entry, sign, snapshot).tick().currentLayout();
  }

  public static boolean switchToSearching(
    @NonNull ServiceInfoSnapshot snapshot,
    @Nullable SignConfigurationEntry entry
  ) {
    if (entry != null) {
      for (var groupConfiguration : entry.groupConfigurations()) {
        if (snapshot.configuration().groups().contains(groupConfiguration.targetGroup())
          && groupConfiguration.switchToSearchingWhenServiceIsFull()) {
          return true;
        }
      }
    }
    return entry != null && entry.switchToSearchingWhenServiceIsFull();
  }

  public static @NonNull SignLayoutsHolder layoutHolder(
    @NonNull SignConfigurationEntry entry,
    @NonNull Sign sign,
    @Nullable ServiceInfoSnapshot snapshot
  ) {
    // check if no snapshot is used for the check process - return the searching layout in that case
    if (snapshot == null) {
      return entry.searchingLayout();
    }
    // return the correct layout based on the state
    var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(snapshot);
    if (state == BridgeServiceHelper.ServiceInfoState.STOPPED) {
      return entry.searchingLayout();
    } else if (state == BridgeServiceHelper.ServiceInfoState.STARTING) {
      return entry.startingLayout();
    } else {
      // check for an overriding group configuration
      SignGroupConfiguration groupConfiguration = null;
      for (var configuration : entry.groupConfigurations()) {
        if (configuration.targetGroup().equals(sign.targetGroup())) {
          groupConfiguration = configuration;
          break;
        }
      }
      // get the correct layout based on the entry, group layout and state
      return switch (state) {
        case EMPTY_ONLINE -> groupConfiguration == null ? entry.emptyLayout() : groupConfiguration.emptyLayout();
        case ONLINE -> groupConfiguration == null ? entry.onlineLayout() : groupConfiguration.onlineLayout();
        case FULL_ONLINE -> switchToSearching(snapshot, entry)
          ? entry.searchingLayout()
          : groupConfiguration == null ? entry.fullLayout() : groupConfiguration.fullLayout();
        default -> throw new IllegalStateException("Unexpected service state: " + state);
      };
    }
  }

  public static <C> void updateSignLines(
    @NonNull SignLayout layout,
    @NonNull String signTargetGroup,
    @Nullable ServiceInfoSnapshot target,
    @NonNull ComponentFormat<C> componentFormat,
    @NonNull BiConsumer<Integer, C> lineSetter
  ) {
    var placeholders = new HashMap<String, Component>();
    BridgeServiceHelper.fillCommonPlaceholders(placeholders, signTargetGroup, target);
    var lines = layout.lines();
    for (var i = 0; i < Math.min(4, lines.size()); i++) {
      var converted = componentFormat.fromAdventure(
        ComponentFormats.USER_INPUT.withPlaceholders(placeholders).toAdventure(lines.get(i))
      );
      lineSetter.accept(i, converted);
    }
  }
}
