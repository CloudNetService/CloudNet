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

package de.dytanic.cloudnet.driver.network.http.content;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;

public class StaticContentHttpHandler implements IHttpHandler {

  private final ContentStreamProvider provider;

  public StaticContentHttpHandler(ContentStreamProvider provider) {
    this.provider = provider;
  }

  @Override
  public void handle(String path, IHttpContext context) throws Exception {
    path = path.replaceFirst(context.pathPrefix(), "");
    if (path.endsWith("/") || path.isEmpty()) {
      path += "index.html";
    }

    ContentStreamProvider.StreamableContent content = this.provider.provideContent(path);
    if (content != null) {
      context
        .closeAfter(true)
        .cancelNext(true)
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .header("Content-Type", content.contentType())
        .body(content.openStream());
    }
  }
}
