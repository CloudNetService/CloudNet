package de.dytanic.cloudnet.database.h2;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.database.IDatabaseHandler;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class H2DatabaseProviderTest implements IDatabaseHandler {

    protected String resultString;

    protected boolean value, geh, cleared;

    @Test
    public void testDatabaseProvider() throws Exception {
        AbstractDatabaseProvider databaseProvider = new H2DatabaseProvider("build/h2database");
        Assert.assertTrue(databaseProvider.init());

        databaseProvider.setDatabaseHandler(this);

        IDatabase database = databaseProvider.getDatabase("randomDataDatabase");
        Assert.assertNotNull(database);

        Assert.assertTrue(databaseProvider.getDatabaseNames().contains("randomDataDatabase".toUpperCase()));
        Assert.assertTrue(databaseProvider.deleteDatabase("randomDataDatabase"));
        Assert.assertFalse(databaseProvider.getDatabaseNames().contains("randomDataDatabase".toUpperCase()));

        database = databaseProvider.getDatabase("randomDataDatabase");

        Assert.assertTrue(database.insert("_xxx_", new JsonDocument("value", "1")));
        Assert.assertEquals(1, database.get("value", "1").size());
        Assert.assertTrue(database.contains("_xxx_"));
        database.clear();

        Assert.assertFalse(database.contains("_xxx_"));

        JsonDocument document = new JsonDocument();
        for (int i = 0; i < 100; i++) {
            document.append("val", i).append("name", i == 50 ? "Albert" : i > 70 ? "Luzifer" : "Peter Parker").append("age", i > 70 ? 20 : 18).append("uniqueId", UUID.randomUUID()).append("random", new Random().nextLong());

            Assert.assertEquals(5, document.size());
            Assert.assertTrue(database.insert(String.valueOf(i), document));
        }

        Assert.assertEquals(100, database.documents().size());
        Assert.assertTrue(database.update(String.valueOf(10), new JsonDocument("val", 10)));
        Assert.assertEquals(1, database.get(String.valueOf(10)).size());

        Assert.assertEquals(1, database.get("val", 61).size());
        Assert.assertEquals(1, database.get(new JsonDocument("name", "Albert").append("val", 50)).size());
        Assert.assertEquals(29, database.get(new JsonDocument("age", 20).append("name", "Luzifer")).size());

        AtomicInteger counter = new AtomicInteger();
        database.iterate((s, strings) -> counter.incrementAndGet());
        Assert.assertEquals(100, counter.get());

        Assert.assertEquals(3, database.filter((s, strings) -> s.equalsIgnoreCase("10") ||
                s.equalsIgnoreCase("14") ||
                s.equalsIgnoreCase("16")).size());

        Assert.assertTrue(database.delete("10"));
        Assert.assertEquals(99, database.documents().size());

        Assert.assertTrue(value && geh && resultString.equals("foobar"));

        database.clear();
        Assert.assertTrue(cleared);

        databaseProvider.close();
    }

    @Override
    public void handleInsert(IDatabase database, String key, JsonDocument document) {
        this.resultString = "foobar";
    }

    @Override
    public void handleUpdate(IDatabase database, String key, JsonDocument document) {
        this.value = true;
    }

    @Override
    public void handleDelete(IDatabase database, String key) {
        geh = true;
    }

    @Override
    public void handleClear(IDatabase database) {
        cleared = true;
    }
}