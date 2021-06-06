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

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.CachedChunkedPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.ChunkedPacketSession;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.template.ClusterSynchronizedTemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import org.jetbrains.annotations.NotNull;

public final class PacketServerDeployLocalTemplateListener extends CachedChunkedPacketListener {

  @Override
  protected void handleComplete(@NotNull ChunkedPacketSession session, @NotNull InputStream inputStream)
    throws IOException {
    TemplateStorage storage = CloudNetDriver.getInstance().getLocalTemplateStorage();
    ServiceTemplate template = session.getHeader().get("template", ServiceTemplate.class);
    boolean preClear = session.getHeader().getBoolean("preClear");

    try {
      this.deployToStorage(storage, template, inputStream, preClear);
    } finally {
      inputStream.close();
    }
  }

  protected void deployToStorage(TemplateStorage templateStorage, ServiceTemplate template, InputStream stream,
    boolean preClear) {
    if (templateStorage instanceof ClusterSynchronizedTemplateStorage) {
      ClusterSynchronizedTemplateStorage synchronizedStorage = (ClusterSynchronizedTemplateStorage) templateStorage;
      if (preClear) {
        synchronizedStorage.deleteWithoutSynchronization(template);
      }

      synchronizedStorage.deployWithoutSynchronization(stream, template);
    } else {
      if (preClear) {
        templateStorage.delete(template);
      }

      templateStorage.deploy(stream, template);
    }
  }
}
