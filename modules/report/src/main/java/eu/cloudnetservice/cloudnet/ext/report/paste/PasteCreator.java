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

package eu.cloudnetservice.cloudnet.ext.report.paste;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.service.CloudService;
import eu.cloudnetservice.cloudnet.ext.report.config.PasteService;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.EmitterRegistry;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record PasteCreator(@NonNull PasteService pasteService, @NonNull EmitterRegistry registry) {

  /**
   * Creates a new paste by emitting all ICloudService emitters and collecting their data.
   *
   * @param service the service to collect the data for
   * @return the resulting url after uploading collected the content
   */
  public @Nullable String createServicePaste(@NonNull CloudService service) {
    return this.pasteContent(this.collectData(CloudService.class, service));
  }

  /**
   * Creates a new paste by emitting all NetworkClusterNodeInfoSnapshot emitters and collecting their data.
   *
   * @param nodeInfoSnapshot the nodeInfoSnapshot to collect the data for
   * @return the resulting url after uploading the collected content
   */
  public @Nullable String createNodePaste(@NonNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot) {
    return this.pasteContent(this.collectData(NetworkClusterNodeInfoSnapshot.class, nodeInfoSnapshot));
  }

  /**
   * Collects the data from every emitter that is registered with the given class
   *
   * @param clazz   the class the emitters are registered for
   * @param context the context used to collect the data
   * @param <T>     the type to collect the data for
   * @return the emitted data
   */
  public <T> @NonNull String collectData(Class<T> clazz, T context) {
    var content = new StringBuilder();
    for (var emitter : this.registry.emitters(clazz)) {
      emitter.emitData(content, context);
    }

    return content.toString();
  }

  /**
   * Pastes the given content to this {@link PasteService} and parses the resulting url
   *
   * @param content the content to upload to the paste service
   * @return the resulting url
   */
  private @Nullable String pasteContent(@NonNull String content) {
    return this.parsePasteServiceResponse(this.pasteService.pasteToService(content));
  }

  /**
   * Parses the access token from the response and rebuilds the final url for the uploaded content
   *
   * @param response the response
   * @return the final url of the paste
   */
  private @Nullable String parsePasteServiceResponse(@Nullable String response) {
    if (response == null) {
      return null;
    }

    var document = JsonDocument.fromJsonString(response);
    return String.format("%s/%s", this.pasteService.serviceUrl(), document.getString("key"));
  }
}
