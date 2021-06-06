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
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class V1HttpHandlerDatabase extends V1HttpHandler {

  public V1HttpHandlerDatabase(String permission) {
    super(permission);
  }

  private AbstractDatabaseProvider getDatabaseProvider() {
    return this.getCloudNet().getDatabaseProvider();
  }

  @Override
  public void handleOptions(String path, IHttpContext context) {
    super.sendOptions(context, "GET, DELETE, POST");
  }

  @Override
  public void handleGet(String path, IHttpContext context) {
    Database database = this.getDatabaseProvider().getDatabase(context.request().pathParameters().get("name"));

    context
      .response()
      .header("Content-Type", "application/json")
      .statusCode(HttpResponseCode.HTTP_OK);

    if (context.request().pathParameters().containsKey("key")) {
      context
        .response()
        .body(database.contains(context.request().pathParameters().get("key")) ?
          database.get(context.request().pathParameters().get("key")).toJson()
          :
            new JsonDocument().toJson()
        );

    } else {
      Map<String, String> queryFilters = new HashMap<>();

      for (Map.Entry<String, List<String>> queryEntry : context.request().queryParameters().entrySet()) {
        queryFilters.put(queryEntry.getKey(), queryEntry.getValue().get(0));
      }

      context
        .response()
        .body(GSON.toJson(database.get(new JsonDocument(queryFilters))))
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }

  @Override
  public void handlePost(String path, IHttpContext context) {
    Database database = this.getDatabaseProvider().getDatabase(context.request().pathParameters().get("name"));

    context
      .response()
      .header("Content-Type", "application/json")
      .statusCode(HttpResponseCode.HTTP_OK)
      .context()
      .closeAfter(true)
      .cancelNext();

    if (context.request().pathParameters().containsKey("key")) {
      try {

        JsonDocument jsonDocument = new JsonDocument(context.request().body());

        context
          .response()
          .body(
            new JsonDocument("success", database.insert(context.request().pathParameters().get("key"), jsonDocument))
              .toString());

      } catch (Exception ignored) {
        context
          .response()
          .statusCode(HttpResponseCode.HTTP_BAD_REQUEST)
          .body(new JsonDocument("reason", "Your input data must to be json").toJson());
      }
    }
  }

  @Override
  public void handleDelete(String path, IHttpContext context) {
    Database database = this.getDatabaseProvider().getDatabase(context.request().pathParameters().get("name"));

    context
      .response()
      .header("Content-Type", "application/json")
      .statusCode(HttpResponseCode.HTTP_OK)
      .context()
      .closeAfter(true)
      .cancelNext();

    if (context.request().pathParameters().containsKey("key")) {
      context
        .response()
        .body(new JsonDocument("success", database.delete(context.request().pathParameters().get("key"))).toJson());
    } else {
      database.clear();

      context
        .response()
        .body(new JsonDocument("success", true).toJson());
    }
  }
}
