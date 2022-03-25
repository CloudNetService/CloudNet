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

package eu.cloudnetservice.cloudnet.node.network.chunk;

import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.driver.network.chunk.ChunkedPacketHandler.Callback;
import eu.cloudnetservice.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;

final class TemplateFileDeployCallback implements Callback {

  public static final TemplateFileDeployCallback INSTANCE = new TemplateFileDeployCallback();

  private TemplateFileDeployCallback() {
  }

  @Override
  public void handleSessionComplete(
    @NonNull ChunkSessionInformation information,
    @NonNull InputStream dataInput
  ) throws IOException {
    // read the information
    var storageName = information.transferInformation().readString();
    var template = information.transferInformation().readObject(ServiceTemplate.class);
    var path = information.transferInformation().readString();
    var append = information.transferInformation().readBoolean();
    // get the storage for the template
    var storage = CloudNet.instance().templateStorageProvider().templateStorage(storageName);
    if (storage != null) {
      // open the stream and write the data to it
      try (var out = append
        ? storage.appendOutputStream(template, path)
        : storage.newOutputStream(template, path)
      ) {
        FileUtil.copy(dataInput, out);
      }
    }
  }
}
