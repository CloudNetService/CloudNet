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

import com.google.common.hash.Hashing;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.node.http.RestUser;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record DefaultRestUser(
  @NonNull String id,
  @Nullable String passwordHash,
  @NonNull Set<String> scopes
) implements RestUser {

  @Override
  public boolean verifyPassword(@NonNull String password) {
    if (this.passwordHash == null) {
      return false;
    }

    var hash = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).asBytes();
    return this.passwordHash.equals(Base64.getEncoder().encodeToString(hash));
  }

  @Override
  public boolean hasScope(@NonNull String scope) {
    return this.scopes.contains("admin") || this.scopes.contains(StringUtil.toLower(scope));
  }

  @Override
  public void addScope(@NonNull String scope) {
    this.scopes.add(StringUtil.toLower(scope));
  }

  @Override
  public void removeScope(@NonNull String scope) {
    this.scopes.remove(StringUtil.toLower(scope));
  }

  @Override
  public @NonNull Set<String> scopes() {
    return Collections.unmodifiableSet(this.scopes);
  }
}
