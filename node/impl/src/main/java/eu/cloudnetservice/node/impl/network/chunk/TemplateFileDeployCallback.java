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

import eu.cloudnetservice.driver.network.chunk.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketHandler;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.utils.base.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;

@Singleton
final class TemplateFileDeployCallback implements ChunkedPacketHandler.Callback {

  private final TemplateStorageProvider templateStorageProvider;

  @Inject
  public TemplateFileDeployCallback(@NonNull TemplateStorageProvider templateStorageProvider) {
    this.templateStorageProvider = templateStorageProvider;
  }

  @Override
  public boolean handleSessionComplete(
    @NonNull ChunkSessionInformation information,
    @NonNull InputStream dataInput
  ) throws IOException {
    // read the information
    var storageName = information.transferInformation().readString();
    var template = information.transferInformation().readObject(ServiceTemplate.class);
    var path = information.transferInformation().readString();
    var append = information.transferInformation().readBoolean();

    // get the storage for the template
    var storage = this.templateStorageProvider.templateStorage(storageName);
    if (storage != null) {
      // open the stream and write the data to it
      try (var out = append
        ? storage.appendOutputStream(template, path)
        : storage.newOutputStream(template, path)
      ) {
        FileUtil.copy(dataInput, out);
      }
    }

    return true;
  }
}
