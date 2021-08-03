/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import org.jetbrains.annotations.NotNull;

/**
 * The database is used to store cloudnet related data, these databases can be used by an api user too. To create or
 * retrieve a database see {@link de.dytanic.cloudnet.driver.database.DatabaseProvider}
 */
public interface Database extends INameable, AutoCloseable {

  /**
   * Insert the given document for the given key
   *
   * @param key      the key in the database
   * @param document the document to be stored
   * @return whether the operation was successful or not
   */
  boolean insert(String key, JsonDocument document);

  /**
   * Updates the given document for the given key
   *
   * @param key      the key in the database
   * @param document the document to be stored
   * @return whether the operation was successful or not
   */
  boolean update(String key, JsonDocument document);

  /**
   * @param key the key to look for
   * @return whether the database contains the given key
   */
  boolean contains(String key);

  /**
   * Deletes the given key and corresponding data
   *
   * @param key the key to be deleted
   * @return whether the operation was successful or not
   */
  boolean delete(String key);

  /**
   * Searches for a {@link JsonDocument} by the given key
   *
   * @param key the key to the document
   * @return the document associated with the key
   */
  JsonDocument get(String key);

  /**
   * Searches the database for a field that has the given fieldValue
   *
   * @param fieldName  the fieldName to look for
   * @param fieldValue the fieldValue associated with the fieldName
   * @return a List with the documents containing the field and fieldValue
   */
  List<JsonDocument> get(String fieldName, Object fieldValue);

  /**
   * Filters the database by the given document filters
   *
   * @param filters the filter to filter with
   * @return all documents that passed the filter
   */
  List<JsonDocument> get(JsonDocument filters);

  /**
   * @return all keys of the database
   */
  Collection<String> keys();

  /**
   * @return all documents of the database
   */
  Collection<JsonDocument> documents();

  /**
   * Retrieves all keys and corresponding documents from the database. This option should not be used with big databases
   * Use {@link Database#iterate(BiConsumer, int)}} instead
   *
   * @return all entries of the database
   */
  Map<String, JsonDocument> entries();

  /**
   * Retrieves all entries that match the given filter predicate
   *
   * @param predicate the filter for the entries
   * @return all entries that match the filter
   */
  Map<String, JsonDocument> filter(BiPredicate<String, JsonDocument> predicate);

  /**
   * Iterates over all entries in the database This option should not be used with big databases Use {@link
   * Database#iterate(BiConsumer, int)}} instead
   *
   * @param consumer the consumer to pass the entries into
   */
  void iterate(BiConsumer<String, JsonDocument> consumer);

  /**
   * Iterates over all entries in the database, but in chunks in the given size
   *
   * @param consumer  the consumer to pass the entries into
   * @param chunkSize the chunkSize of the entries
   */
  void iterate(BiConsumer<String, JsonDocument> consumer, int chunkSize);

  /**
   * Clears the whole database
   */
  void clear();

  /**
   * @return the count of all persistent documents
   */
  long getDocumentsCount();

  /**
   * @return whether the database is synchronized (like MongoDB, MySQL) or not (like H2)
   */
  boolean isSynced();

  /**
   * Insert the given document for the given key
   *
   * @param key      the key in the database
   * @param document the document to be stored
   * @return whether the operation was successful or not
   */
  @NotNull
  ITask<Boolean> insertAsync(String key, JsonDocument document);

  /**
   * Updates the given document for the given key
   *
   * @param key      the key in the database
   * @param document the document to be stored
   * @return whether the operation was successful or not
   */
  @NotNull
  ITask<Boolean> updateAsync(String key, JsonDocument document);

  /**
   * @param key the key to look for
   * @return whether the database contains the given key
   */
  @NotNull
  ITask<Boolean> containsAsync(String key);

  /**
   * Deletes the given key and corresponding data
   *
   * @param key the key to be deleted
   * @return whether the operation was successful or not
   */
  @NotNull
  ITask<Boolean> deleteAsync(String key);

  /**
   * Searches for a {@link JsonDocument} by the given key
   *
   * @param key the key to the document
   * @return the document associated with the key
   */
  @NotNull
  ITask<JsonDocument> getAsync(String key);

  /**
   * Searches the database for a field that has the given fieldValue
   *
   * @param fieldName  the fieldName to look for
   * @param fieldValue the fieldValue associated with the fieldName
   * @return a List with the documents containing the field and fieldValue
   */
  @NotNull
  ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue);

  /**
   * Filters the database by the given document filters
   *
   * @param filters the filter to filter with
   * @return all documents that passed the filter
   */
  @NotNull
  ITask<List<JsonDocument>> getAsync(JsonDocument filters);

  /**
   * @return all keys of the database
   */
  @NotNull
  ITask<Collection<String>> keysAsync();

  /**
   * @return all documents of the database
   */
  @NotNull
  ITask<Collection<JsonDocument>> documentsAsync();

  /**
   * Retrieves all keys and corresponding documents from the database. This option should not be used with big databases
   * Use {@link Database#iterate(BiConsumer, int)}} instead
   *
   * @return all entries of the database
   */
  @NotNull
  ITask<Map<String, JsonDocument>> entriesAsync();

  /**
   * Retrieves all entries that match the given filter predicate
   *
   * @param predicate the filter for the entries
   * @return all entries that match the filter
   */
  @NotNull
  ITask<Map<String, JsonDocument>> filterAsync(BiPredicate<String, JsonDocument> predicate);

  /**
   * Iterates over all entries in the database This option should not be used with big databases Use {@link
   * Database#iterate(BiConsumer, int)}} instead
   *
   * @param consumer the consumer to pass the entries into
   */
  @NotNull
  ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer);

  /**
   * Iterates over all entries in the database, but in chunks in the given size
   *
   * @param consumer  the consumer to pass the entries into
   * @param chunkSize the chunkSize of the entries
   */
  @NotNull
  ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer, int chunkSize);

  /**
   * Clears the whole database
   */
  @NotNull
  ITask<Void> clearAsync();

  /**
   * @return the count of all persistent documents
   */
  @NotNull
  ITask<Long> getDocumentsCountAsync();

}
