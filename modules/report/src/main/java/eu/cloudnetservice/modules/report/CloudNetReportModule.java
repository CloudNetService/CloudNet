/*
 * Copyright 2019-2023 CloudNetService team & contributors
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
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.command.CommandProvider;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

@Singleton
public final class CloudNetReportModule extends DriverModule {

  private ReportConfiguration configuration;

  @ModuleTask(order = 127)
  public void prepareEmitterRegistry(@NonNull ServiceRegistry serviceRegistry) {
    var emitterRegistry = new EmitterRegistry();
    serviceRegistry.registerProvider(EmitterRegistry.class, "ReportEmitterRegistry", emitterRegistry);
  }

  @ModuleTask(order = 64)
  public void registerDefaultEmitters() {
    var emitterRegistry = ServiceRegistry.first(EmitterRegistry.class);
    emitterRegistry
      // general emitters
      .registerEmitter(SystemInfoDataEmitter.class)
      .registerEmitter(ThreadInfoDataEmitter.class)
      .registerEmitter(HeapDumpDataEmitter.class)
      .registerEmitter(LocalNodeConfigDataEmitter.class)
      // specific class emitters
      .registerSpecificEmitter(NodeServer.class, NodeServerDataEmitter.class)
      .registerSpecificEmitter(ModuleWrapper.class, LocalModuleDataEmitter.class)
      .registerSpecificEmitter(ServiceTask.class, ServiceTasksDataEmitter.class)
      .registerSpecificEmitter(GroupConfiguration.class, GroupConfigDataEmitter.class)
      .registerSpecificEmitter(ServiceInfoSnapshot.class, ServiceInfoDataEmitter.class);
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
  public void finishStartup(@NonNull CommandProvider commandProvider) {
    this.configuration = this.readConfig(
      ReportConfiguration.class,
      () -> new ReportConfiguration(Set.of(PasteServer.DEFAULT_PASTER_SERVER)));
    commandProvider.register(ReportCommand.class);
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    this.configuration = this.readConfig(
      ReportConfiguration.class,
      () -> new ReportConfiguration(Set.of(PasteServer.DEFAULT_PASTER_SERVER)));
  }

  public @NonNull ReportConfiguration configuration() {
    return this.configuration;
  }
}
