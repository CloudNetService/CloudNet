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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import eu.cloudnetservice.node.http.RestUser;
import eu.cloudnetservice.node.http.RestUserManagement;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultUserManagement implements RestUserManagement {

  private static final String REST_USER_DB_NAME = "REST_SCOPE_USER";

  private final LocalDatabase localDatabase;

  public DefaultUserManagement(@NonNull NodeDatabaseProvider databaseProvider) {
    this.localDatabase = databaseProvider.database(REST_USER_DB_NAME);
  }

  @Override
  public @Nullable RestUser restUser(@NonNull String id) {
    var user = this.localDatabase.get(id);
    if (user == null) {
      return null;
    }

    return user.toInstanceOf(DefaultRestUser.class);
  }

  @Override
  public void saveRestUser(@NonNull RestUser user) {
    this.localDatabase.insert(user.id(), Document.newJsonDocument().appendTree(user));
  }

  @Override
  public boolean deleteRestUser(@NonNull RestUser user) {
    return this.localDatabase.delete(user.id());
  }

  @Override
  public @NonNull RestUser.Builder builder() {
    return new DefaultRestUser.Builder();
  }

  @Override
  public @NonNull RestUser.Builder builder(@NonNull RestUser restUser) {
    return this.builder()
      .id(restUser.id())
      .password(restUser.passwordHash())
      .scopes(restUser.scopes());
  }
}
