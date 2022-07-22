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
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.report.command.ReportCommand;
import eu.cloudnetservice.modules.report.config.PasteServer;
import eu.cloudnetservice.modules.report.config.ReportConfiguration;
import eu.cloudnetservice.modules.report.emitter.EmitterRegistry;
import eu.cloudnetservice.modules.report.emitter.defaults.GroupConfigDataEmitter;
import eu.cloudnetservice.modules.report.emitter.defaults.HeapDumpDataEmitter;
import eu.cloudnetservice.modules.report.emitter.defaults.LocalModuleDataEmitter;
import eu.cloudnetservice.modules.report.emitter.defaults.LocalNodeConfigDataEmitter;
import eu.cloudnetservice.modules.report.emitter.defaults.NodeServerDataEmitter;
import eu.cloudnetservice.modules.report.emitter.defaults.ServiceInfoDataEmitter;
import eu.cloudnetservice.modules.report.emitter.defaults.ServiceTasksDataEmitter;
import eu.cloudnetservice.modules.report.emitter.defaults.SystemInfoDataEmitter;
import eu.cloudnetservice.modules.report.emitter.defaults.ThreadInfoDataEmitter;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.cluster.NodeServer;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

public final class CloudNetReportModule extends DriverModule {

  private final Node node = Node.instance();

  private ReportConfiguration configuration;

  @ModuleTask(order = 127)
  public void prepareEmitterRegistry() {
    var emitterRegistry = new EmitterRegistry();
    this.driver().serviceRegistry().registerProvider(EmitterRegistry.class, "ReportEmitterRegistry", emitterRegistry);
  }

  @ModuleTask(order = 64)
  public void registerDefaultEmitters() {
    var emitterRegistry = ServiceRegistry.first(EmitterRegistry.class);
    emitterRegistry
      // general emitters
      .registerEmitter(new SystemInfoDataEmitter())
      .registerEmitter(new ThreadInfoDataEmitter())
      .registerEmitter(new HeapDumpDataEmitter())
      .registerEmitter(new LocalNodeConfigDataEmitter(this.node))
      // specific class emitters
      .registerSpecificEmitter(NodeServer.class, new NodeServerDataEmitter(this.node))
      .registerSpecificEmitter(ModuleWrapper.class, new LocalModuleDataEmitter())
      .registerSpecificEmitter(ServiceTask.class, new ServiceTasksDataEmitter())
      .registerSpecificEmitter(GroupConfiguration.class, new GroupConfigDataEmitter())
      .registerSpecificEmitter(ServiceInfoSnapshot.class, new ServiceInfoDataEmitter());
  }

  @ModuleTask(order = 46)
  public void convertConfiguration() {
    var config = this.readConfig();
    if (config.contains("savingRecords")) {
      // old configuration, convert now
      this.writeConfig(JsonDocument.newDocument(new ReportConfiguration(Set.of(new PasteServer(
        "cloudnet",
        config.getString("pasteServerUrl"),
        "documents",
        "POST",
        Map.of(),
        "key")))));
    }
  }

  @ModuleTask
  public void finishStartup() {
    this.configuration = this.readConfig(
      ReportConfiguration.class,
      () -> new ReportConfiguration(Set.of(PasteServer.DEFAULT_PASTER_SERVER)));
   this.node.commandProvider().register(new ReportCommand(this));
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    this.configuration = this.readConfig(
      ReportConfiguration.class,
      () -> new ReportConfiguration(Set.of(PasteServer.DEFAULT_PASTER_SERVER)));
  }

  public @NonNull ReportConfiguration configuration() {
    return this.configuration;
  }
}
