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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public final class TemplateDeployCallbackListener {

  @EventListener
  public void handle(@NotNull ChunkedPacketSessionOpenEvent event) {
    switch (event.getSession().getTransferChannel()) {
      case "deploy_service_template":
        event.setHandler(new DefaultFileChunkedPacketHandler(event.getSession(), TemplateDeployCallback.INSTANCE));
        break;
      case "deploy_single_file":
        event.setHandler(new DefaultFileChunkedPacketHandler(event.getSession(), TemplateFileDeployCallback.INSTANCE));
        break;
      default:
        break;
    }
  }

  @EventListener
  public void handle(@NotNull ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL) && event.getMessage() != null) {
      switch (event.getMessage()) {
        case "remote_templates_zip_template":
          this.handleInputRequest(event, TemplateStorage::zipTemplate);
          break;
        case "remote_templates_template_file":
          // read the path info first
          String path = event.getContent().readString();
          this.handleInputRequest(event, (storage, template) -> storage.newInputStream(template, path));
          break;
        default:
          break;
      }
    }
  }

  private void handleInputRequest(
    @NotNull ChannelMessageReceiveEvent event,
    @NotNull ThrowableBiFunction<TemplateStorage, ServiceTemplate, InputStream, IOException> streamOpener
  ) {
    // read the information
    String storageName = event.getContent().readString();
    ServiceTemplate template = event.getContent().readObject(ServiceTemplate.class);
    UUID responseId = event.getContent().readUniqueId();
    // get the storage
    TemplateStorage storage = CloudNet.getInstance().getTemplateStorage(storageName);
    if (storage == null) {
      // missing storage - no result
      event.setBinaryResponse(DataBuf.empty().writeBoolean(false));
      return;
    }
    // zip the template and return to the sender
    try (InputStream zip = streamOpener.apply(storage, template)) {
      // check if the template exists
      if (zip == null) {
        event.setBinaryResponse(DataBuf.empty().writeBoolean(false));
        return;
      }
      // send the zip to the requesting component
      TransferStatus status = ChunkedPacketSender.forFileTransfer()
        .source(zip)
        .sessionUniqueId(responseId)
        .toChannels(event.getNetworkChannel())
        .transferChannel("request_template_file_result")
        .build()
        .transferChunkedData()
        .get(5, TimeUnit.MINUTES, TransferStatus.FAILURE);
      event.setBinaryResponse(DataBuf.empty().writeBoolean(status == TransferStatus.SUCCESS));
    } catch (IOException exception) {
      event.setBinaryResponse(DataBuf.empty().writeBoolean(false));
    }
  }
}
