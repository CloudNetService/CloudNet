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

package eu.cloudnetservice.cloudnet.ext.report.listener;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.event.service.CloudServicePostLifecycleEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreLifecycleEvent;
import de.dytanic.cloudnet.service.IServiceConsoleLogCache;
import eu.cloudnetservice.cloudnet.ext.report.CloudNetReportModule;
import eu.cloudnetservice.cloudnet.ext.report.config.ReportConfiguration;
import eu.cloudnetservice.cloudnet.ext.report.util.Record;

public final class CloudNetReportListener {

  private static final Logger LOGGER = LogManager.getLogger(CloudNetReportListener.class);
  private final CloudNetReportModule reportModule;

  public CloudNetReportListener(CloudNetReportModule reportModule) {
    this.reportModule = reportModule;
  }

  @EventListener
  public void handlePostStop(CloudServicePreLifecycleEvent event) {
    // we just want to handle the stop lifecycle
    if (event.getTargetLifecycle() != ServiceLifeCycle.STOPPED) {
      return;
    }

    ReportConfiguration configuration = this.reportModule.getReportConfiguration();
    long serviceLifetimeSetting = configuration.getServiceLifetime();
    // -1 is used to disable the log printing.
    if (serviceLifetimeSetting == -1L) {
      return;
    }

    long connectedTime = System.currentTimeMillis() - event.getServiceInfo().getConnectedTime();
    // check if we should print the log lines based on the online time.
    if (connectedTime <= serviceLifetimeSetting) {
      IServiceConsoleLogCache consoleLogCache = event.getService().getServiceConsoleLogCache().update();
      for (String logMessage : consoleLogCache.getCachedLogMessages()) {
        LOGGER.severe(String.format("[%s] %s", event.getServiceInfo().getName(), logMessage));
      }
    }
  }

  @EventListener
  public void handlePreDelete(CloudServicePostLifecycleEvent event) {
    // we just handle the deleted lifecycle
    if (event.getNewLifeCycle() != ServiceLifeCycle.DELETED) {
      return;
    }
    // check if the user disabled records
    if (!this.reportModule.getReportConfiguration().isSaveRecords()) {
      return;
    }
    // we need to check and create the record directory as it's time based.
    Record recordCreator = Record.forService(this.reportModule.getCurrentRecordDirectory(),
      event.getService());
    // unable to create records as the directory already exists
    if (recordCreator == null) {
      return;
    }
    // copy and create our files
    recordCreator.copyLogFiles();
    recordCreator.writeCachedConsoleLog();
    recordCreator.writeServiceInfoSnapshot();
    recordCreator.notifySuccess();
  }
}
