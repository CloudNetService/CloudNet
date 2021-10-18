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

import com.github.derklaro.requestbuilder.RequestBuilder;
import com.github.derklaro.requestbuilder.method.RequestMethod;
import com.github.derklaro.requestbuilder.result.RequestResult;
import com.github.derklaro.requestbuilder.types.MimeTypes;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PasteService implements INameable {

  private static final Logger LOGGER = LogManager.getLogger(PasteService.class);

  private final String name;
  private final String serviceUrl;

  public PasteService(@NotNull String name, @NotNull String serviceUrl) {
    this.name = name;
    this.serviceUrl = serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
  }

  /**
   * @return the name of this PasteService - the user can use this to decide where to upload a paste
   */
  @Override
  public @NotNull String getName() {
    return this.name;
  }

  /**
   * @return the url of the service without trailing '/'
   */
  public @NotNull String getServiceUrl() {
    return this.serviceUrl;
  }

  /**
   * Uploads the given content to this paste services
   *
   * @param content the content to upload to this service
   * @return the result of the upload - null if the content is empty or the upload failed
   */
  public @Nullable String pasteToService(@NotNull String content) {
    if (content.trim().isEmpty()) {
      return null;
    }

    RequestBuilder requestBuilder = RequestBuilder.newBuilder(String.format("%s/documents", this.serviceUrl))
      .requestMethod(RequestMethod.POST)
      .mimeType(MimeTypes.getMimeType("json"))
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(5, TimeUnit.SECONDS)
      .enableOutput()
      .addHeader("content-length", String.valueOf(content.getBytes(StandardCharsets.UTF_8).length))
      .addBody(content);

    try (RequestResult result = requestBuilder.fire()) {
      if (result.getStatusCode() >= 200 && result.getStatusCode() < 300) {
        return result.getSuccessResultAsString();
      }
    } catch (Exception exception) {
      LOGGER.severe("Unable to paste content to %s", exception, this.serviceUrl);
    }

    return null;
  }
}
