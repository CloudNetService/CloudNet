package de.dytanic.cloudnet.database.h2;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public final class H2Database implements IDatabase {

    private static final String TABLE_COLUMN_KEY = "Name", TABLE_COLUMN_VALUE = "Document";

    private final H2DatabaseProvider databaseProvider;

    private final String name;

    public H2Database(H2DatabaseProvider databaseProvider, String name) {
        Validate.checkNotNull(databaseProvider);
        Validate.checkNotNull(name);

        this.databaseProvider = databaseProvider;
        this.name = name;

        databaseProvider.executeUpdate("CREATE TABLE IF NOT EXISTS " + name + "(" + TABLE_COLUMN_KEY + " VARCHAR(1024), " + TABLE_COLUMN_VALUE + " TEXT);");
    }

    @Override
    public void close() throws Exception {
        databaseProvider.cachedDatabaseInstances.remove(name);
    }

    @Override
    public boolean insert(String key, JsonDocument document) {
        Validate.checkNotNull(key);
        Validate.checkNotNull(document);

        if (databaseProvider.getDatabaseHandler() != null)
            databaseProvider.getDatabaseHandler().handleInsert(this, key, document);

        return insert0(key, document);
    }

    public boolean insert0(String key, JsonDocument document) {
        Validate.checkNotNull(key);
        Validate.checkNotNull(document);

        return !contains(key) ?
                databaseProvider.executeUpdate(
                        "INSERT INTO " + name + "(" + TABLE_COLUMN_KEY + "," + TABLE_COLUMN_VALUE + ") VALUES (?, ?);",
                        key, document.toString()
                ) != -1 : update0(key, document);
    }

    @Override
    public boolean update(String key, JsonDocument document) {
        Validate.checkNotNull(key);
        Validate.checkNotNull(document);

        if (databaseProvider.getDatabaseHandler() != null)
            databaseProvider.getDatabaseHandler().handleUpdate(this, key, document);

        return !contains(key) ? insert0(key, document) : update0(key, document);
    }

    public boolean update0(String key, JsonDocument document) {
        return databaseProvider.executeUpdate(
                "UPDATE " + name + " SET " + TABLE_COLUMN_VALUE + "=? WHERE " + TABLE_COLUMN_KEY + "=?",
                document.toString(), key
        ) != -1;
    }

    @Override
    public boolean contains(String key) {
        Validate.checkNotNull(key);

        return databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_KEY + " FROM " + name + " WHERE " + TABLE_COLUMN_KEY + "=?",
                new IThrowableCallback<ResultSet, Boolean>() {

                    @Override
                    public Boolean call(ResultSet resultSet) throws Throwable {
                        return resultSet.next();
                    }
                },
                key
        );
    }

    @Override
    public boolean delete(String key) {
        Validate.checkNotNull(key);

        if (databaseProvider.getDatabaseHandler() != null)
            databaseProvider.getDatabaseHandler().handleDelete(this, key);

        return delete0(key);
    }

    public boolean delete0(String key) {
        return databaseProvider.executeUpdate(
                "DELETE FROM " + name + " WHERE " + TABLE_COLUMN_KEY + "=?",
                key
        ) != -1;
    }

    @Override
    public JsonDocument get(String key) {
        Validate.checkNotNull(key);

        return databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_VALUE + " FROM " + name + " WHERE " + TABLE_COLUMN_KEY + "=?",
                new IThrowableCallback<ResultSet, JsonDocument>() {
                    @Override
                    public JsonDocument call(ResultSet resultSet) throws Throwable {
                        return resultSet.next() ? JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)) : null;
                    }
                },
                key
        );
    }

    @Override
    public List<JsonDocument> get(String fieldName, Object fieldValue) {
        Validate.checkNotNull(fieldName);
        Validate.checkNotNull(fieldValue);

        return databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_VALUE + " FROM " + name + " WHERE " + TABLE_COLUMN_VALUE + " LIKE ?",
                new IThrowableCallback<ResultSet, List<JsonDocument>>() {
                    @Override
                    public List<JsonDocument> call(ResultSet resultSet) throws Throwable {
                        List<JsonDocument> jsonDocuments = Iterables.newArrayList();

                        while (resultSet.next())
                            jsonDocuments.add(JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));

                        return jsonDocuments;
                    }
                },
                "%\"" + fieldName + "\":" + JsonDocument.GSON.toJson(fieldValue) + "%"
        );
    }

    @Override
    public List<JsonDocument> get(JsonDocument filters) {
        Validate.checkNotNull(filters);

        StringBuilder stringBuilder = new StringBuilder("SELECT ").append(TABLE_COLUMN_VALUE).append(" FROM ").append(name);

        Collection<String> collection = Iterables.newArrayList();

        if (filters.size() > 0) {
            stringBuilder.append(" WHERE ");

            Iterator<String> iterator = filters.iterator();
            String item;

            while (iterator.hasNext()) {
                item = iterator.next();

                stringBuilder.append(TABLE_COLUMN_VALUE).append(" LIKE ?");
                collection.add("%\"" + item + "\":" + filters.get(item).toString() + "%");

                if (iterator.hasNext()) stringBuilder.append(" and ");
            }
        }

        return databaseProvider.executeQuery(
                stringBuilder.toString(),
                new IThrowableCallback<ResultSet, List<JsonDocument>>() {
                    @Override
                    public List<JsonDocument> call(ResultSet resultSet) throws Throwable {
                        List<JsonDocument> jsonDocuments = Iterables.newArrayList();

                        while (resultSet.next())
                            jsonDocuments.add(JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));

                        return jsonDocuments;
                    }
                },
                collection.toArray()
        );
    }

    @Override
    public Collection<String> keys() {
        return databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_KEY + " FROM " + name,
                new IThrowableCallback<ResultSet, Collection<String>>() {
                    @Override
                    public Collection<String> call(ResultSet resultSet) throws Throwable {
                        Collection<String> keys = Iterables.newArrayList();

                        while (resultSet.next())
                            keys.add(resultSet.getString(TABLE_COLUMN_KEY));

                        return keys;
                    }
                }
        );
    }

    @Override
    public Collection<JsonDocument> documents() {
        return databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_VALUE + " FROM " + name,
                new IThrowableCallback<ResultSet, Collection<JsonDocument>>() {
                    @Override
                    public Collection<JsonDocument> call(ResultSet resultSet) throws Throwable {
                        Collection<JsonDocument> documents = Iterables.newArrayList();

                        while (resultSet.next())
                            documents.add(JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));

                        return documents;
                    }
                }
        );
    }

    @Override
    public Map<String, JsonDocument> entries() {
        return databaseProvider.executeQuery(
                "SELECT * FROM " + name,
                new IThrowableCallback<ResultSet, Map<String, JsonDocument>>() {
                    @Override
                    public Map<String, JsonDocument> call(ResultSet resultSet) throws Throwable {
                        Map<String, JsonDocument> map = Maps.newWeakHashMap();

                        while (resultSet.next())
                            map.put(resultSet.getString(TABLE_COLUMN_KEY), JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));

                        return map;
                    }
                }
        );
    }

    @Override
    public Map<String, JsonDocument> filter(BiPredicate<String, JsonDocument> predicate) {
        Validate.checkNotNull(predicate);

        return databaseProvider.executeQuery(
                "SELECT * FROM " + name,
                new IThrowableCallback<ResultSet, Map<String, JsonDocument>>() {
                    @Override
                    public Map<String, JsonDocument> call(ResultSet resultSet) throws Throwable {
                        Map<String, JsonDocument> map = Maps.newHashMap();

                        while (resultSet.next()) {
                            String key = resultSet.getString(TABLE_COLUMN_KEY);
                            JsonDocument document = JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE));

                            if (predicate.test(key, document)) map.put(key, document);
                        }

                        return map;
                    }
                }
        );
    }

    @Override
    public void iterate(BiConsumer<String, JsonDocument> consumer) {
        Validate.checkNotNull(consumer);

        databaseProvider.executeQuery(
                "SELECT * FROM " + name,
                new IThrowableCallback<ResultSet, Void>() {
                    @Override
                    public Void call(ResultSet resultSet) throws Throwable {
                        while (resultSet.next()) {
                            String key = resultSet.getString(TABLE_COLUMN_KEY);
                            JsonDocument document = JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE));
                            consumer.accept(key, document);
                        }

                        return null;
                    }
                }
        );
    }

    @Override
    public ITask<Boolean> insertAsync(String key, JsonDocument document) {
        return schedule(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return insert(key, document);
            }
        });
    }

    @Override
    public ITask<Boolean> containsAsync(String key) {
        return schedule(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return contains(key);
            }
        });
    }

    @Override
    public ITask<Boolean> deleteAsync(String key) {
        return schedule(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return delete(key);
            }
        });
    }

    @Override
    public ITask<JsonDocument> getAsync(String key) {
        return schedule(new Callable<JsonDocument>() {
            @Override
            public JsonDocument call() throws Exception {
                return get(key);
            }
        });
    }

    @Override
    public ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue) {
        return schedule(new Callable<List<JsonDocument>>() {
            @Override
            public List<JsonDocument> call() throws Exception {
                return get(fieldName, fieldValue);
            }
        });
    }

    @Override
    public ITask<List<JsonDocument>> getAsync(JsonDocument filters) {
        return schedule(new Callable<List<JsonDocument>>() {
            @Override
            public List<JsonDocument> call() throws Exception {
                return get(filters);
            }
        });
    }

    @Override
    public ITask<Collection<String>> keysAsync() {
        return schedule(new Callable<Collection<String>>() {
            @Override
            public Collection<String> call() throws Exception {
                return keys();
            }
        });
    }

    @Override
    public ITask<Collection<JsonDocument>> documentsAsync() {
        return schedule(new Callable<Collection<JsonDocument>>() {
            @Override
            public Collection<JsonDocument> call() throws Exception {
                return documents();
            }
        });
    }

    @Override
    public ITask<Map<String, JsonDocument>> entriesAsync() {
        return schedule(new Callable<Map<String, JsonDocument>>() {
            @Override
            public Map<String, JsonDocument> call() throws Exception {
                return entries();
            }
        });
    }

    @Override
    public ITask<Map<String, JsonDocument>> filterAsync(BiPredicate<String, JsonDocument> predicate) {
        return schedule(new Callable<Map<String, JsonDocument>>() {
            @Override
            public Map<String, JsonDocument> call() throws Exception {
                return filter(predicate);
            }
        });
    }

    @Override
    public ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer) {
        return schedule(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                iterate(consumer);
                return null;
            }
        });
    }

    @Override
    public void clear() {
        if (databaseProvider.getDatabaseHandler() != null)
            databaseProvider.getDatabaseHandler().handleClear(this);

        clear0();
    }

    public void clear0() {
        databaseProvider.executeUpdate("TRUNCATE TABLE " + name);
    }

    @Override
    public ITask<Void> clearAsync() {
        return schedule(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                clear();
                return null;
            }
        });
    }

    /*= -------------------------------------------------------- =*/

    private <T> ITask<T> schedule(Callable<T> callable) {
        return schedule(callable);
    }

    public H2DatabaseProvider getDatabaseProvider() {
        return this.databaseProvider;
    }

    public String getName() {
        return this.name;
    }
}