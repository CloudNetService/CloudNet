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

package de.dytanic.cloudnet.database.util;

import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;

public final class LocalDatabaseUtils {

  private static final Logger LOGGER = LogManager.getLogger(LocalDatabaseUtils.class);

  private LocalDatabaseUtils() {
    throw new UnsupportedOperationException();
  }

  public static void bigWarningThatEveryoneCanSeeWhenRunningInCluster(boolean runsInCluster) {
    if (runsInCluster) {
      LOGGER.warning("╔══════════════════════════════════════════════════════════════════╗");
      LOGGER.warning("║                               WARNING                             ");
      LOGGER.warning("║   " + I18n.trans("cloudnet-cluster-local-db-warning"));
      LOGGER.warning("║                                                                   ");
      LOGGER.warning("║                                                                   ");
      LOGGER.warning("║        https://cloudnetservice.eu/docs/3.4/setup/cluster          ");
      LOGGER.warning("╚══════════════════════════════════════════════════════════════════╝");
    }
  }
}
