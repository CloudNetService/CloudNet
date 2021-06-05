package de.dytanic.cloudnet.driver.database;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public interface Database extends INameable, AutoCloseable {

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

    boolean isSynced();

    @NotNull
    ITask<Boolean> insertAsync(String key, JsonDocument document);

    @NotNull
    ITask<Boolean> updateAsync(String key, JsonDocument document);

    @NotNull
    ITask<Boolean> containsAsync(String key);

    @NotNull
    ITask<Boolean> deleteAsync(String key);

    @NotNull
    ITask<JsonDocument> getAsync(String key);

    @NotNull
    ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue);

    @NotNull
    ITask<List<JsonDocument>> getAsync(JsonDocument filters);

    @NotNull
    ITask<Collection<String>> keysAsync();

    @NotNull
    ITask<Collection<JsonDocument>> documentsAsync();

    @NotNull
    ITask<Map<String, JsonDocument>> entriesAsync();

    @NotNull
    ITask<Map<String, JsonDocument>> filterAsync(BiPredicate<String, JsonDocument> predicate);

    @NotNull
    ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer);

    @NotNull
    ITask<Void> clearAsync();

    @NotNull
    ITask<Long> getDocumentsCountAsync();

}
