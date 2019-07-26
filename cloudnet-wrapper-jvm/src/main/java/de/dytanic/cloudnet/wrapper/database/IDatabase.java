package de.dytanic.cloudnet.wrapper.database;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public interface IDatabase extends INameable, AutoCloseable {

    boolean insert(String key, JsonDocument document);

    boolean update(String key, JsonDocument document);

    boolean contains(String key);

    boolean delete(String key);

    JsonDocument get(String key);

    List<JsonDocument> get(String fieldName, Object fieldValue);

    List<JsonDocument> get(JsonDocument filters);

    Collection<String> keys();

    Collection<JsonDocument> documents();

    Map<String, JsonDocument> entries();

    void iterate(BiConsumer<String, JsonDocument> consumer);

    void clear();

    ITask<Boolean> insertAsync(String key, JsonDocument document);

    ITask<Boolean> containsAsync(String key);

    ITask<Boolean> updateAsync(String key, JsonDocument document);

    ITask<Boolean> deleteAsync(String key);

    ITask<JsonDocument> getAsync(String key);

    ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue);

    ITask<List<JsonDocument>> getAsync(JsonDocument filters);

    ITask<Collection<String>> keysAsync();

    ITask<Collection<JsonDocument>> documentsAsync();

    ITask<Map<String, JsonDocument>> entriesAsync();

    ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer);

    ITask<Void> clearAsync();

}
