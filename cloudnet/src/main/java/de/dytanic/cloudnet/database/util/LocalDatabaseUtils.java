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

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.driver.CloudNetDriver;

public final class LocalDatabaseUtils {

  private LocalDatabaseUtils() {
    throw new UnsupportedOperationException();
  }

  public static void bigWarningThatEveryoneCanSeeWhenRunningInCluster(boolean runsInCluster) {
    if (runsInCluster) {
      ILogger logger = CloudNetDriver.getInstance().getLogger();

      logger.warning("╔══════════════════════════════════════════════════════════════════╗");
      logger.warning("║                               WARNING                             ");
      logger.warning("║   " + LanguageManager.getMessage("cloudnet-cluster-local-db-warning"));
      logger.warning("║                                                                   ");
      logger.warning("║                                                                   ");
      logger.warning("║        https://cloudnetservice.eu/docs/3.4/setup/cluster          ");
      logger.warning("╚══════════════════════════════════════════════════════════════════╝");
    }
  }
}
