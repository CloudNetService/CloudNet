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
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import de.dytanic.cloudnet.service.ICloudService;
import eu.cloudnetservice.cloudnet.ext.report.command.CommandPaste;
import eu.cloudnetservice.cloudnet.ext.report.config.ReportConfiguration;
import eu.cloudnetservice.cloudnet.ext.report.config.ReportConfigurationHelper;
import eu.cloudnetservice.cloudnet.ext.report.listener.CloudNetReportListener;
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
import org.jetbrains.annotations.NotNull;

public final class CloudNetReportModule extends NodeCloudNetModule {

  private static final Logger LOGGER = LogManager.getLogger(CloudNetReportModule.class);

  private static CloudNetReportModule instance;
  private ReportConfiguration reportConfiguration;
  private EmitterRegistry registry;
  private Path currentRecordDirectory;

  @ModuleTask
  public void init() {
    this.reportConfiguration = ReportConfigurationHelper.read(
      this.moduleWrapper.getDataDirectory().resolve("config.json"));
    this.registerListener(new CloudNetReportListener(this));
    this.registry = new EmitterRegistry();

    this.registry.registerDataEmitter(ICloudService.class, new ServiceLogEmitter(), new ServiceInfoSnapshotEmitter(),
      new ServiceOverviewEmitter(), new ServiceTaskEmitter());
    this.registry.registerDataEmitter(NetworkClusterNodeInfoSnapshot.class, new ConsoleLogEmitter(),
      new NodeSnapshotEmitter(),
      new NodeConfigurationEmitter(), new NodeAllocationEmitter(), new ModuleEmitter());

    CloudNet.getInstance().getCommandProvider().register(new CommandPaste(this));
  }

  public static CloudNetReportModule getInstance() {
    return CloudNetReportModule.instance;
  }

  public void createRecordDirectory() {
    String date = this.reportConfiguration.getDateFormat().format(System.currentTimeMillis());
    Path recordDestination = this.moduleWrapper.getDataDirectory()
      .resolve(this.reportConfiguration.getRecordDestination());
    this.currentRecordDirectory = recordDestination.resolve(date);

    FileUtils.createDirectoryReported(this.currentRecordDirectory);
  }

  public @NotNull EmitterRegistry getEmitterRegistry() {
    return this.registry;
  }

  public ReportConfiguration getReportConfiguration() {
    return this.reportConfiguration;
  }

  public Path getCurrentRecordDirectory() {
    return this.currentRecordDirectory;
  }
}
