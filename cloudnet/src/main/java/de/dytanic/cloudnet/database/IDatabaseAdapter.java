package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

@Deprecated
public class IDatabaseAdapter implements Database {

    private IDatabase original;

    public IDatabaseAdapter(IDatabase original) {
        this.original = original;
    }

    @Override
    public AbstractDatabaseProvider getDatabaseProvider() {
        return original.getDatabaseProvider();
    }

    @Override
    public boolean insert(String key, JsonDocument document) {
        return original.insert(key, document);
    }

    @Override
    public boolean update(String key, JsonDocument document) {
        return original.update(key, document);
    }

    @Override
    public boolean contains(String key) {
        return original.contains(key);
    }

    @Override
    public boolean delete(String key) {
        return original.delete(key);
    }

    @Override
    public JsonDocument get(String key) {
        return original.get(key);
    }

    @Override
    public List<JsonDocument> get(String fieldName, Object fieldValue) {
        return original.get(fieldName, fieldValue);
    }

    @Override
    public List<JsonDocument> get(JsonDocument filters) {
        return original.get(filters);
    }

    @Override
    public Collection<String> keys() {
        return original.keys();
    }

    @Override
    public Collection<JsonDocument> documents() {
        return original.documents();
    }

    @Override
    public Map<String, JsonDocument> entries() {
        return original.entries();
    }

    @Override
    public Map<String, JsonDocument> filter(BiPredicate<String, JsonDocument> predicate) {
        return original.filter(predicate);
    }

    @Override
    public void iterate(BiConsumer<String, JsonDocument> consumer) {
        original.iterate(consumer);
    }

    @Override
    public void clear() {
        original.clear();
    }

    @Override
    public ITask<Boolean> insertAsync(String key, JsonDocument document) {
        return original.insertAsync(key, document);
    }

    @Override
    public ITask<Boolean> containsAsync(String key) {
        return original.containsAsync(key);
    }

    @Override
    public ITask<Boolean> deleteAsync(String key) {
        return original.deleteAsync(key);
    }

    @Override
    public ITask<JsonDocument> getAsync(String key) {
        return original.getAsync(key);
    }

    @Override
    public ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue) {
        return original.getAsync(fieldName, fieldValue);
    }

    @Override
    public ITask<List<JsonDocument>> getAsync(JsonDocument filters) {
        return original.getAsync(filters);
    }

    @Override
    public ITask<Collection<String>> keysAsync() {
        return original.keysAsync();
    }

    @Override
    public ITask<Collection<JsonDocument>> documentsAsync() {
        return original.documentsAsync();
    }

    @Override
    public ITask<Map<String, JsonDocument>> entriesAsync() {
        return original.entriesAsync();
    }

    @Override
    public ITask<Map<String, JsonDocument>> filterAsync(BiPredicate<String, JsonDocument> predicate) {
        return original.filterAsync(predicate);
    }

    @Override
    public ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer) {
        return original.iterateAsync(consumer);
    }

    @Override
    public ITask<Void> clearAsync() {
        return original.clearAsync();
    }

    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public void close() throws Exception {
        original.close();
    }
}
