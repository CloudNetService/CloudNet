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

import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketHandler.Callback;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import java.io.InputStream;
import org.jetbrains.annotations.NotNull;

final class TemplateDeployCallback implements Callback {

  public static final TemplateDeployCallback INSTANCE = new TemplateDeployCallback();

  private TemplateDeployCallback() {
  }

  @Override
  public void handleSessionComplete(
    @NotNull ChunkSessionInformation information,
    @NotNull InputStream dataInput
  ) {
    // get the information for the deployment
    ServiceTemplate template = information.getTransferInformation().readObject(ServiceTemplate.class);
    boolean overrideTemplate = information.getTransferInformation().readBoolean();
    // get the storage of the template if present
    SpecificTemplateStorage storage = template.nullableStorage();
    if (storage != null) {
      // delete the template if requested
      if (overrideTemplate) {
        storage.delete();
      }
      // deploy the data into the template
      storage.deploy(dataInput);
    }
  }
}
