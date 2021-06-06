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

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.MethodHttpHandlerAdapter;
import java.io.InputStream;

public final class V1HttpHandlerShowOpenAPI extends MethodHttpHandlerAdapter {

  @Override
  public void handleGet(String path, IHttpContext context) throws Exception {
    try (InputStream inputStream = V1HttpHandlerShowOpenAPI.class.getClassLoader()
      .getResourceAsStream("openapi/v1-openapi.yml")) {
      if (inputStream != null) {
        context
          .response()
          .statusCode(HttpResponseCode.HTTP_OK)
          .header("Content-Type", "text/plain")
          .body(FileUtils.toByteArray(inputStream))
          .context()
          .closeAfter(true)
          .cancelNext()
        ;
      }
    }
  }
}
