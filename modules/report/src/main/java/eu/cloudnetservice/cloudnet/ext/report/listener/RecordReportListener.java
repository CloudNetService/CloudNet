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
import de.dytanic.cloudnet.event.service.CloudServiceCrashEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreLifecycleEvent;
import de.dytanic.cloudnet.service.ICloudService;
import eu.cloudnetservice.cloudnet.ext.report.CloudNetReportModule;
import eu.cloudnetservice.cloudnet.ext.report.util.RecordMaker;

public final class RecordReportListener {

  private static final Logger LOGGER = LogManager.logger(RecordReportListener.class);
  private final CloudNetReportModule reportModule;

  public RecordReportListener(CloudNetReportModule reportModule) {
    this.reportModule = reportModule;
  }

  @EventListener
  public void handleServicePreStop(CloudServicePreLifecycleEvent event) {
    // we just want to handle the stop lifecycle
    if (event.getTargetLifecycle() != ServiceLifeCycle.STOPPED) {
      return;
    }

    var configuration = this.reportModule.getReportConfiguration();
    var serviceLifetimeSetting = configuration.serviceLifetime();
    // -1 is used to disable the log printing.
    if (serviceLifetimeSetting == -1L) {
      return;
    }

    var connectedTime = System.currentTimeMillis() - event.getServiceInfo().getConnectedTime();
    // check if we should print the log lines based on the online time.
    if (connectedTime <= serviceLifetimeSetting) {
      var consoleLogCache = event.getService().getServiceConsoleLogCache().update();
      for (var logMessage : consoleLogCache.getCachedLogMessages()) {
        LOGGER.severe(String.format("[%s] %s", event.getServiceInfo().name(), logMessage));
      }
    }
  }

  @EventListener
  public void handleServiceCrash(CloudServiceCrashEvent event) {
    // check if the user disabled records
    if (!this.reportModule.getReportConfiguration().saveRecords()) {
      return;
    }
    // we have to create the record
    this.createRecord(event.getService());
  }

  @EventListener
  public void handleServicePreDelete(CloudServicePreLifecycleEvent event) {
    // we just handle the deleted lifecycle
    if (event.getTargetLifecycle() != ServiceLifeCycle.DELETED
      && event.getTargetLifecycle() != ServiceLifeCycle.STOPPED) {
      return;
    }
    // check if the user disabled records
    if (!this.reportModule.getReportConfiguration().saveRecords()) {
      return;
    }
    // check if the user only wants to save reports for crashed services
    if (this.reportModule.getReportConfiguration().saveOnCrashOnly()) {
      return;
    }
    // create the record
    this.createRecord(event.getService());
  }

  private void createRecord(ICloudService cloudService) {
    // we need to check and create the record directory as it's time based.
    var recordCreator = RecordMaker.forService(this.reportModule.getCurrentRecordDirectory(), cloudService);
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
