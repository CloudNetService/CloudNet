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
import de.dytanic.cloudnet.service.ICloudService;
import eu.cloudnetservice.cloudnet.ext.report.config.PasteService;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.EmitterRegistry;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.ReportDataEmitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PasteCreator {

  private final PasteService pasteService;
  private final EmitterRegistry registry;

  public PasteCreator(@NotNull PasteService pasteService, @NotNull EmitterRegistry registry) {
    this.pasteService = pasteService;
    this.registry = registry;
  }

  public @Nullable String createServicePaste(@NotNull ICloudService service) {
    return this.pasteContent(this.collectData(ICloudService.class, service));
  }

  public @Nullable String createNodePaste(@NotNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot) {
    return this.pasteContent(this.collectData(NetworkClusterNodeInfoSnapshot.class, nodeInfoSnapshot));
  }

  public <T> @NotNull String collectData(Class<T> clazz, T data) {
    StringBuilder content = new StringBuilder();
    for (ReportDataEmitter<T> emitter : this.registry.getEmitters(clazz)) {
      emitter.emitData(content, data);
    }

    return content.toString();
  }

  private @Nullable String pasteContent(@NotNull String content) {
    return this.parsePasteServiceResponse(this.pasteService.pasteToService(content));
  }

  private @Nullable String parsePasteServiceResponse(@Nullable String response) {
    if (response == null) {
      return null;
    }

    JsonDocument document = JsonDocument.newDocument(response);
    return String.format("%s/%s", this.pasteService.getServiceUrl(), document.getString("key"));
  }
}
