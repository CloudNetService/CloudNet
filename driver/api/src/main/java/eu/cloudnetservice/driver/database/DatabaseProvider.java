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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

/**
 * Represents a provider for databases. A database in CloudNet might better be known as a table (SQL) or collection
 * (MongoDB). Therefore, a database provider represents a collection of databases, also known as a Database
 * (SQL/MongoDB).
 * <p>
 * Every database must have a unique name which is case-sensitive. When retrieving a database there is no difference
 * between a database which runs externally or a database which is embedded. The creation of a new database (if
 * required) should be executed when retrieving the database object. For remote databases (retrieval while not being on
 * a node component) the create operation can be executed when the database is used for the first time. Therefore, the
 * method {@link #database(String)} should never suspend the calling thread.
 * <p>
 * Deleting a database must remove all key-value pairs which are stored in the database. There is no guarantee that a
 * database request/delete is directly visible to all components in the cluster as stated in {@link Database}.
 * <p>
 * Warning: In normal cases it is not recommended using the CloudNet database system for all of your data. The system
 * will just write the value document as a plain string into the database which might lead to problems with some
 * databases which are unable to handle such a mass of data. On the other hand it is much easier to use another
 * structure if you want to randomly access single data fields instead of always accessing the hole chunk of data stored
 * in the database.
 *
 * @see Database
 * @since 4.0
 */
public interface DatabaseProvider {

  /**
   * Retrieves or creates non-blocking a facade for a database to write and read data to. The name of the database
   * should be unique for later identification.
   *
   * @param name the unique name of the database.
   * @return a facade for a database to write and read data to.
   * @throws NullPointerException if name is null.
   */
  @NonNull
  Database database(@NonNull String name);

  /**
   * Checks whether the database with the given name already exists. When a call to {@link #database(String)} is made
   * there is no requirement for the database to get created.
   *
   * @param name the name of the database to check.
   * @return true if a database with the given name exists, false otherwise.
   * @throws NullPointerException if name is null.
   */
  boolean containsDatabase(@NonNull String name);

  /**
   * Deletes the database with the given name, removing all data which was previously stored in it.
   *
   * @param name the name of the database to remove.
   * @return true if the database was deleted and all data in it got dropped, false otherwise
   * @throws NullPointerException if name is null.
   */
  boolean deleteDatabase(@NonNull String name);

  /**
   * Retrieves all names of all top-level existing databases. When a call to {@link #database(String)} is made there is
   * no requirement for the database to get created.
   *
   * @return all names of all top-level existing databases.
   */
  @NonNull
  Collection<String> databaseNames();

  /**
   * Checks whether the database with the given name already exists. When a call to {@link #database(String)} is made
   * there is no requirement for the database to get created.
   * <p>
   * The returned future, if completed successfully, completes with true to indicate that the database with the given
   * name already exists and with false if either the lookup failed or the database does not exist.
   *
   * @param name the name of the database to check.
   * @return a future completed with the existence status of the database.
   * @throws NullPointerException if name is null.
   */
  @NonNull
  CompletableFuture<Boolean> containsDatabaseAsync(@NonNull String name);

  /**
   * Deletes the database with the given name, removing all data which was previously stored in it.
   * <p>
   * The returned future, if completed successfully, completes with true to indicate that the database with the given
   * name was removed successfully and all stored data in it was removed and with false if either the deletion failed or
   * the database does not exist.
   *
   * @param name the name of the database to remove.
   * @return a future completed with the deletion result of the database.
   * @throws NullPointerException if name is null.
   */
  @NonNull
  CompletableFuture<Boolean> deleteDatabaseAsync(@NonNull String name);

  /**
   * Retrieves all names of all top-level existing databases. When a call to {@link #database(String)} is made there is
   * no requirement for the database to get created.
   * <p>
   * The returned future, if completed successfully, completes with a collection of all existing top-level database
   * names or with an empty collection if either the query failed or no databases are existing.
   *
   * @return a future completed with a collection of all database names.
   */
  @NonNull
  CompletableFuture<Collection<String>> databaseNamesAsync();
}
