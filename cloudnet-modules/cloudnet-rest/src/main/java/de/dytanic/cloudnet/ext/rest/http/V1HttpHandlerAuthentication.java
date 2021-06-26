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

package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

public final class V1HttpHandlerAuthentication extends V1HttpHandler {

  public V1HttpHandlerAuthentication() {
    super(null);
  }

  @Override
  public void handle(String path, IHttpContext context) throws Exception {
    if (context.request().method().equalsIgnoreCase("OPTIONS")) {
      this.sendOptions(context, "OPTIONS, GET");
      return;
    }

    if (HTTP_SESSION.auth(context)) {
      if (context.request().queryParameters().containsKey("redirect")) {
        context
          .response()
          .statusCode(HttpResponseCode.HTTP_MOVED_TEMP)
          .header("Location", context.request().queryParameters().get("redirect").iterator().next())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        context
          .response()
          .statusCode(HttpResponseCode.HTTP_OK)
          .header("Content-Type", "application/json")
          .body(new JsonDocument("success", true).append("userUniqueId", HTTP_SESSION.getUser(context).getUniqueId())
            .toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    } else {
      context
        .response()
        .statusCode(HttpResponseCode.HTTP_UNAUTHORIZED)
        .header("WWW-Authenticate", "Basic realm=\"CloudNet-REST-v1\"")
        .context()
        .closeAfter(true)
        .cancelNext();
    }

    super.handle(path, context);
  }
}
