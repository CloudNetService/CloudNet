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

package eu.cloudnetservice.cloudnet.ext.report;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.service.CloudService;
import eu.cloudnetservice.cloudnet.ext.report.command.CommandReport;
import eu.cloudnetservice.cloudnet.ext.report.config.ReportConfiguration;
import eu.cloudnetservice.cloudnet.ext.report.config.ReportConfigurationHelper;
import eu.cloudnetservice.cloudnet.ext.report.listener.RecordReportListener;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.EmitterRegistry;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.node.ConsoleLogEmitter;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.node.ModuleEmitter;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.node.NodeAllocationEmitter;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.node.NodeConfigurationEmitter;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.node.NodeSnapshotEmitter;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.service.ServiceInfoSnapshotEmitter;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.service.ServiceLogEmitter;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.service.ServiceOverviewEmitter;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.service.ServiceTaskEmitter;
import java.nio.file.Path;
import lombok.NonNull;

public final class CloudNetReportModule extends DriverModule {

  private Path recordDirectory;
  private EmitterRegistry registry;
  private ReportConfiguration reportConfiguration;

  @ModuleTask
  public void init() {
    // read the configuration from the file and convert it if necessary
    this.reloadConfiguration();
    // create a new registry for our report data emitters
    this.registry = new EmitterRegistry();
    // register all emitters that are used for the ICloudService report
    this.registry.registerDataEmitter(CloudService.class,
      new ServiceLogEmitter(),
      new ServiceInfoSnapshotEmitter(),
      new ServiceOverviewEmitter(),
      new ServiceTaskEmitter());
    // register all emitters that are used for the Node report
    this.registry.registerDataEmitter(NetworkClusterNodeInfoSnapshot.class,
      new ConsoleLogEmitter(),
      new NodeAllocationEmitter(),
      new NodeSnapshotEmitter(),
      new NodeConfigurationEmitter(),
      new ModuleEmitter());
    // register our listener to handle stopping and deleted services
    this.registerListener(new RecordReportListener(this));
    // register the command of the module at the node
    CloudNet.instance().commandProvider().register(new CommandReport(this));
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void reload() {
    this.reloadConfiguration();
  }

  public @NonNull EmitterRegistry emitterRegistry() {
    return this.registry;
  }

  public @NonNull ReportConfiguration reportConfiguration() {
    return this.reportConfiguration;
  }

  public @NonNull Path currentRecordDirectory() {
    var date = this.reportConfiguration.dateFormat().format(System.currentTimeMillis());
    var recordBaseDestination = this.moduleWrapper.dataDirectory()
      .resolve(this.reportConfiguration.recordDestination());
    var timeBasedDestination = recordBaseDestination.resolve(date);
    if (timeBasedDestination.equals(this.recordDirectory)) {
      return this.recordDirectory;
    }

    FileUtils.createDirectory(timeBasedDestination);
    return this.recordDirectory = timeBasedDestination;
  }

  private void reloadConfiguration() {
    this.reportConfiguration = ReportConfigurationHelper.read(this.configPath());
  }
}
