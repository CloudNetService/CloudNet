package de.dytanic.cloudnet.examples.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.IDatabase;

import java.util.List;

public final class ExampleDatabaseProviderHandling {

    public void testDatabaseProvider() throws Throwable {
        AbstractDatabaseProvider databaseProvider = CloudNet.getInstance().getDatabaseProvider();

        IDatabase database = databaseProvider.getDatabase("My custom Database");
        database.insert("Peter", new JsonDocument()
                .append("name", "Peter")
                .append("lastName", "Parker")
                .append("age", 17)
                .append("registered", System.currentTimeMillis())
        );

        if (database.contains("Peter")) {
            database.getAsync("Peter").onComplete(document -> {
                System.out.println(document.getString("name"));
                System.out.println(document.getString("lastName"));
                System.out.println(document.getInt("age"));
            }).addListener(ITaskListener.FIRE_EXCEPTION_ON_FAILURE);
        }

        List<JsonDocument> responses = database.get("name", "Peter"); //filter with a key/value pair in value
        System.out.println("Founded items: " + responses.size()); //Founded items: 1

        responses = database.get(new JsonDocument("age", 17).append("lastName", "Parker")); //Filter with JsonDocument properties
        System.out.println("Founded items: " + responses.size()); //Founded items: 1

        database.clearAsync().get();
    }
}