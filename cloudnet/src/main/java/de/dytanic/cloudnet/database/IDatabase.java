package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public interface IDatabase extends INameable, AutoCloseable {

    AbstractDatabaseProvider getDatabaseProvider();

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

    Map<String, JsonDocument> filter(BiPredicate<String, JsonDocument> predicate);

    void iterate(BiConsumer<String, JsonDocument> consumer);

    void clear();

    long getDocumentsCount();


    ITask<Boolean> insertAsync(String key, JsonDocument document);

    ITask<Boolean> containsAsync(String key);

    ITask<Boolean> deleteAsync(String key);

    ITask<JsonDocument> getAsync(String key);

    ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue);

    ITask<List<JsonDocument>> getAsync(JsonDocument filters);

    ITask<Collection<String>> keysAsync();

    ITask<Collection<JsonDocument>> documentsAsync();

    ITask<Map<String, JsonDocument>> entriesAsync();

    ITask<Map<String, JsonDocument>> filterAsync(BiPredicate<String, JsonDocument> predicate);

    ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer);

    ITask<Void> clearAsync();

    ITask<Long> getDocumentsCountAsync();


}