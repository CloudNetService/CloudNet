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

package eu.cloudnetservice.node.network.chunk;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.event.events.chunk.ChunkedPacketSessionOpenEvent;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.chunk.defaults.DefaultFileChunkedPacketHandler;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import io.vavr.CheckedFunction2;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

@Singleton
public final class FileDeployCallbackListener {

  private final TemplateDeployCallback templateDeployCallback;
  private final StaticServiceDeployCallback serviceDeployCallback;
  private final TemplateFileDeployCallback templateFileDeployCallback;

  private final TemplateStorageProvider templateStorageProvider;

  @Inject
  public FileDeployCallbackListener(
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
    switch (event.session().transferChannel()) {
      case "deploy_service_template" -> event.handler(new DefaultFileChunkedPacketHandler(
        event.session(),
        this.templateDeployCallback));
      case "deploy_single_file" -> event.handler(new DefaultFileChunkedPacketHandler(
        event.session(),
        this.templateFileDeployCallback));
      case "deploy_static_service" -> event.handler(new DefaultFileChunkedPacketHandler(
        event.session(),
        this.serviceDeployCallback));
      default -> {
      }
    }
  }

  @EventListener
  public void handle(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)) {
      switch (event.message()) {
        case "remote_templates_zip_template" -> this.handleInputRequest(event, TemplateStorage::zipTemplate);
        case "remote_templates_template_file" -> {
          // read the path info first
          var path = event.content().readString();
          this.handleInputRequest(event, (storage, template) -> storage.newInputStream(template, path));
        }
        default -> {
        }
      }
    }
  }

  private void handleInputRequest(
    @NonNull ChannelMessageReceiveEvent event,
    @NonNull CheckedFunction2<TemplateStorage, ServiceTemplate, InputStream> streamOpener
  ) {
    // read the information
    var storageName = event.content().readString();
    var template = event.content().readObject(ServiceTemplate.class);
    var responseId = event.content().readUniqueId();

    // get the storage
    var storage = this.templateStorageProvider.templateStorage(storageName);
    if (storage == null) {
      // missing storage - no result
      event.binaryResponse(DataBuf.empty().writeBoolean(false));
      return;
    }

    // zip the template and return to the sender
    try (var zip = streamOpener.apply(storage, template)) {
      // check if the template exists
      if (zip == null) {
        event.binaryResponse(DataBuf.empty().writeBoolean(false));
        return;
      }

      // send the zip to the requesting component
      var status = ChunkedPacketSender.forFileTransfer()
        .source(zip)
        .sessionUniqueId(responseId)
        .toChannels(event.networkChannel())
        .transferChannel("request_template_file_result")
        .build()
        .transferChunkedData()
        .get(5, TimeUnit.MINUTES, TransferStatus.FAILURE);
      event.binaryResponse(DataBuf.empty().writeBoolean(status == TransferStatus.SUCCESS));
    } catch (Throwable exception) {
      event.binaryResponse(DataBuf.empty().writeBoolean(false));
    }
  }
}
