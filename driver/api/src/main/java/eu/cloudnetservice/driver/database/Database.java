/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.database;

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.document.Document;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a database in the CloudNet. A database might be better known as a table (SQL) or collection (MongoDB).
 * CloudNet uses a database to store values mapped to a key. A key must be unique within the database but can be
 * overridden when a new value should get associated with a previously existing key. Every key is case-sensitive.
 * <p>
 * A database object can be obtained using {@link DatabaseProvider#database(String)} and should never be instantiated
 * directly (may lead to unexpected behaviour when using the database).
 * <p>
 * Furthermore, there is no guarantee that writes to a database are synced directly into the cluster. You can verify if
 * an operation will be directly visible to all nodes by calling {@link #synced()}. If the method returns false either
 * the server owner should consider changing to a solution which syncs instantly in the cluster, or you might run into
 * synchronization problems when operating on the same data from different nodes.
 * <p>
 * Warning: In normal cases it is not recommended using the CloudNet database system for all of your data. The system
 * will just write the value document as a plain string into the database which might lead to problems with some
 * databases which are unable to handle such a mass of data. On the other hand it is much easier to use another
 * structure if you want to randomly access single data fields instead of always accessing the hole chunk of data stored
 * in the database.
 * <p>
 * Note: Great care must be taken when trying to read all values or key-value pairs from a database. The operation might
 * take a while and for components which are not a node they need to get transferred over the network. Same thing
 * applied for searches based on an entry in the database. These searches are not deep, meaning that you can only search
 * reliably for top level entries in the value json rather than nested searches.
 *
 * @see DatabaseProvider
 * @since 4.0
 */
public interface Database extends Named, AutoCloseable {

  /**
   * Associates the given key with the given document in the database. A key should be unique for later identification
   * when trying other operations on the key like read or remove. If the key already exists it will get overridden.
   *
   * @param key      the unique key for the document.
   * @param document the document to associate with the key.
   * @return true if the document was associated with the key successfully, false otherwise.
   * @throws NullPointerException if either key or document is null.
   */
  boolean insert(@NonNull String key, @NonNull Document document);

  /**
   * Tests whether a document is associated with the given key.
   *
   * @param key the key to check.
   * @return true if the database contains the given key, false otherwise.
   * @throws NullPointerException if key is null.
   */
  boolean contains(@NonNull String key);

  /**
   * Removes the key and the associated document from the database.
   *
   * @param key the key to remove.
   * @return true if the key and document were removed from the database, false otherwise.
   * @throws NullPointerException if key is null.
   */
  boolean delete(@NonNull String key);

  /**
   * Gets the associated document with the given key from the database. If the returned document is null than there is
   * no document associated with the given key.
   *
   * @param key the key of the document to get.
   * @return the document associated with the key or null if there is no document associated with the key.
   * @throws NullPointerException if key is null.
   */
  @Nullable
  Document get(@NonNull String key);

  /**
   * Searches for all entries in the database which value contains the given field and the field value matches the given
   * value. Null as the field value is permitted and will be used as literally null. The search is not deep meaning that
   * you can only reliably search for top-level value mappings, nested types might work but will most likely not.
   *
   * @param fieldName  the name of the field which the document value must contain.
   * @param fieldValue the value of the field which the document must have. Null is valid.
   * @return all documents in the database which contain the given field mapped to the given field value.
   * @throws NullPointerException if fieldName is null.
   */
  @NonNull
  Collection<Document> find(@NonNull String fieldName, @Nullable String fieldValue);

  /**
   * Searches for all entries in the database which contain each entry of the provided map. Null as a field value is
   * permitted and will be used as literally null. The search is not deep meaning that you can only reliably search for
   * top-level value mappings, nested types might work but will most likely not.
   *
   * @param filters the map containing the key-value pairs which the searched database document must contain.
   * @return all documents in the database which contain all key-value mappings of the filter document.
   * @throws NullPointerException if filters is null.
   */
  @NonNull
  Collection<Document> find(@NonNull Map<String, String> filters);

  /**
   * Get all keys which are currently stored and mapped to a document in the database. This operation might be heavy
   * when querying a huge database.
   *
   * @return all keys stored in the database.
   */
  @NonNull
  Collection<String> keys();

  /**
   * Get all values which are currently stored and mapped to a key in the database. This operation might be heavy when
   * querying a huge database.
   *
   * @return all documents stored in the database.
   */
  @NonNull
  Collection<Document> documents();

  /**
   * Get all key-value pairs which are currently stored in the database. This operation might be heavy when querying a
   * huge database.
   *
   * @return all key-value pairs stored in the database.
   */
  @NonNull
  Map<String, Document> entries();

  /**
   * Removes all key-value pairs which are currently stored in the database. This operation will not remove the
   * database.
   */
  void clear();

  /**
   * Get the amount of key-value pairs currently stored in the database.
   *
   * @return the amount of key-value pairs in the database.
   */
  long documentCount();

  /**
   * Gets if this database is synced to the cluster. This means that every change made to the database will be directly
   * visible to all components in the cluster rather than requiring a special sync. Normally synced databases are
   * databases which are running as an external process, like MySQL or MongoDB.
   *
   * @return true if all modify operations are directly visible to all components in a cluster, false otherwise.
   */
  boolean synced();

  /**
   * Associates the given key with the given document in the database. A key should be unique for later identification
   * when trying other operations on the key like read or remove. If the key already exists it will get overridden.
   * <p>
   * The returned future, if completed successfully, completes with true to indicate that the value was written into the
   * database successfully. Will be completed with false if the data wasn't written without specifying a reason.
   *
   * @param key      the unique key for the document.
   * @param document the document to associate with the key.
   * @return a future completed with the write operation status.
   * @throws NullPointerException if either key or document is null.
   */
  @NonNull
  CompletableFuture<Boolean> insertAsync(@NonNull String key, @NonNull Document document);

  /**
   * Tests whether a document is associated with the given key.
   * <p>
   * The returned future, if completed successfully, completes with true to indicate that the database contains the
   * given key and with false to indicate that either the database does not contain the given key or the lookup failed
   * without giving a reason for that.
   *
   * @param key the key to check.
   * @return a future completed with the lookup status when completed.
   * @throws NullPointerException if key is null.
   */
  @NonNull
  CompletableFuture<Boolean> containsAsync(@NonNull String key);

  /**
   * Removes the key and the associated document from the database.
   * <p>
   * The returned future, if completed successfully, completes with true to indicate that the database no longer
   * contains a mapping for the given key and with false to indicate that there was an issue removing the key from the
   * database without specifying a reason.
   *
   * @param key the key to remove.
   * @return a future completed with the removal status of the given key.
   * @throws NullPointerException if key is null.
   */
  @NonNull
  CompletableFuture<Boolean> deleteAsync(@NonNull String key);

  /**
   * Gets the associated document with the given key from the database. If the returned document is null than there is
   * no document associated with the given key.
   * <p>
   * The returned future, if completed successfully, completes with the document mapped to the given key in the database
   * and with null if either the lookup in the database failed or no document in the database is associated with the
   * given key.
   *
   * @param key the key of the document to get.
   * @return a future completed with the document associated with the given key.
   * @throws NullPointerException if key is null.
   */
  @NonNull
  CompletableFuture<Document> getAsync(@NonNull String key);

  /**
   * Searches for all entries in the database which value contains the given field and the field value matches the given
   * value. Null as the field value is permitted and will be used as literally null. The search is not deep meaning that
   * you can only reliably search for top-level value mappings, nested types might work but will most likely not.
   * <p>
   * The returned future, if completed successfully, completes with a collection of documents which are all matching the
   * given field key/value matcher or with an empty collection if either the lookup failed or the database does not
   * contain any document matching the field key/value.
   *
   * @param fieldName  the name of the field which the document value must contain.
   * @param fieldValue the value of the field which the document must have. Null is valid.
   * @return a future completed with all documents matching the given field key/value.
   * @throws NullPointerException if fieldName is null.
   */
  @NonNull
  CompletableFuture<Collection<Document>> findAsync(@NonNull String fieldName, @Nullable String fieldValue);

  /**
   * Searches for all entries in the database which contain each entry of the provided map. Null as a field value is
   * permitted and will be used as literally null. The search is not deep meaning that you can only reliably search for
   * top-level value mappings, nested types might work but will most likely not.
   * <p>
   * The returned future, if completed successfully, completes with a collection of documents which are all matching the
   * given filters or with an empty collection if either the lookup failed or the database does not contain any document
   * matching the given filters.
   *
   * @param filters the map containing the key-value pairs which the searched database document must contain.
   * @return a future completed with all documents matching the given filters.
   * @throws NullPointerException if filters is null.
   */
  @NonNull
  CompletableFuture<Collection<Document>> findAsync(@NonNull Map<String, String> filters);

  /**
   * Get all keys which are currently stored and mapped to a document in the database. This operation might be heavy
   * when querying a huge database.
   * <p>
   * The returned future, if completed successfully, completes with a collection of all keys which are currently stored
   * in the database or an empty collection if the lookup failed or the database does not contain any keys.
   *
   * @return a future completed with all keys which are currently stored in the database.
   */
  @NonNull
  CompletableFuture<Collection<String>> keysAsync();

  /**
   * Get all values which are currently stored and mapped to a key in the database. This operation might be heavy when
   * querying a huge database.
   * <p>
   * The returned future, if completed successfully, completes with a collection of all documents which are currently
   * stored in the database or an empty collection if the lookup failed or the database does not contain any documents.
   *
   * @return a future completed with all documents which are currently stored in the database.
   */
  @NonNull
  CompletableFuture<Collection<Document>> documentsAsync();

  /**
   * Get all key-value pairs which are currently stored in the database. This operation might be heavy when querying a
   * huge database.
   * <p>
   * The returned future, if completed successfully, completes with all key-value pairs which are currently stored in
   * the database or an empty collection if the lookup failed or the database does not contain anything.
   *
   * @return a future completed with all key-value pairs currently stored in the database.
   */
  @NonNull
  CompletableFuture<Map<String, Document>> entriesAsync();

  /**
   * Removes all key-value pairs which are currently stored in the database. This operation will not remove the
   * database.
   * <p>
   * The returned future is just for listening reasons and has no special return value. It will always get completed
   * with null.
   *
   * @return a future completed when the operation took place.
   */
  @NonNull
  CompletableFuture<Void> clearAsync();

  /**
   * Get the amount of key-value pairs currently stored in the database.
   * <p>
   * The returned future, if completed successfully, completes with the amount of key-value pairs currently stored in
   * the database or 0 if the lookup failed or the database does not contain anything.
   *
   * @return a future completed with the amount of documents currently stored in the database.
   */
  @NonNull
  CompletableFuture<Long> documentCountAsync();
}
