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

package eu.cloudnetservice.node.http;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpRequest;
import eu.cloudnetservice.driver.network.http.HttpResponse;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;

public abstract class V2HttpHandler {

  protected void send403(@NonNull HttpContext context, @NonNull String reason) {
    this.response(context, HttpResponseCode.FORBIDDEN)
      .body(this.failure().append("reason", reason).toString().getBytes(StandardCharsets.UTF_8))
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  protected void send401(@NonNull HttpContext context, @NonNull String reason) {
    this.response(context, HttpResponseCode.UNAUTHORIZED)
      .body(this.failure().append("reason", reason).toString().getBytes(StandardCharsets.UTF_8))
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  protected @NonNull HttpResponse ok(@NonNull HttpContext context) {
    return this.response(context, HttpResponseCode.OK);
  }

  protected @NonNull HttpResponse badRequest(@NonNull HttpContext context) {
    return this.response(context, HttpResponseCode.BAD_REQUEST);
  }

  protected @NonNull HttpResponse notFound(@NonNull HttpContext context) {
    return this.response(context, HttpResponseCode.NOT_FOUND);
  }

  protected @NonNull HttpResponse response(@NonNull HttpContext context, @NonNull HttpResponseCode statusCode) {
    return context.response()
      .status(statusCode)
      .header("Content-Type", "application/json");
  }

  protected @NonNull Document.Mutable body(@NonNull HttpRequest request) {
    return DocumentFactory.json().parse(request.body());
  }

  protected @NonNull Document.Mutable success() {
    return Document.newJsonDocument().append("success", true);
  }

  protected @NonNull Document.Mutable failure() {
    return Document.newJsonDocument().append("success", false);
  }
}
