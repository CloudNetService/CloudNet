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

package de.dytanic.cloudnet.network.chunk;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.function.ThrowableBiFunction;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.chunk.ChunkedPacketSessionOpenEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketSender;
import de.dytanic.cloudnet.driver.network.chunk.TransferStatus;
import de.dytanic.cloudnet.driver.network.chunk.defaults.DefaultFileChunkedPacketHandler;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public final class FileDeployCallbackListener {

  @EventListener
  public void handle(@NotNull ChunkedPacketSessionOpenEvent event) {
    switch (event.session().transferChannel()) {
      case "deploy_service_template" -> event.handler(
          new DefaultFileChunkedPacketHandler(event.session(), TemplateDeployCallback.INSTANCE));
      case "deploy_single_file" -> event.handler(
          new DefaultFileChunkedPacketHandler(event.session(), TemplateFileDeployCallback.INSTANCE));
      case "deploy_static_service" -> event.handler(
          new DefaultFileChunkedPacketHandler(event.session(), StaticServiceDeployCallback.INSTANCE));
      default -> {
      }
    }
  }

  @EventListener
  public void handle(@NotNull ChannelMessageReceiveEvent event) {
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
    @NotNull ChannelMessageReceiveEvent event,
    @NotNull ThrowableBiFunction<TemplateStorage, ServiceTemplate, InputStream, IOException> streamOpener
  ) {
    // read the information
    var storageName = event.content().readString();
    var template = event.content().readObject(ServiceTemplate.class);
    var responseId = event.content().readUniqueId();
    // get the storage
    var storage = CloudNet.instance().templateStorage(storageName);
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
    } catch (IOException exception) {
      event.binaryResponse(DataBuf.empty().writeBoolean(false));
    }
  }
}
