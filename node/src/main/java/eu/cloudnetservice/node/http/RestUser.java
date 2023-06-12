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

import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The rest user represents an authorized user on the cloudnet http server. Combined with the
 * {@link eu.cloudnetservice.node.http.annotation.SecurityAnnotationExtension} security and authorization can be done.
 * <p>
 * Obtaining a rest user can be done using {@link RestUserManagement#restUser(String)}.
 *
 * @see RestUser.Builder
 * @see RestUserManagement
 * @since 4.0
 */
public interface RestUser {

  /**
   * Gets the id of this rest user.
   *
   * @return the id of this rest user.
   */
  @NonNull String id();

  /**
   * Checks if the given password matches the password set for this rest user.
   *
   * @param password the password to check.
   * @return true if the passwords match, false otherwise.
   * @throws NullPointerException if the given password is null.
   */
  boolean verifyPassword(@NonNull String password);

  /**
   * Gets the hashed password of the rest user, {@code null} if no password was set.
   *
   * @return hashed password of the rest user.
   */
  @Nullable String passwordHash();

  /**
   * Checks whether the user has the given scope.
   *
   * @param scope the scope to check.
   * @return true if the rest user has that scope assigned, false otherwise.
   * @throws NullPointerException if the given scope is null.
   */
  boolean hasScope(@NonNull String scope);

  /**
   * Checks whether the user has at least one of the given scopes.
   *
   * @param scopes the scopes to check.
   * @return true if the user has at least one of the given scopes.
   * @throws NullPointerException if the given scopes array is null.
   */
  default boolean hasOneScopeOf(@NonNull String[] scopes) {
    for (var scope : scopes) {
      if (this.hasScope(scope)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets an unmodifiable view of the scopes the rest user has.
   *
   * @return an unmodifiable view of the scopes the rest user has.
   */
  @Unmodifiable
  @NonNull Set<String> scopes();

  /**
   * The rest user builder used to create and modify rest users to ensure immutability of the rest user itself.
   * <p>
   * Obtain a fresh builder instance using {@link RestUserManagement#builder()} or
   * {@link RestUserManagement#builder(RestUser)} if you want to copy existing properties.
   *
   * @see RestUser
   * @see RestUserManagement
   * @since 4.0
   */
  interface Builder {

    /**
     * Sets the id of the rest user.
     *
     * @param id the id to set.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given id is null.
     */
    @NonNull Builder id(@NonNull String id);

    /**
     * Sets the password of the rest user. Passing {@code null} implies that the user does not have a password.
     *
     * @param password the password to set.
     * @return the same instance as used to call the method, for chaining.
     */
    @NonNull Builder password(@Nullable String password);

    /**
     * Adds the given scope to the rest users scopes. The scope has to follow the
     * {@link RestUserManagement#SCOPE_NAMING_REGEX} regex pattern. The only exception to that is the {@code admin}
     * scope that grants access to everything.
     *
     * @param scope the scope to add to the rest users scope.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException     if the given scope is null.
     * @throws IllegalArgumentException if the scope does not follow the mentioned regex pattern.
     */
    @NonNull Builder addScope(@NonNull String scope);

    /**
     * Removes the given scope from the rest users scopes.
     *
     * @param scope the scope to remove from the rest users scopes.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given scope is null.
     */
    @NonNull Builder removeScope(@NonNull String scope);

    /**
     * Sets all scopes of the rest user, overwriting already set scopes. The scopes have to follow the
     * {@link RestUserManagement#SCOPE_NAMING_REGEX} regex pattern. The only exception to that is the {@code admin}
     * scope that grants access to everything.
     *
     * @param scopes the scopes to set.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException     if the given scope set is null.
     * @throws IllegalArgumentException if the scopes do not follow the mentioned regex pattern.
     */
    @NonNull Builder scopes(@NonNull Set<String> scopes);

    /**
     * Creates the rest user from this builder.
     *
     * @return the newly built rest user from this builder.
     * @throws NullPointerException if no id was set.
     */
    @NonNull RestUser build();
  }
}
