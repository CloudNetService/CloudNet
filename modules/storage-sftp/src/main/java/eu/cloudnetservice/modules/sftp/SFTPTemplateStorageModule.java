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

package eu.cloudnetservice.modules.sftp;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.cloudnet.driver.module.ModuleTask;
import eu.cloudnetservice.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import eu.cloudnetservice.cloudnet.driver.template.TemplateStorage;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.modules.sftp.config.SFTPTemplateStorageConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

public final class SFTPTemplateStorageModule extends DriverModule {

  private SFTPTemplateStorage storage;
  private volatile SFTPTemplateStorageConfig config;

  @ModuleTask(order = Byte.MAX_VALUE, event = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    // the old config was located in a directory called '-ftp' rather than '-sftp'
    var oldConfigPath = this.moduleWrapper().moduleProvider().moduleDirectoryPath()
      .resolve("CloudNet-Storage-FTP")
      .resolve("config.json");
    if (Files.exists(oldConfigPath)) {
      var config = JsonDocument.newDocument(oldConfigPath);
      // convert to the new config format
      this.updateConfig(new SFTPTemplateStorageConfig(
        config.get("address", HostAndPort.class),
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

  @ModuleTask(event = ModuleLifeCycle.LOADED)
  public void handleInit() {
    this.config = this.readConfig(SFTPTemplateStorageConfig.class, SFTPTemplateStorageConfig::new);
    // init the storage
    this.storage = new SFTPTemplateStorage(this.config);
    this.serviceRegistry().registerProvider(TemplateStorage.class, this.storage.name(), this.storage);
    // register the cluster sync handler
    CloudNet.instance().dataSyncRegistry().registerHandler(DataSyncHandler.<SFTPTemplateStorageConfig>builder()
      .key("sftp-storage-config")
      .nameExtractor($ -> "SFTP Template Storage Config")
      .convertObject(SFTPTemplateStorageConfig.class)
      .writer(this::updateConfig)
      .singletonCollector(() -> this.config)
      .currentGetter($ -> this.config)
      .build());
  }

  @ModuleTask(event = ModuleLifeCycle.STOPPED)
  public void handleStop() throws IOException {
    this.storage.close();
    this.serviceRegistry().unregisterProvider(TemplateStorage.class, this.storage.name());
  }

  public void updateConfig(@NonNull SFTPTemplateStorageConfig config) {
    this.config = config;
    this.writeConfig(JsonDocument.newDocument(config));
  }
}
