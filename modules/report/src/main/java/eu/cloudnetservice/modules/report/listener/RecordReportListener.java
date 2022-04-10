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

package eu.cloudnetservice.modules.report.listener;

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.modules.report.CloudNetReportModule;
import eu.cloudnetservice.modules.report.util.RecordMaker;
import eu.cloudnetservice.node.event.service.CloudServicePreForceStopEvent;
import eu.cloudnetservice.node.event.service.CloudServicePreLifecycleEvent;
import eu.cloudnetservice.node.event.service.CloudServicePreProcessStartEvent;
import eu.cloudnetservice.node.service.CloudService;
import lombok.NonNull;

public final class RecordReportListener {

  private static final Logger LOGGER = LogManager.logger(RecordReportListener.class);
  private final CloudNetReportModule reportModule;

  public RecordReportListener(@NonNull CloudNetReportModule reportModule) {
    this.reportModule = reportModule;
  }

  @EventListener
  public void handleServicePreStop(@NonNull CloudServicePreProcessStartEvent event) {
    var configuration = this.reportModule.reportConfiguration();
    var serviceLifetimeSetting = configuration.serviceLifetime();
    // -1 is used to disable the log printing.
    if (serviceLifetimeSetting == -1L) {
      return;
    }

    var connectedTime = System.currentTimeMillis() - event.serviceInfo().connectedTime();
    // check if we should print the log lines based on the online time.
    if (connectedTime <= serviceLifetimeSetting) {
      var consoleLogCache = event.service().serviceConsoleLogCache().update();
      for (var logMessage : consoleLogCache.cachedLogMessages()) {
        LOGGER.severe(String.format("[%s] %s", event.serviceInfo().name(), logMessage));
      }
    }
  }

  @EventListener
  public void handleServiceCrash(@NonNull CloudServicePreForceStopEvent event) {
    // check if the user disabled records
    if (!this.reportModule.reportConfiguration().saveRecords()) {
      return;
    }
    // we have to create the record
    this.createRecord(event.service());
  }

  @EventListener
  public void handleServicePreDelete(@NonNull CloudServicePreLifecycleEvent event) {
    // we just handle the deleted lifecycle
    if (event.targetLifecycle() != ServiceLifeCycle.DELETED
      && event.targetLifecycle() != ServiceLifeCycle.STOPPED) {
      return;
    }
    // check if the user disabled records
    if (!this.reportModule.reportConfiguration().saveRecords()) {
      return;
    }
    // check if the user only wants to save reports for crashed services
    if (this.reportModule.reportConfiguration().saveOnCrashOnly()) {
      return;
    }
    // create the record
    this.createRecord(event.service());
  }

  private void createRecord(@NonNull CloudService cloudService) {
    // we need to check and create the record directory as it's time based.
    var recordCreator = RecordMaker.forService(this.reportModule.currentRecordDirectory(), cloudService);
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
