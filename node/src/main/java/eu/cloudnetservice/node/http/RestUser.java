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

import eu.cloudnetservice.common.Named;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface RestUser extends Named {

  @NonNull String id();

  boolean verifyPassword(@NonNull String password);

  @Nullable String passwordHash();

  boolean hasScope(@NonNull String scope);

  default boolean hasOneScopeOf(@NonNull String[] @NonNull scopes) {
    for (var scope : scopes) {
      if (this.hasScope(scope)) {
        return true;
      }
    }

    return false;
  }

  void addScope(@NonNull String scope);

  void removeScope(@NonNull String scope);

  @NonNull Set<String> scopes();

}
