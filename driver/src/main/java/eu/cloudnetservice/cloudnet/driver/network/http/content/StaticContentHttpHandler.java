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

package eu.cloudnetservice.cloudnet.driver.network.http.content;

import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpHandler;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponseCode;
import lombok.NonNull;

/**
 * A http handler which provides the requested content from the http request uri using the given content stream
 * provider.
 *
 * @param provider the content stream provider which serves the requested content.
 * @since 4.0
 */
public record StaticContentHttpHandler(@NonNull ContentStreamProvider provider) implements HttpHandler {

  /**
   * {@inheritDoc}
   */
  @Override
  public void handle(@NonNull String path, @NonNull HttpContext context) throws Exception {
    path = path.replaceFirst(context.pathPrefix(), "");
    if (path.endsWith("/") || path.isEmpty()) {
      var pathPrefix = context.pathPrefix().endsWith("/")
        ? context.pathPrefix()
        : context.pathPrefix() + "/";

      context
        .closeAfter(true)
        .cancelNext(true)
        .response()
        .status(HttpResponseCode.MOVED_PERMANENTLY)
        .header("Location", pathPrefix + "index.html");
      return;
    }

    var content = this.provider.provideContent(path);
    if (content != null) {
      context
        .closeAfter(true)
        .cancelNext(true)
        .response()
        .status(HttpResponseCode.OK)
        .header("Content-Type", content.contentType())
        .body(content.openStream());
    }
  }
}
