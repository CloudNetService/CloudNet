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

import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerPermission;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.NonNull;

@Singleton
@HandlerPermission("http.v2.database")
@ApplyHeaders
public final class V2HttpHandlerDatabase extends V2HttpHandler {

  private static final Type MAP_TYPE = TypeFactory.parameterizedClass(Map.class, String.class, String.class);

  private final DatabaseProvider databaseProvider;

  @Inject
  public V2HttpHandlerDatabase(@NonNull DatabaseProvider databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/database")
  private void handleNamesRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("names", this.databaseProvider.databaseNames()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/database/{name}/clear")
  private void handleClearRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("name") String name) {
    var database = this.databaseProvider.database(name);
    database.clear();

    this.ok(context)
      .body(this.success().toString())
      .context().closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/database/{name}/contains")
  private void handleContainsRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("key") String key
  ) {
    var database = this.databaseProvider.database(name);
    this.ok(context)
      .body(this.success().append("result", database.contains(key)).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/database/{name}/keys")
  private void handleKeysRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("name") String name) {
    var database = this.databaseProvider.database(name);
    this.ok(context)
      .body(this.success().append("keys", database.keys()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/database/{name}/count")
  private void handleCountRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("name") String name) {
    var database = this.databaseProvider.database(name);
    this.ok(context)
      .body(this.success().append("count", database.documentCount()).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/database/{name}", methods = "POST")
  private void handleInsertRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @RequestBody Document document
  ) {
    var database = this.databaseProvider.database(name);
    this.withContextData(context, document, (key, data) -> {
      if (database.insert(key, data)) {
        this.ok(context)
          .body(this.success().toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      } else {
        this.ok(context)
          .body(this.failure().toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      }
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/database/{name}/get", methods = "POST")
  private void handleGetRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("key") String key
  ) {
    var database = this.databaseProvider.database(name);
    this.ok(context)
      .body(this.success().append("result", database.get(key)).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "api/v2/database/{name}/find", methods = "POST")
  private void handleFindRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @RequestBody Document body
  ) {
    var database = this.databaseProvider.database(name);

    var result = database.find(body.toInstanceOf(MAP_TYPE));
    this.ok(context)
      .body(this.success().append("result", result).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/database/{name}", methods = "DELETE")
  private void handleDeleteRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("name") String name,
    @NonNull @FirstRequestQueryParam("key") String key
  ) {
    var database = this.databaseProvider.database(name);
    if (database.delete(key)) {
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.ok(context)
        .body(this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    }
  }

  private void withContextData(
    @NonNull HttpContext context,
    @NonNull Document body,
    @NonNull BiConsumer<String, Document> handler
  ) {
    var key = body.getString("key");
    var data = body.readDocument("document");

    if (key == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing key").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    handler.accept(key, data);
  }
}
