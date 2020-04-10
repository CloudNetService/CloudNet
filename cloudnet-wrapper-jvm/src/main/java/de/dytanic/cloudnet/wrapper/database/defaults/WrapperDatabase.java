package de.dytanic.cloudnet.wrapper.database.defaults;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.wrapper.database.IDatabase;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class WrapperDatabase implements IDatabase {

    private String name;
    private DefaultWrapperDatabaseProvider databaseProvider;

    public WrapperDatabase(String name, DefaultWrapperDatabaseProvider databaseProvider) {
        this.name = name;
        this.databaseProvider = databaseProvider;
    }

    @Override
    public boolean insert(String key, JsonDocument document) {
        return this.insertAsync(key, document).getDef(false);
    }

    @Override
    public boolean update(String key, JsonDocument document) {
        return this.updateAsync(key, document).getDef(false);
    }

    @Override
    public boolean contains(String key) {
        return this.containsAsync(key).getDef(false);
    }

    @Override
    public boolean delete(String key) {
        return this.deleteAsync(key).getDef(false);
    }

    @Override
    public JsonDocument get(String key) {
        return this.getAsync(key).getDef(null);
    }

    @Override
    public List<JsonDocument> get(String fieldName, Object fieldValue) {
        return this.getAsync(fieldName, fieldValue).getDef(Collections.emptyList());
    }

    @Override
    public List<JsonDocument> get(JsonDocument filters) {
        return this.getAsync(filters).getDef(Collections.emptyList());
    }

    @Override
    public Collection<String> keys() {
        return this.keysAsync().getDef(Collections.emptyList());
    }

    @Override
    public Collection<JsonDocument> documents() {
        return this.documentsAsync().getDef(Collections.emptyList());
    }

    @Override
    public Map<String, JsonDocument> entries() {
        return this.entriesAsync().getDef(Collections.emptyMap());
    }

    @Override
    public void iterate(BiConsumer<String, JsonDocument> consumer) {
        this.iterateAsync(consumer).getDef(null);
    }

    @Override
    public void clear() {
        this.clearAsync().getDef(null);
    }

    @Override
    public long getDocumentsCount() {
        Long result = this.getDocumentsCountAsync().getDef(-1L);
        return result != null ? result : -1;
    }

    @Override
    @NotNull
    public ITask<Boolean> insertAsync(String key, JsonDocument document) {
        return this.databaseProvider.executeQuery(this.name, "insert",
                new JsonDocument()
                        .append("key", key)
                        .append("value", document),
                response -> response.getSecond()[0] == 1
        );
    }

    @Override
    @NotNull
    public ITask<Boolean> containsAsync(String key) {
        return this.databaseProvider.executeQuery(this.name, "contains",
                new JsonDocument()
                        .append("key", key),
                response -> response.getSecond()[0] == 1
        );
    }

    @Override
    @NotNull
    public ITask<Boolean> updateAsync(String key, JsonDocument document) {
        return this.databaseProvider.executeQuery(this.name, "update",
                new JsonDocument()
                        .append("key", key)
                        .append("value", document),
                response -> response.getSecond()[0] == 1
        );
    }

    @Override
    @NotNull
    public ITask<Boolean> deleteAsync(String key) {
        return this.databaseProvider.executeQuery(this.name, "delete",
                new JsonDocument()
                        .append("key", key),
                response -> response.getSecond()[0] == 1
        );
    }

    @Override
    @NotNull
    public ITask<JsonDocument> getAsync(String key) {
        return this.databaseProvider.executeQuery(this.name, "get",
                new JsonDocument()
                        .append("key", key),
                response -> response.getFirst().getDocument("match")
        );
    }

    @Override
    @NotNull
    public ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue) {
        return this.databaseProvider.executeQuery(this.name, "get",
                new JsonDocument()
                        .append("name", fieldName)
                        .append("value", fieldValue),
                response -> response.getFirst().get("matches", new TypeToken<List<JsonDocument>>() {
                }.getType())
        );
    }

    @Override
    @NotNull
    public ITask<List<JsonDocument>> getAsync(JsonDocument filters) {
        return this.databaseProvider.executeQuery(this.name, "get",
                new JsonDocument()
                        .append("filters", filters),
                response -> response.getFirst().get("matches", new TypeToken<List<JsonDocument>>() {
                }.getType())
        );
    }

    @Override
    @NotNull
    public ITask<Collection<String>> keysAsync() {
        return this.databaseProvider.executeQuery(this.name, "keys",
                response -> response.getFirst().get("keys", new TypeToken<Collection<String>>() {
                        }.getType()
                )
        );
    }

    @Override
    @NotNull
    public ITask<Collection<JsonDocument>> documentsAsync() {
        return this.databaseProvider.executeQuery(this.name, "documents",
                response -> response.getFirst().get("documents", new TypeToken<Collection<JsonDocument>>() {
                        }.getType()
                )
        );
    }

    @Override
    @NotNull
    public ITask<Map<String, JsonDocument>> entriesAsync() {
        return this.databaseProvider.executeQuery(this.name, "entries",
                response -> response.getFirst().get("entries", new TypeToken<Map<String, JsonDocument>>() {
                        }.getType()
                )
        );
    }

    @Override
    @NotNull
    public ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer) {
        ITask<Void> task = new ListenableTask<>(() -> null);
        this.entriesAsync().onComplete(response -> {
            response.forEach(consumer);
            try {
                task.call();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
        return task;
    }

    @Override
    @NotNull
    public ITask<Void> clearAsync() {
        return this.databaseProvider.executeQuery(this.name, "clear", response -> null);
    }

    @Override
    @NotNull
    public ITask<Long> getDocumentsCountAsync() {
        return this.databaseProvider.executeQuery(this.name, "documentsCount", response -> response.getFirst().getLong("documentsCount"));
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void close() {
        this.databaseProvider.executeQuery(this.name, "close", response -> null).getDef(null);
    }

}
