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

package eu.cloudnetservice.modules.rest.v2;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.modules.rest.RestUtil;
import eu.cloudnetservice.node.http.HttpSession;
import eu.cloudnetservice.node.http.V2HttpHandler;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class V2HttpHandlerDatabase extends V2HttpHandler {

  public V2HttpHandlerDatabase(@Nullable String requiredPermission) {
    super(requiredPermission, "GET", "POST", "PUT", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(
    @NonNull String path,
    @NonNull HttpContext context,
    @NonNull HttpSession session
  ) {
    if (context.request().method().equals("GET")) {
      if (path.endsWith("/contains")) {
        // contains in a specific database
        this.handleContainsRequest(context);
      } else if (path.endsWith("/database")) {
        // names of the database
        this.handleNamesRequest(context);
      } else if (path.endsWith("/clear")) {
        // clear of a specific database
        this.handleClearRequest(context);
      } else if (path.endsWith("/keys")) {
        // list all keys in database
        this.handleKeysRequest(context);
      } else if (path.endsWith("/count")) {
        // count of documents in database
        this.handleCountRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      if (path.endsWith("/get")) {
        // get a value from the database
        this.handleGetRequest(context);
      } else {
        // insert of a document into a database
        this.handleInsertRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("DELETE")) {
      // deletes a document from the database
      this.handleDeleteRequest(context);
    }
  }

  protected void handleNamesRequest(@NonNull HttpContext context) {
    this.ok(context)
      .body(this.success().append("names", this.databaseProvider().databaseNames()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleClearRequest(@NonNull HttpContext context) {
    var database = this.database(context);
    if (database == null) {
      this.sendInvalidDatabaseName(context);
      return;
    }

    database.clear();
    this.ok(context)
      .body(this.success().toString())
      .context().closeAfter(true)
      .cancelNext();
  }

  protected void handleContainsRequest(@NonNull HttpContext context) {
    var database = this.database(context);
    if (database == null) {
      this.sendInvalidDatabaseName(context);
      return;
    }

    var key = RestUtil.first(context.request().queryParameters().get("key"));
    if (key == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing key in request").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      this.ok(context)
        .body(this.success().append("result", database.contains(key)).toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }

  protected void handleGetRequest(@NonNull HttpContext context) {
    var database = this.database(context);
    if (database == null) {
      this.sendInvalidDatabaseName(context);
      return;
    }

    var body = this.body(context.request());
    var filter = body.getDocument("filter");

    var result = database.find(filter);
    this.ok(context)
      .body(this.success().append("result", result).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleKeysRequest(@NonNull HttpContext context) {
    var database = this.database(context);
    if (database == null) {
      this.sendInvalidDatabaseName(context);
      return;
    }

    this.ok(context)
      .body(this.success().append("keys", database.keys()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();

  }

  protected void handleCountRequest(@NonNull HttpContext context) {
    var database = this.database(context);
    if (database == null) {
      this.sendInvalidDatabaseName(context);
      return;
    }

    this.ok(context)
      .body(this.success().append("count", database.documentCount()).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleInsertRequest(@NonNull HttpContext context) {
    var database = this.database(context);
    if (database == null) {
      this.sendInvalidDatabaseName(context);
      return;
    }

    this.withContextData(context, (key, data) -> {
      if (database.insert(key, data)) {
        this.ok(context)
          .body(this.success().toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context)
          .body(this.failure().toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleDeleteRequest(@NonNull HttpContext context) {
    var database = this.database(context);
    if (database == null) {
      this.sendInvalidDatabaseName(context);
      return;
    }

    var key = RestUtil.first(context.request().queryParameters().get("key"));
    if (key != null && database.delete(key)) {
      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      this.ok(context)
        .body(this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }

  protected void withContextData(@NonNull HttpContext context, @NonNull BiConsumer<String, JsonDocument> handler) {
    var body = this.body(context.request());
    var key = body.getString("key");
    var data = body.getDocument("document");

    if (key == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing key").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    handler.accept(key, data);
  }

  protected void sendInvalidDatabaseName(@NonNull HttpContext context) {
    this.badRequest(context)
      .body(this.failure().append("reason", "No such database").toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected @NonNull DatabaseProvider databaseProvider() {
    return this.node().databaseProvider();
  }

  protected @Nullable Database database(@NonNull HttpContext context) {
    var name = context.request().pathParameters().get("name");
    return name == null ? null : this.database(name);
  }

  protected @NonNull Database database(@NonNull String name) {
    return this.databaseProvider().database(name);
  }
}
