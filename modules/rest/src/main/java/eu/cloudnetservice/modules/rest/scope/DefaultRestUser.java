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

package eu.cloudnetservice.modules.rest.scope;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.node.http.RestUser;
import eu.cloudnetservice.node.http.RestUserManagement;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

record DefaultRestUser(
  @NonNull String id,
  @Nullable String passwordHash,
  @NonNull Set<String> scopes
) implements RestUser {

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean verifyPassword(@NonNull String password) {
    if (this.passwordHash == null) {
      return false;
    }

    var hash = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).asBytes();
    return this.passwordHash.equals(Base64.getEncoder().encodeToString(hash));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasScope(@NonNull String scope) {
    return this.scopes.contains("admin") || this.scopes.contains(StringUtil.toLower(scope));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Set<String> scopes() {
    return Collections.unmodifiableSet(this.scopes);
  }

  /**
   * {@inheritDoc}
   */
  static class Builder implements RestUser.Builder {

    private String id;
    private String password;
    private final Set<String> scopes = new HashSet<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder id(@NonNull String id) {
      this.id = id;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder password(@Nullable String password) {
      this.password = password;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder addScope(@NonNull String scope) {
      var matcher = RestUserManagement.SCOPE_NAMING_PATTERN.matcher(scope);
      if (scope.equals("admin") || matcher.matches()) {
        this.scopes.add(StringUtil.toLower(scope));
      } else {
        throw new IllegalArgumentException(String.format(
          "The given scope %s does not match the desired scope regex %s",
          scope,
          RestUserManagement.SCOPE_NAMING_REGEX));
      }
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder removeScope(@NonNull String scope) {
      this.scopes.remove(StringUtil.toLower(scope));
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder scopes(@NonNull Set<String> scopes) {
      this.scopes.clear();
      for (var scope : scopes) {
        this.addScope(scope);
      }

      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull RestUser build() {
      Preconditions.checkNotNull(this.id, "Missing rest user id");

      return new DefaultRestUser(this.id, this.hashPassword(), this.scopes);
    }

    /**
     * Hashes the currently set password and encodes it using base64.
     *
     * @return the hashed and encoded password, null if no password was set.
     */
    private @Nullable String hashPassword() {
      if (this.password == null) {
        return null;
      }

      return Base64.getEncoder().encodeToString(
        Hashing.sha256().hashString(this.password, StandardCharsets.UTF_8).asBytes());
    }
  }
}
