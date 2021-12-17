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

package eu.cloudnetservice.cloudnet.ext.report.config;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record PasteService(@NonNull String name, @NonNull String serviceUrl) implements INameable {

  public static final PasteService FALLBACK = new PasteService("fallback", "https://just-paste.it");
  private static final Logger LOGGER = LogManager.logger(PasteService.class);

  public PasteService(@NonNull String name, @NonNull String serviceUrl) {
    this.name = name;
    this.serviceUrl = serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
  }

  /**
   * Uploads the given content to this paste services
   *
   * @param content the content to upload to this service
   * @return the result of the upload - null if the content is empty or the upload failed
   */
  public @Nullable String pasteToService(@NonNull String content) {
    if (content.trim().isEmpty()) {
      return null;
    }

    try {
      var response = Unirest.post(String.format("%s/documents", this.serviceUrl))
        .contentType("application/json")
        .connectTimeout(5_000)
        .body(content)
        .asString();
      if (response.isSuccess()) {
        return response.getBody();
      }
    } catch (UnirestException exception) {
      LOGGER.severe("Unable to paste content to %s", exception, this.serviceUrl);
    }

    return null;
  }
}
