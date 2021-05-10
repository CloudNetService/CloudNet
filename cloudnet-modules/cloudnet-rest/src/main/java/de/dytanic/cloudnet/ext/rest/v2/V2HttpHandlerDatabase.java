package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.database.DatabaseProvider;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;

public class V2HttpHandlerDatabase extends V2HttpHandler {

    public V2HttpHandlerDatabase(String requiredPermission) {
        super(requiredPermission, "GET", "POST", "PUT", "DELETE");
    }

    @Override
    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
        if (context.request().method().equals("GET")) {
            if (path.endsWith("/database")) {
                // names of the database
                this.handleNamesRequest(context);
            } else if (path.endsWith("/clear")) {
                // clear of a specific database
                this.handleClearRequest(context);
            } else if (path.endsWith("/contains")) {
                // contains in a specific database
                this.handleContainsRequest(context);
            } else if (path.endsWith("/get")) {
                // get a value from the database
                this.handleGetRequest(context);
            } else if (path.endsWith("/keys")) {
                // list all keys in database
                this.handleKeysRequest(context);
            } else if (path.endsWith("/count")) {
                // count of documents in database
                this.handleCountRequest(context);
            }
        } else if (context.request().method().equalsIgnoreCase("POST")) {
            // insert of a document into a database
            this.handleInsertRequest(context);
        } else if (context.request().method().equalsIgnoreCase("PUT")) {
            // update a document in the database
            this.handleUpdateRequest(context);
        } else if (context.request().method().equalsIgnoreCase("DELETE")) {
            // deletes a document from the database
            this.handleDeleteRequest(context);
        }
    }

    protected void handleNamesRequest(IHttpContext context) {
        this.ok(context)
                .body(this.success().append("names", this.getDatabaseProvider().getDatabaseNames()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleClearRequest(IHttpContext context) {
        Database database = this.getDatabase(context);
        if (database == null) {
            this.sendInvalidDatabaseName(context);
            return;
        }

        database.clear();
        this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
    }

    protected void handleContainsRequest(IHttpContext context) {
        Database database = this.getDatabase(context);
        if (database == null) {
            this.sendInvalidDatabaseName(context);
            return;
        }

        String key = this.body(context.request()).getString("key");
        if (key == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Missing key in request").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
        }

        this.ok(context)
                .body(this.success().append("result", database.contains(key)).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleGetRequest(IHttpContext context) {
        Database database = this.getDatabase(context);
        if (database == null) {
            this.sendInvalidDatabaseName(context);
            return;
        }

        JsonDocument body = this.body(context.request());
        String key = body.getString("key");
        JsonDocument filter = body.getDocument("filter");

        if (key == null && filter == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Key or filter is required").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
        }

        Collection<JsonDocument> result = filter == null ? Collections.singleton(database.get(key)) : database.get(filter);
        this.ok(context)
                .body(this.success().append("result", result).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleKeysRequest(IHttpContext context) {
        Database database = this.getDatabase(context);
        if (database == null) {
            this.sendInvalidDatabaseName(context);
            return;
        }

        this.ok(context)
                .body(this.success().append("keys", database.keys()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();

    }

    protected void handleCountRequest(IHttpContext context) {
        Database database = this.getDatabase(context);
        if (database == null) {
            this.sendInvalidDatabaseName(context);
            return;
        }

        this.ok(context)
                .body(this.success().append("count", database.getDocumentsCount()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleInsertRequest(IHttpContext context) {
        Database database = this.getDatabase(context);
        if (database == null) {
            this.sendInvalidDatabaseName(context);
            return;
        }

        this.withContextData(context, (key, data) -> {
            if (database.insert(key, data)) {
                this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
            } else {
                this.ok(context).body(this.failure().toByteArray()).context().closeAfter(true).cancelNext();
            }
        });
    }

    protected void handleUpdateRequest(IHttpContext context) {
        Database database = this.getDatabase(context);
        if (database == null) {
            this.sendInvalidDatabaseName(context);
            return;
        }

        this.withContextData(context, (key, data) -> {
            if (database.update(key, data)) {
                this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
            } else {
                this.ok(context).body(this.failure().toByteArray()).context().closeAfter(true).cancelNext();
            }
        });
    }

    protected void handleDeleteRequest(IHttpContext context) {
        Database database = this.getDatabase(context);
        if (database == null) {
            this.sendInvalidDatabaseName(context);
            return;
        }

        String key = this.body(context.request()).getString("key");
        if (database.delete(key)) {
            this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
        } else {
            this.ok(context).body(this.failure().toByteArray()).context().closeAfter(true).cancelNext();
        }
    }

    protected void withContextData(IHttpContext context, BiConsumer<String, JsonDocument> handler) {
        JsonDocument body = this.body(context.request());
        String key = body.getString("key");
        JsonDocument data = body.getDocument("document");

        if (key == null || data == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", key == null ? "Missing key" : "Missing value").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        handler.accept(key, data);
    }

    protected void sendInvalidDatabaseName(IHttpContext context) {
        this.badRequest(context)
                .body(this.failure().append("reason", "No such database").toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected DatabaseProvider getDatabaseProvider() {
        return this.getCloudNet().getDatabaseProvider();
    }

    protected Database getDatabase(IHttpContext context) {
        String name = context.request().pathParameters().get("name");
        return name == null ? null : this.getDatabase(name);
    }

    protected Database getDatabase(String name) {
        return this.getDatabaseProvider().getDatabase(name);
    }
}
