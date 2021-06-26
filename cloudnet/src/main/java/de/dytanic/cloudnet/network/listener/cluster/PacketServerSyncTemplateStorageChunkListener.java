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

package de.dytanic.cloudnet.network.listener.cluster;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.CachedChunkedPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.ChunkedPacketSession;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.template.ClusterSynchronizedTemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.jetbrains.annotations.NotNull;

public class PacketServerSyncTemplateStorageChunkListener extends CachedChunkedPacketListener {

  private final boolean redirectToCluster;

  public PacketServerSyncTemplateStorageChunkListener(boolean redirectToCluster) {
    this.redirectToCluster = redirectToCluster;
  }

  @Override
  protected void handleComplete(@NotNull ChunkedPacketSession session, @NotNull InputStream inputStream)
    throws IOException {
    ServiceTemplate template = session.getHeader().get("template", ServiceTemplate.class);
    DriverAPIRequestType requestType = session.getHeader().get("type", DriverAPIRequestType.class);

    TemplateStorage storage = CloudNet.getInstance().getTemplateStorage(template.getStorage());
    Preconditions.checkNotNull(storage, "invalid storage %s", template.getStorage());

    if (!this.redirectToCluster && !(storage instanceof ClusterSynchronizedTemplateStorage)) {
      throw new IllegalArgumentException("TemplateStorage " + template.getStorage() + " is not cluster synchronized");
    }

    switch (requestType) {
      case SET_FILE_CONTENT:
        try (OutputStream outputStream = this.redirectToCluster ?
          storage.newOutputStream(template, session.getHeader().getString("path")) :
          ((ClusterSynchronizedTemplateStorage) storage)
            .newOutputStreamWithoutSynchronization(template, session.getHeader().getString("path"))) {
          FileUtils.copy(inputStream, outputStream);
        }
        break;

      case APPEND_FILE_CONTENT:
        try (OutputStream outputStream = this.redirectToCluster ?
          storage.appendOutputStream(template, session.getHeader().getString("path")) :
          ((ClusterSynchronizedTemplateStorage) storage)
            .appendOutputStreamWithoutSynchronization(template, session.getHeader().getString("path"))) {
          FileUtils.copy(inputStream, outputStream);
        }
        break;

      case DEPLOY_TEMPLATE_STREAM:
        boolean success = this.redirectToCluster ? storage.deploy(inputStream, template)
          : ((ClusterSynchronizedTemplateStorage) storage).deployWithoutSynchronization(inputStream, template);
        session.getChannel().sendPacket(
          Packet.createResponseFor(session.getFirstPacket(), ProtocolBuffer.create().writeBoolean(success)));
        break;

      default:
        inputStream.close();
        throw new IllegalStateException("Unexpected value: " + requestType);
    }

    inputStream.close();
  }
}
