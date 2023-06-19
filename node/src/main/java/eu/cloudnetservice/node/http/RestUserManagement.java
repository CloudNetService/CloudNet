/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.http;

import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The rest user management is responsible for the database management of the rest users. By default, an implementation
 * by provided by the cloudnet rest module, but the rest user system can also be replaced by own implementations.
 * <p>
 * The rest user management can be accessed using {@code ServiceRegistry.first(RestUserManagement.class)} or by
 * injecting it directly in combination with the {@link eu.cloudnetservice.driver.registry.injection.Service}
 * annotation.
 * <p>
 * In order to replace the rest user management use
 * {@link eu.cloudnetservice.driver.registry.ServiceRegistry#registerProvider(Class, String, Object)}
 *
 * @see RestUser
 * @see eu.cloudnetservice.driver.registry.injection.Service
 * @see eu.cloudnetservice.driver.registry.ServiceRegistry
 * @since 4.0
 */
@ApiStatus.Experimental
public interface RestUserManagement {

  String SCOPE_NAMING_REGEX = "(^[a-z][a-z0-9_]{4,39}):([a-z0-9.\\-_]+)";

  // https://regex101.com/r/3nG0Nu/1
  Pattern SCOPE_NAMING_PATTERN = Pattern.compile(SCOPE_NAMING_REGEX);

  /**
   * Gets the rest user associated with the given id. The rest user must have been created previously using
   * {@link #saveRestUser(RestUser)}.
   * <p>
   * The rest user is always retrieved from the database, there is no caching.
   *
   * @param id the user id of the rest user to get from the database.
   * @return the rest user with the given id or null if there is no rest user.
   * @throws NullPointerException if the given id is null.
   */
  @Nullable RestUser restUser(@NonNull String id);

  /**
   * Creates and updates the given rest user. If the given user does not exist the user is created and saved otherwise
   * we update the users information in the database.
   *
   * @param user the user to save to the database.
   * @throws NullPointerException if the given user is null.
   */
  void saveRestUser(@NonNull RestUser user);

  /**
   * Deletes the given rest user from the database with immediate effect.
   *
   * @param user the user to delete from the database.
   * @return true if the user was deleted, false otherwise.
   * @throws NullPointerException if the given user is null.
   */
  boolean deleteRestUser(@NonNull RestUser user);

  /**
   * Gets a new rest user builder.
   *
   * @return a new rest user builder.
   */
  @NonNull RestUser.Builder builder();

  /**
   * Gets a new rest user builder copying all properties from the given rest user into the new builder.
   *
   * @param restUser the user to copy all properties from.
   * @return a new rest user builder copying everything from the given user.
   * @throws NullPointerException if the given rest user is null.
   */
  @NonNull RestUser.Builder builder(@NonNull RestUser restUser);

}
