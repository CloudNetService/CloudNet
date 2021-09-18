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

package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

public final class ServiceInfoSnapshotUtil {

  private ServiceInfoSnapshotUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated use {@link IPlayerManager#taskOnlinePlayers(String)}
   */
  @Deprecated
  @ScheduledForRemoval
  public static int getTaskOnlineCount(String taskName) {
    return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class)
      .taskOnlinePlayers(taskName)
      .count();
  }

  /**
   * @deprecated use {@link IPlayerManager#groupOnlinePlayers(String)}
   */
  @Deprecated
  @ScheduledForRemoval
  public static int getGroupOnlineCount(String groupName) {
    return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class)
      .groupOnlinePlayers(groupName)
      .count();
  }
}
