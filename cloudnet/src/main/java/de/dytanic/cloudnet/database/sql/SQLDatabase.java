package de.dytanic.cloudnet.database.sql;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public abstract class SQLDatabase implements IDatabase {

    private static final String TABLE_COLUMN_KEY = "Name", TABLE_COLUMN_VALUE = "Document";

    protected final SQLDatabaseProvider databaseProvider;
    protected final String name;

    protected final ExecutorService executorService;

    public SQLDatabase(SQLDatabaseProvider databaseProvider, String name, ExecutorService executorService) {
        Preconditions.checkNotNull(databaseProvider);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(executorService);

        this.databaseProvider = databaseProvider;
        this.name = name;
        this.executorService = executorService;

        databaseProvider.executeUpdate("CREATE TABLE IF NOT EXISTS `" + name + "` (" + TABLE_COLUMN_KEY + " VARCHAR(64) PRIMARY KEY, " + TABLE_COLUMN_VALUE + " TEXT);");
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
        this.databaseProvider.cachedDatabaseInstances.remove(this.name);
    }

    @Override
    public boolean insert(String key, JsonDocument document) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(document);

        if (this.databaseProvider.getDatabaseHandler() != null) {
            this.databaseProvider.getDatabaseHandler().handleInsert(this, key, document);
        }
        return this.contains(key) ? this.update0(key, document) : this.insert0(key, document);
    }

    public boolean insert0(String key, JsonDocument document) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(document);

        return this.databaseProvider.executeUpdate(
                "INSERT INTO `" + this.name + "` (" + TABLE_COLUMN_KEY + "," + TABLE_COLUMN_VALUE + ") VALUES (?, ?);",
                key, document.toString()
        ) != -1;
    }

    @Override
    public boolean update(String key, JsonDocument document) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(document);

        if (this.databaseProvider.getDatabaseHandler() != null) {
            this.databaseProvider.getDatabaseHandler().handleUpdate(this, key, document);
        }

        return this.contains(key) ? this.update0(key, document) : this.insert0(key, document);
    }

    public boolean update0(String key, JsonDocument document) {
        return this.databaseProvider.executeUpdate(
                "UPDATE `" + this.name + "` SET " + TABLE_COLUMN_VALUE + "=? WHERE " + TABLE_COLUMN_KEY + "=?",
                document.toString(), key
        ) != -1;
    }

    @Override
    public boolean contains(String key) {
        Preconditions.checkNotNull(key);

        return this.databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_KEY + " FROM `" + this.name + "` WHERE " + TABLE_COLUMN_KEY + "=?",
                ResultSet::next,
                key
        );
    }

    @Override
    public boolean delete(String key) {
        Preconditions.checkNotNull(key);

        if (this.databaseProvider.getDatabaseHandler() != null) {
            this.databaseProvider.getDatabaseHandler().handleDelete(this, key);
        }

        return this.delete0(key);
    }

    public boolean delete0(String key) {
        return this.databaseProvider.executeUpdate(
                "DELETE FROM `" + this.name + "` WHERE " + TABLE_COLUMN_KEY + "=?",
                key
        ) != -1;
    }

    @Override
    public JsonDocument get(String key) {
        Preconditions.checkNotNull(key);

        return this.databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_VALUE + " FROM `" + this.name + "` WHERE " + TABLE_COLUMN_KEY + "=?",
                resultSet -> resultSet.next() ? JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)) : null,
                key
        );
    }

    @Override
    public List<JsonDocument> get(String fieldName, Object fieldValue) {
        Preconditions.checkNotNull(fieldName);
        Preconditions.checkNotNull(fieldValue);

        return this.databaseProvider.executeQuery(
                "SELECT " + TABLE_COLUMN_VALUE + " FROM `" + this.name + "` WHERE " + TABLE_COLUMN_VALUE + " LIKE ?",
                resultSet -> {
                    List<JsonDocument> jsonDocuments = new ArrayList<>();

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
        Preconditions.checkNotNull(filters);

        StringBuilder stringBuilder = new StringBuilder("SELECT ").append(TABLE_COLUMN_VALUE).append(" FROM `").append(this.name).append('`');

        Collection<String> collection = new ArrayList<>();

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
                    List<JsonDocument> jsonDocuments = new ArrayList<>();

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
                "SELECT " + TABLE_COLUMN_KEY + " FROM `" + this.name + "`",
                resultSet -> {
                    Collection<String> keys = new ArrayList<>();

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
                "SELECT " + TABLE_COLUMN_VALUE + " FROM `" + this.name + "`",
                resultSet -> {
                    Collection<JsonDocument> documents = new ArrayList<>();

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
                "SELECT * FROM `" + this.name + "`",
                resultSet -> {
                    Map<String, JsonDocument> map = new WeakHashMap<>();

                    while (resultSet.next()) {
                        map.put(resultSet.getString(TABLE_COLUMN_KEY), JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));
                    }

                    return map;
                }
        );
    }

    @Override
    public Map<String, JsonDocument> filter(BiPredicate<String, JsonDocument> predicate) {
        Preconditions.checkNotNull(predicate);

        return this.databaseProvider.executeQuery(
                "SELECT * FROM `" + this.name + "`",
                resultSet -> {
                    Map<String, JsonDocument> map = new HashMap<>();

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
        Preconditions.checkNotNull(consumer);

        this.databaseProvider.executeQuery(
                "SELECT * FROM `" + this.name + "`",
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
        this.databaseProvider.executeUpdate("TRUNCATE TABLE `" + this.name + "`");
    }

    @Override
    @NotNull
    public ITask<Boolean> insertAsync(String key, JsonDocument document) {
        return this.schedule(() -> this.insert(key, document));
    }

    @Override
    public @NotNull ITask<Boolean> updateAsync(String key, JsonDocument document) {
        return this.schedule(() -> this.update(key, document));
    }

    @Override
    @NotNull
    public ITask<Boolean> containsAsync(String key) {
        return this.schedule(() -> this.contains(key));
    }

    @Override
    @NotNull
    public ITask<Boolean> deleteAsync(String key) {
        return this.schedule(() -> this.delete(key));
    }

    @Override
    @NotNull
    public ITask<JsonDocument> getAsync(String key) {
        return this.schedule(() -> this.get(key));
    }

    @Override
    @NotNull
    public ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue) {
        return this.schedule(() -> this.get(fieldName, fieldValue));
    }

    @Override
    @NotNull
    public ITask<List<JsonDocument>> getAsync(JsonDocument filters) {
        return this.schedule(() -> this.get(filters));
    }

    @Override
    @NotNull
    public ITask<Collection<String>> keysAsync() {
        return this.schedule(this::keys);
    }

    @Override
    @NotNull
    public ITask<Collection<JsonDocument>> documentsAsync() {
        return this.schedule(this::documents);
    }

    @Override
    @NotNull
    public ITask<Map<String, JsonDocument>> entriesAsync() {
        return this.schedule(this::entries);
    }

    @Override
    @NotNull
    public ITask<Map<String, JsonDocument>> filterAsync(BiPredicate<String, JsonDocument> predicate) {
        return this.schedule(() -> this.filter(predicate));
    }

    @Override
    @NotNull
    public ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer) {
        return this.schedule(() -> {
            this.iterate(consumer);
            return null;
        });
    }

    @Override
    @NotNull
    public ITask<Void> clearAsync() {
        return this.schedule(() -> {
            this.clear();
            return null;
        });
    }

    @Override
    public long getDocumentsCount() {
        return this.databaseProvider.executeQuery("SELECT COUNT(*) FROM " + this.name, resultSet -> {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return -1L;
        });
    }

    @Override
    @NotNull
    public ITask<Long> getDocumentsCountAsync() {
        return this.schedule(this::getDocumentsCount);
    }

    @NotNull
    private <T> ITask<T> schedule(Callable<T> callable) {
        ITask<T> task = new ListenableTask<>(callable);
        this.executorService.execute(() -> {
            try {
                Thread.sleep(0, 100000);
                task.call();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
        return task;
    }

}
