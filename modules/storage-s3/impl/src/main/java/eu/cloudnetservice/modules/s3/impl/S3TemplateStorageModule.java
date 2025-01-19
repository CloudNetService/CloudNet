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

package eu.cloudnetservice.modules.s3.impl;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.modules.s3.config.S3TemplateStorageConfig;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class S3TemplateStorageModule extends DriverModule {

  private S3TemplateStorage storage;
  private volatile S3TemplateStorageConfig config;

  @ModuleTask(lifecycle = ModuleLifeCycle.LOADED)
  public void handleInit(@NonNull ServiceRegistry serviceRegistry, @NonNull DataSyncRegistry dataSyncRegistry) {
    this.config = this.readConfig(
      S3TemplateStorageConfig.class,
      () -> new S3TemplateStorageConfig(
        "s3",
        "cloudnet",
        "eu-west-1",
        "key",
        "secret",
        null,
        false,
        false,
        true,
        true,
        false),
      DocumentFactory.json());
    // init the storage
    this.storage = new S3TemplateStorage(this);
    serviceRegistry.registerProvider(TemplateStorage.class, this.config.name(), this.storage);
    // register the cluster sync handler
    dataSyncRegistry.registerHandler(DataSyncHandler.<S3TemplateStorageConfig>builder()
      .key("s3-storage-config")
      .nameExtractor($ -> "S3 Template Storage Config")
      .convertObject(S3TemplateStorageConfig.class)
      .writer(this::writeConfig)
      .singletonCollector(() -> this.config)
      .currentGetter($ -> this.config)
      .build());
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
  public void handleStop(@NonNull ServiceRegistry serviceRegistry) {
    this.storage.close();
    var registration = serviceRegistry.registration(TemplateStorage.class, this.storage.name());
    if (registration != null) {
      registration.unregister();
    }
  }

  public void writeConfig(@NonNull S3TemplateStorageConfig config) {
    this.config = config;
    this.writeConfig(Document.newJsonDocument().appendTree(config));
  }

  public @NonNull S3TemplateStorageConfig config() {
    return this.config;
  }
}
