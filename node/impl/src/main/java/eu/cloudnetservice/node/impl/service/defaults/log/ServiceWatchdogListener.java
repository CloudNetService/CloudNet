/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.service.defaults.log;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.node.event.service.CloudServicePreForceStopEvent;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ServiceWatchdogListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceWatchdogListener.class);
  private static final int SERVICE_LOG_THRESHOLD_MILLIS =
    Integer.getInteger("cloudnet.service-watchdog-threshold", 5) * 1000;

  @EventListener
  private void handleCloudServicePreForceStop(@NonNull CloudServicePreForceStopEvent event) {
    var connectionTime = event.serviceInfo().connectedTime();
    if (System.currentTimeMillis() - connectionTime < SERVICE_LOG_THRESHOLD_MILLIS) {
      for (var cachedLogMessage : event.service().cachedLogMessages()) {
        LOGGER.debug("[{} - Watchdog] {}", event.serviceInfo().name(), cachedLogMessage);
      }
    }
  }
}
