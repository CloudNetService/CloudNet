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

package eu.cloudnetservice.node.impl.network.chunk;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.impl.network.chunk.DefaultFileChunkedPacketHandler;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.driver.network.chunk.event.ChunkedPacketSessionOpenEvent;
import eu.cloudnetservice.driver.network.chunk.event.FileQueryRequestEvent;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.function.BiConsumer;
import lombok.NonNull;

@Singleton
public final class FileDeployCallbackListener {

  private final TemplateDeployCallback templateDeployCallback;
  private final StaticServiceDeployCallback serviceDeployCallback;
  private final TemplateFileDeployCallback templateFileDeployCallback;

  private final TemplateStorageProvider templateStorageProvider;

  @Inject
  FileDeployCallbackListener(
    @NonNull TemplateDeployCallback templateDeployCallback,
    @NonNull StaticServiceDeployCallback serviceDeployCallback,
    @NonNull TemplateFileDeployCallback templateFileDeployCallback,
    @NonNull TemplateStorageProvider templateStorageProvider
  ) {
    this.templateDeployCallback = templateDeployCallback;
    this.serviceDeployCallback = serviceDeployCallback;
    this.templateFileDeployCallback = templateFileDeployCallback;
    this.templateStorageProvider = templateStorageProvider;
  }

  @EventListener
  public void handle(@NonNull ChunkedPacketSessionOpenEvent event) {
    var callback = switch (event.session().transferChannel()) {
      case "deploy_static_service" -> this.serviceDeployCallback;
      case "deploy_single_file" -> this.templateFileDeployCallback;
      case "deploy_service_template" -> this.templateDeployCallback;
      default -> null;
    };
    if (callback != null) {
      event.handler(new DefaultFileChunkedPacketHandler(event.session(), callback));
    }
  }

  @EventListener
  public void handle(@NonNull FileQueryRequestEvent event) {
    switch (event.dataId()) {
      case "remote_templates_zip_template" -> this.handleInputRequest(event.requestData(), (storage, template) -> {
        try {
          var zipInputStream = storage.zipTemplate(template);
          if (zipInputStream != null) {
            var responseHandler = ChunkedPacketSender.forStreamTransfer().source(zipInputStream);
            event.responseHandler(responseHandler);
          }
        } catch (IOException _) {
        }
      });
      case "remote_templates_template_file" -> this.handleInputRequest(event.requestData(), (storage, template) -> {
        try {
          var filePath = event.requestData().readString();
          var fileInputStream = storage.newInputStream(template, filePath);
          if (fileInputStream != null) {
            var responseHandler = ChunkedPacketSender.forStreamTransfer().source(fileInputStream);
            event.responseHandler(responseHandler);
          }
        } catch (IOException _) {
        }
      });
    }
  }

  private void handleInputRequest(
    @NonNull DataBuf requestData,
    @NonNull BiConsumer<TemplateStorage, ServiceTemplate> handler
  ) {
    var storageName = requestData.readString();
    var template = requestData.readObject(ServiceTemplate.class);
    var storage = this.templateStorageProvider.templateStorage(storageName);
    if (storage != null && storage.contains(template)) {
      handler.accept(storage, template);
    }
  }
}
