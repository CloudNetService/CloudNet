/*
 * Copyright 2019-2022 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.driver.database;

import de.dytanic.cloudnet.common.Nameable;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.Task;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The database is used to store cloudnet related data, these databases can be used by an api user too. To create or
 * retrieve a database see {@link de.dytanic.cloudnet.driver.database.DatabaseProvider}
 */
public interface Database extends Nameable, AutoCloseable {

  /**
   * Insert the given document for the given key
   *
   * @param key      the key in the database
   * @param document the document to be stored
   * @return whether the operation was successful or not
   */
  boolean insert(@NonNull String key, @NonNull JsonDocument document);

  /**
   * Updates the given document for the given key
   *
   * @param key      the key in the database
   * @param document the document to be stored
   * @return whether the operation was successful or not
   */
  boolean update(@NonNull String key, @NonNull JsonDocument document);

  /**
   * @param key the key to look for
   * @return whether the database contains the given key
   */
  boolean contains(@NonNull String key);

  /**
   * Deletes the given key and corresponding data
   *
   * @param key the key to be deleted
   * @return whether the operation was successful or not
   */
  boolean delete(@NonNull String key);

  /**
   * Searches for a {@link JsonDocument} by the given key
   *
   * @param key the key to the document
   * @return the document associated with the key
   */
  @Nullable JsonDocument get(String key);

  /**
   * Searches the database for a field that has the given fieldValue
   *
   * @param fieldName  the fieldName to look for
   * @param fieldValue the fieldValue associated with the fieldName
   * @return a List with the documents containing the field and fieldValue
   */
  @NonNull List<JsonDocument> get(@NonNull String fieldName, @Nullable Object fieldValue);

  /**
   * Filters the database by the given document filters
   *
   * @param filters the filter to filter with
   * @return all documents that passed the filter
   */
  @NonNull List<JsonDocument> get(@NonNull JsonDocument filters);

  /**
   * @return all keys of the database
   */
  @NonNull Collection<String> keys();

  /**
   * @return all documents of the database
   */
  @NonNull Collection<JsonDocument> documents();

  /**
   * Retrieves all keys and corresponding documents from the database.
   *
   * @return all entries of the database
   */
  @NonNull Map<String, JsonDocument> entries();

  /**
   * Clears the whole database
   */
  void clear();

  /**
   * @return the count of all persistent documents
   */
  long documentCount();

  /**
   * @return whether the database is synchronized (like MongoDB, MySQL) or not (like H2)
   */
  boolean synced();

  /**
   * Insert the given document for the given key
   *
   * @param key      the key in the database
   * @param document the document to be stored
   * @return whether the operation was successful or not
   */
  default @NonNull Task<Boolean> insertAsync(@NonNull String key, @NonNull JsonDocument document) {
    return CompletableTask.supply(() -> this.insert(key, document));
  }

  /**
   * Updates the given document for the given key
   *
   * @param key      the key in the database
   * @param document the document to be stored
   * @return whether the operation was successful or not
   */
  default @NonNull Task<Boolean> updateAsync(@NonNull String key, @NonNull JsonDocument document) {
    return CompletableTask.supply(() -> this.update(key, document));
  }

  /**
   * @param key the key to look for
   * @return whether the database contains the given key
   */
  default @NonNull Task<Boolean> containsAsync(@NonNull String key) {
    return CompletableTask.supply(() -> this.contains(key));
  }

  /**
   * Deletes the given key and corresponding data
   *
   * @param key the key to be deleted
   * @return whether the operation was successful or not
   */
  default @NonNull Task<Boolean> deleteAsync(@NonNull String key) {
    return CompletableTask.supply(() -> this.delete(key));
  }

  /**
   * Searches for a {@link JsonDocument} by the given key
   *
   * @param key the key to the document
   * @return the document associated with the key
   */
  default @NonNull Task<JsonDocument> getAsync(@NonNull String key) {
    return CompletableTask.supply(() -> this.get(key));
  }

  /**
   * Searches the database for a field that has the given fieldValue
   *
   * @param fieldName  the fieldName to look for
   * @param fieldValue the fieldValue associated with the fieldName
   * @return a List with the documents containing the field and fieldValue
   */
  default @NonNull Task<List<JsonDocument>> getAsync(@NonNull String fieldName, @Nullable Object fieldValue) {
    return CompletableTask.supply(() -> this.get(fieldName, fieldValue));
  }

  /**
   * Filters the database by the given document filters
   *
   * @param filters the filter to filter with
   * @return all documents that passed the filter
   */
  default @NonNull Task<List<JsonDocument>> getAsync(@NonNull JsonDocument filters) {
    return CompletableTask.supply(() -> this.get(filters));
  }

  /**
   * @return all keys of the database
   */
  default @NonNull Task<Collection<String>> keysAsync() {
    return CompletableTask.supply(this::keys);
  }

  /**
   * @return all documents of the database
   */
  default @NonNull Task<Collection<JsonDocument>> documentsAsync() {
    return CompletableTask.supply(this::documents);
  }

  /**
   * Retrieves all keys and corresponding documents from the database.
   *
   * @return all entries of the database
   */
  default @NonNull Task<Map<String, JsonDocument>> entriesAsync() {
    return CompletableTask.supply(this::entries);
  }

  /**
   * Clears the whole database
   */
  default @NonNull Task<Void> clearAsync() {
    return CompletableTask.supply(this::clear);
  }

  /**
   * @return the count of all persistent documents
   */
  default @NonNull Task<Long> documentCountAsync() {
    return CompletableTask.supply(this::documentCount);
  }
}
