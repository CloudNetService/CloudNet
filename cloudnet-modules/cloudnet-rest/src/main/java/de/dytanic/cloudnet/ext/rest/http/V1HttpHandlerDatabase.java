package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.IDatabase;
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
        return getCloudNet().getDatabaseProvider();
    }

    @Override
    public void handleOptions(String path, IHttpContext context) {
        super.sendOptions(context, "GET, DELETE, POST");
    }

    @Override
    public void handleGet(String path, IHttpContext context) {
        IDatabase database = getDatabaseProvider().getDatabase(context.request().pathParameters().get("name"));

        context
                .response()
                .header("Content-Type", "application/json")
                .statusCode(HttpResponseCode.HTTP_OK)
        ;

        if (context.request().pathParameters().containsKey("key")) {
            context
                    .response()
                    .body(database.contains(context.request().pathParameters().get("key")) ?
                            database.get(context.request().pathParameters().get("key")).toJson()
                            :
                            new JsonDocument().toJson())
            ;

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
                    .cancelNext()
            ;
        }
    }

    @Override
    public void handlePost(String path, IHttpContext context) {
        IDatabase database = getDatabaseProvider().getDatabase(context.request().pathParameters().get("name"));

        context
                .response()
                .header("Content-Type", "application/json")
                .statusCode(HttpResponseCode.HTTP_OK)
                .context()
                .closeAfter(true)
                .cancelNext()
        ;

        if (context.request().pathParameters().containsKey("key")) {
            try {

                JsonDocument jsonDocument = new JsonDocument(context.request().body());

                context
                        .response()
                        .body(new JsonDocument("success", database.insert(context.request().pathParameters().get("key"), jsonDocument)).toString())
                ;

            } catch (Exception ignored) {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_BAD_REQUEST)
                        .body(new JsonDocument("reason", "Your input data must to be json").toJson())
                ;
            }
        }
    }

    @Override
    public void handleDelete(String path, IHttpContext context) {
        IDatabase database = getDatabaseProvider().getDatabase(context.request().pathParameters().get("name"));

        context
                .response()
                .header("Content-Type", "application/json")
                .statusCode(HttpResponseCode.HTTP_OK)
                .context()
                .closeAfter(true)
                .cancelNext()
        ;

        if (context.request().pathParameters().containsKey("key")) {
            context
                    .response()
                    .body(new JsonDocument("success", database.delete(context.request().pathParameters().get("key"))).toJson())
            ;
        } else {
            database.clear();

            context
                    .response()
                    .body(new JsonDocument("success", true).toJson())
            ;
        }
    }
}