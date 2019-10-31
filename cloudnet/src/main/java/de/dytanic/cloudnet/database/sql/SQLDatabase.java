package de.dytanic.cloudnet.database.sql;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.CloudNetDriver;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public abstract class SQLDatabase implements IDatabase {

    private static final String TABLE_COLUMN_KEY = "Name", TABLE_COLUMN_VALUE = "Document";

    protected SQLDatabaseProvider databaseProvider;
    protected String name;

    public SQLDatabase(SQLDatabaseProvider databaseProvider, String name) {
        Validate.checkNotNull(databaseProvider);
        Validate.checkNotNull(name);

        this.databaseProvider = databaseProvider;
        this.name = name;

        databaseProvider.executeUpdate("CREATE TABLE IF NOT EXISTS " + name + "(" + TABLE_COLUMN_KEY + " VARCHAR(1024), " + TABLE_COLUMN_VALUE + " TEXT);");
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public SQLDatabaseProvider getDatabaseProvider() {
        return this.databaseProvider;
    }

    @Override
    public void close() {
        this.databaseProvider.cachedDatabaseInstances.remove(name);
    }

    @Override
    public boolean insert(String key, JsonDocument document) {
        Validate.checkNotNull(key);
        Validate.checkNotNull(document);

        if (this.databaseProvider.getDatabaseHandler() != null) {
            this.databaseProvider.getDatabaseHandler().handleInsert(this, key, document);
        }

        return this.insert0(key, document);
    }

    public boolean insert0(String key, JsonDocument document) {
        Validate.checkNotNull(key);
        Validate.checkNotNull(document);

        return !contains(key) ?
                this.databaseProvider.executeUpdate(
                        "INSERT INTO " + this.name + "(" + TABLE_COLUMN_KEY + "," + TABLE_COLUMN_VALUE + ") VALUES (?, ?);",
                        key, document.toString()
                ) != -1 : update(key, document);
    }

    @Override
    public boolean update(String key, JsonDocument document) {
        Validate.checkNotNull(key);
        Validate.checkNotNull(document);

        if (this.databaseProvider.getDatabaseHandler() != null) {
            this.databaseProvider.getDatabaseHandler().handleUpdate(this, key, document);
        }

        return !contains(key) ? insert0(key, document) : update0(key, document);
    }

    public boolean update0(String key, JsonDocument document) {
        return this.databaseProvider.executeUpdate(
                "UPDATE " + this.name + " SET " + TABLE_COLUMN_VALUE + "=? WHERE " + TABLE_COLUMN_KEY + "=?",
                document.toString(), key
        ) != -1;
    }

    @Override
    public boolean contains(String key) {
        Validate.checkNotNull(key);

        return this.databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_KEY + " FROM " + this.name + " WHERE " + TABLE_COLUMN_KEY + "=?",
                ResultSet::next,
                key
        );
    }

    @Override
    public boolean delete(String key) {
        Validate.checkNotNull(key);

        if (this.databaseProvider.getDatabaseHandler() != null) {
            this.databaseProvider.getDatabaseHandler().handleDelete(this, key);
        }

        return this.delete0(key);
    }

    public boolean delete0(String key) {
        return this.databaseProvider.executeUpdate(
                "DELETE FROM " + this.name + " WHERE " + TABLE_COLUMN_KEY + "=?",
                key
        ) != -1;
    }

    @Override
    public JsonDocument get(String key) {
        Validate.checkNotNull(key);

        return this.databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_VALUE + " FROM " + this.name + " WHERE " + TABLE_COLUMN_KEY + "=?",
                resultSet -> resultSet.next() ? JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)) : null,
                key
        );
    }

    @Override
    public List<JsonDocument> get(String fieldName, Object fieldValue) {
        Validate.checkNotNull(fieldName);
        Validate.checkNotNull(fieldValue);

        return this.databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_VALUE + " FROM " + this.name + " WHERE " + TABLE_COLUMN_VALUE + " LIKE ?",
                resultSet -> {
                    List<JsonDocument> jsonDocuments = Iterables.newArrayList();

                    while (resultSet.next()) {
                        jsonDocuments.add(JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));
                    }

                    return jsonDocuments;
                },
                "%\"" + fieldName + "\":" + JsonDocument.GSON.toJson(fieldValue) + "%"
        );
    }

    @Override
    public List<JsonDocument> get(JsonDocument filters) {
        Validate.checkNotNull(filters);

        StringBuilder stringBuilder = new StringBuilder("SELECT ").append(TABLE_COLUMN_VALUE).append(" FROM ").append(this.name);

        Collection<String> collection = Iterables.newArrayList();

        if (filters.size() > 0) {
            stringBuilder.append(" WHERE ");

            Iterator<String> iterator = filters.iterator();
            String item;

            while (iterator.hasNext()) {
                item = iterator.next();

                stringBuilder.append(TABLE_COLUMN_VALUE).append(" LIKE ?");
                collection.add("%\"" + item + "\":" + filters.get(item).toString() + "%");

                if (iterator.hasNext()) {
                    stringBuilder.append(" and ");
                }
            }
        }

        return this.databaseProvider.executeQuery(
                stringBuilder.toString(),
                resultSet -> {
                    List<JsonDocument> jsonDocuments = Iterables.newArrayList();

                    while (resultSet.next()) {
                        jsonDocuments.add(JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));
                    }

                    return jsonDocuments;
                },
                collection.toArray()
        );
    }

    @Override
    public Collection<String> keys() {
        return this.databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_KEY + " FROM " + this.name,
                resultSet -> {
                    Collection<String> keys = Iterables.newArrayList();

                    while (resultSet.next()) {
                        keys.add(resultSet.getString(TABLE_COLUMN_KEY));
                    }

                    return keys;
                }
        );
    }

    @Override
    public Collection<JsonDocument> documents() {
        return this.databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_VALUE + " FROM " + this.name,
                resultSet -> {
                    Collection<JsonDocument> documents = Iterables.newArrayList();

                    while (resultSet.next()) {
                        documents.add(JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));
                    }

                    return documents;
                }
        );
    }

    @Override
    public Map<String, JsonDocument> entries() {
        return this.databaseProvider.executeQuery(
                "SELECT * FROM " + this.name,
                resultSet -> {
                    Map<String, JsonDocument> map = Maps.newWeakHashMap();

                    while (resultSet.next()) {
                        map.put(resultSet.getString(TABLE_COLUMN_KEY), JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));
                    }

                    return map;
                }
        );
    }

    @Override
    public Map<String, JsonDocument> filter(BiPredicate<String, JsonDocument> predicate) {
        Validate.checkNotNull(predicate);

        return this.databaseProvider.executeQuery(
                "SELECT * FROM " + this.name,
                resultSet -> {
                    Map<String, JsonDocument> map = Maps.newHashMap();

                    while (resultSet.next()) {
                        String key = resultSet.getString(TABLE_COLUMN_KEY);
                        JsonDocument document = JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE));

                        if (predicate.test(key, document)) {
                            map.put(key, document);
                        }
                    }

                    return map;
                }
        );
    }

    @Override
    public void iterate(BiConsumer<String, JsonDocument> consumer) {
        Validate.checkNotNull(consumer);

        this.databaseProvider.executeQuery(
                "SELECT * FROM " + this.name,
                (IThrowableCallback<ResultSet, Void>) resultSet -> {
                    while (resultSet.next()) {
                        String key = resultSet.getString(TABLE_COLUMN_KEY);
                        JsonDocument document = JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE));
                        consumer.accept(key, document);
                    }

                    return null;
                }
        );
    }

    @Override
    public void clear() {
        if (this.databaseProvider.getDatabaseHandler() != null) {
            this.databaseProvider.getDatabaseHandler().handleClear(this);
        }
        this.clear0();
    }

    public void clear0() {
        this.databaseProvider.executeUpdate("TRUNCATE TABLE " + this.name);
    }

    @Override
    public ITask<Boolean> insertAsync(String key, JsonDocument document) {
        return this.schedule(() -> insert(key, document));
    }

    @Override
    public ITask<Boolean> containsAsync(String key) {
        return this.schedule(() -> this.contains(key));
    }

    @Override
    public ITask<Boolean> deleteAsync(String key) {
        return this.schedule(() -> this.delete(key));
    }

    @Override
    public ITask<JsonDocument> getAsync(String key) {
        return this.schedule(() -> this.get(key));
    }

    @Override
    public ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue) {
        return this.schedule(() -> this.get(fieldName, fieldValue));
    }

    @Override
    public ITask<List<JsonDocument>> getAsync(JsonDocument filters) {
        return this.schedule(() -> this.get(filters));
    }

    @Override
    public ITask<Collection<String>> keysAsync() {
        return this.schedule(this::keys);
    }

    @Override
    public ITask<Collection<JsonDocument>> documentsAsync() {
        return this.schedule(this::documents);
    }

    @Override
    public ITask<Map<String, JsonDocument>> entriesAsync() {
        return this.schedule(this::entries);
    }

    @Override
    public ITask<Map<String, JsonDocument>> filterAsync(BiPredicate<String, JsonDocument> predicate) {
        return this.schedule(() -> this.filter(predicate));
    }

    @Override
    public ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer) {
        return this.schedule(() -> {
            this.iterate(consumer);
            return null;
        });
    }

    @Override
    public ITask<Void> clearAsync() {
        return this.schedule(() -> {
            this.clear();
            return null;
        });
    }

    private <T> ITask<T> schedule(Callable<T> callable) {
        return CloudNetDriver.getInstance().getTaskScheduler().schedule(callable);
    }

}
