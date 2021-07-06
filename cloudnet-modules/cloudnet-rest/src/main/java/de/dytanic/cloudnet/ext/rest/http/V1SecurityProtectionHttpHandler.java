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

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

public final class V1SecurityProtectionHttpHandler extends V1HttpHandler {

  public V1SecurityProtectionHttpHandler() {
    super(null);
  }

  @Override
  public void handle(String path, IHttpContext context) throws Exception {
    if (path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/logout")) {
      return;
    }

    if (!HTTP_SESSION.isAuthorized(context)) {
      context
        .response()
        .statusCode(HttpResponseCode.HTTP_MOVED_TEMP)
        .header("Location", "/api/v1/auth?redirect=" + context.request().uri())
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }
}
