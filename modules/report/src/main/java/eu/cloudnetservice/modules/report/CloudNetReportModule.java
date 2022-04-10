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

package eu.cloudnetservice.modules.report;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.modules.report.command.CommandReport;
import eu.cloudnetservice.modules.report.config.PasteService;
import eu.cloudnetservice.modules.report.config.ReportConfiguration;
import eu.cloudnetservice.modules.report.listener.RecordReportListener;
import eu.cloudnetservice.modules.report.paste.emitter.EmitterRegistry;
import eu.cloudnetservice.modules.report.paste.emitter.defaults.node.ConsoleLogEmitter;
import eu.cloudnetservice.modules.report.paste.emitter.defaults.node.ModuleEmitter;
import eu.cloudnetservice.modules.report.paste.emitter.defaults.node.NodeAllocationEmitter;
import eu.cloudnetservice.modules.report.paste.emitter.defaults.node.NodeConfigurationEmitter;
import eu.cloudnetservice.modules.report.paste.emitter.defaults.node.NodeSnapshotEmitter;
import eu.cloudnetservice.modules.report.paste.emitter.defaults.service.ServiceInfoSnapshotEmitter;
import eu.cloudnetservice.modules.report.paste.emitter.defaults.service.ServiceLogEmitter;
import eu.cloudnetservice.modules.report.paste.emitter.defaults.service.ServiceOverviewEmitter;
import eu.cloudnetservice.modules.report.paste.emitter.defaults.service.ServiceTaskEmitter;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.service.CloudService;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import lombok.NonNull;

public final class CloudNetReportModule extends DriverModule {

  private Path recordDirectory;
  private EmitterRegistry registry;
  private ReportConfiguration configuration;

  @ModuleTask(event = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    var config = this.readConfig();
    if (config.contains("savingRecords")) {
      this.writeConfig(JsonDocument.newDocument(new ReportConfiguration(
        config.getBoolean("savingRecords"),
        true,
        config.get("recordDestinationDirectory", Path.class, Path.of("records")),
        config.getLong("serviceLifetimeLogPrint", 5000L),
        new SimpleDateFormat("yyyy-MM-dd"),
        List.of(new PasteService("default", config.getString("pasteServerUrl", "https://just-paste.it")))
      )));
    }
  }

  @ModuleTask
  public void init() {
    // read the configuration from the file and convert it if necessary
    this.reloadConfiguration();
    // create a new registry for our report data emitters
    this.registry = new EmitterRegistry();
    // register all emitters that are used for the ICloudService report
    this.registry.registerDataEmitter(CloudService.class, new ServiceLogEmitter())
      .registerDataEmitter(CloudService.class, new ServiceInfoSnapshotEmitter())
      .registerDataEmitter(CloudService.class, new ServiceOverviewEmitter())
      .registerDataEmitter(CloudService.class, new ServiceTaskEmitter());
    // register all emitters that are used for the Node report
    this.registry.registerDataEmitter(NetworkClusterNodeInfoSnapshot.class, new ConsoleLogEmitter())
      .registerDataEmitter(NetworkClusterNodeInfoSnapshot.class, new NodeAllocationEmitter())
      .registerDataEmitter(NetworkClusterNodeInfoSnapshot.class, new NodeSnapshotEmitter())
      .registerDataEmitter(NetworkClusterNodeInfoSnapshot.class, new NodeConfigurationEmitter())
      .registerDataEmitter(NetworkClusterNodeInfoSnapshot.class, new ModuleEmitter());
    // register our listener to handle stopping and deleted services
    this.registerListener(new RecordReportListener(this));
    this.serviceRegistry().registerProvider(EmitterRegistry.class, "EmitterRegistry", this.registry);
    // register the command of the module at the node
    Node.instance().commandProvider().register(new CommandReport(this));
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void reload() {
    this.reloadConfiguration();
  }

  public @NonNull EmitterRegistry emitterRegistry() {
    return this.registry;
  }

  public @NonNull ReportConfiguration reportConfiguration() {
    return this.configuration;
  }

  public @NonNull Path currentRecordDirectory() {
    // resolve the target record directory
    var date = this.configuration.dateFormat().format(System.currentTimeMillis());
    var dir = this.moduleWrapper.dataDirectory().resolve(this.configuration.recordDestination()).resolve(date);
    // create the directory if it does not yet exist
    FileUtil.createDirectory(dir);
    return this.recordDirectory = dir;
  }

  private void reloadConfiguration() {
    this.configuration = this.readConfig(ReportConfiguration.class, () -> ReportConfiguration.builder().build());
  }
}
