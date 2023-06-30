/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.rest.v2;

import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.RequestPath;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Objects;
import lombok.NonNull;

@Singleton
@ApplyHeaders
public final class V2HttpHandlerDocumentation extends V2HttpHandler {

  @HttpRequestHandler(paths = "/api/v2/documentation")
  private void handleDocumentationRequest(@NonNull HttpContext context) {
    var pathPrefix = context.pathPrefix().endsWith("/")
      ? context.pathPrefix()
      : context.pathPrefix() + "/";

    context.response()
      .status(HttpResponseCode.MOVED_PERMANENTLY)
      .header("Location", pathPrefix + "index.html")
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @HttpRequestHandler(paths = "/api/v2/documentation/*")
  private void handleDocumentationFileRequest(
    @NonNull HttpContext context,
    @RequestPath String path
  ) throws IOException {
    var filePath = path.replaceFirst(context.pathPrefix(), "");
    if (filePath.contains("..")) {
      context.response()
        .status(HttpResponseCode.BAD_REQUEST)
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    var resourcePath = "documentation/" + filePath;

    // get the resource and the content type of it
    var resource = V2HttpHandlerDocumentation.class.getClassLoader().getResource(resourcePath);
    var contentType = Objects.requireNonNullElse(
      URLConnection.guessContentTypeFromName(resourcePath),
      "application/octet-stream");

    // check if the resource exists
    if (resource == null) {
      context.response()
        .status(HttpResponseCode.NOT_FOUND)
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    context
      .response()
      .status(HttpResponseCode.OK)
      .header("Content-Type", contentType)
      .body(resource.openStream())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }
}
