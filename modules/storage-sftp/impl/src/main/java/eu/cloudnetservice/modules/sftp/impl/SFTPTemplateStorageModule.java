/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.sftp.impl;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.modules.sftp.config.SFTPTemplateStorageConfig;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.utils.base.io.FileUtil;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

@Singleton
public final class SFTPTemplateStorageModule extends DriverModule {

  private SFTPTemplateStorage storage;
  private volatile SFTPTemplateStorageConfig config;

  @ModuleTask(order = Byte.MAX_VALUE, lifecycle = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    // the old config was located in a directory called '-ftp' rather than '-sftp'
    var oldConfigPath = this.moduleWrapper().moduleProvider().moduleDirectoryPath()
      .resolve("CloudNet-Storage-FTP")
      .resolve("config.json");
    if (Files.exists(oldConfigPath)) {
      // convert to the new config format
      var config = DocumentFactory.json().parse(oldConfigPath);
      this.updateConfig(new SFTPTemplateStorageConfig(
        config.readObject("address", HostAndPort.class),
        config.getString("storage"),
        config.getString("username"),
        config.getString("password"),
        config.getString("sshKeyPath") == null ? null : Path.of(config.getString("sshKeyPath")),
        config.getString("sshKeyPassword") == null ? null : config.getString("sshKeyPassword"),
        null,
        config.getString("baseDirectory"),
        4));
      // remove the old directory
      FileUtil.delete(oldConfigPath.getParent());
    }
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.LOADED)
  public void handleInit(@NonNull ServiceRegistry serviceRegistry, @NonNull DataSyncRegistry dataSyncRegistry) {
    this.config = this.readConfig(
      SFTPTemplateStorageConfig.class,
      SFTPTemplateStorageConfig::new,
      DocumentFactory.json());
    // init the storage
    this.storage = new SFTPTemplateStorage(this.config);
    serviceRegistry.registerProvider(TemplateStorage.class, this.storage.name(), this.storage);
    // register the cluster sync handler
    dataSyncRegistry.registerHandler(DataSyncHandler.<SFTPTemplateStorageConfig>builder()
      .key("sftp-storage-config")
      .nameExtractor($ -> "SFTP Template Storage Config")
      .convertObject(SFTPTemplateStorageConfig.class)
      .writer(this::updateConfig)
      .singletonCollector(() -> this.config)
      .currentGetter($ -> this.config)
      .build());
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
  public void handleStop(@NonNull ServiceRegistry serviceRegistry) throws IOException {
    this.storage.close();
    var registration = serviceRegistry.registration(TemplateStorage.class, this.storage.name());
    if (registration != null) {
      registration.unregister();
    }
  }

  public void updateConfig(@NonNull SFTPTemplateStorageConfig config) {
    this.config = config;
    this.writeConfig(Document.newJsonDocument().appendTree(config));
  }
}
