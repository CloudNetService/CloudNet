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
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketHandler.Callback;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.io.InputStream;
import lombok.NonNull;

final class TemplateDeployCallback implements Callback {

  public static final TemplateDeployCallback INSTANCE = new TemplateDeployCallback();

  private TemplateDeployCallback() {
  }

  @Override
  public void handleSessionComplete(
    @NonNull ChunkSessionInformation information,
    @NonNull InputStream dataInput
  ) {
    // get the information for the deployment
    var storageName = information.transferInformation().readString();
    var template = information.transferInformation().readObject(ServiceTemplate.class);
    var overrideTemplate = information.transferInformation().readBoolean();
    // get the storage of the template if present
    var storage = CloudNet.instance().templateStorage(storageName);
    if (storage != null) {
      // pause the ticking of CloudNet before writing the file into the template
      CloudNet.instance().mainThread().pause();
      try {
        // delete the template if requested
        if (overrideTemplate) {
          storage.delete(template);
        }
        // deploy the data into the template
        storage.deploy(dataInput, template);
      } finally {
        // resume the main thread execution
        CloudNet.instance().mainThread().resume();
      }
    }
  }
}
