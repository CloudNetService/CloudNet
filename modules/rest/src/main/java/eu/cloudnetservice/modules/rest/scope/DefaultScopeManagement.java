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

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import eu.cloudnetservice.node.http.RestScopeManagement;
import eu.cloudnetservice.node.http.RestUser;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@Provides(RestScopeManagement.class)
public class DefaultScopeManagement implements RestScopeManagement {

  private static final String REST_USER_DB_NAME = "REST_SCOPE_USER";

  private final LocalDatabase localDatabase;

  @Inject
  public DefaultScopeManagement(@NonNull NodeDatabaseProvider databaseProvider) {
    this.localDatabase = databaseProvider.database(REST_USER_DB_NAME);
  }

  @Override
  public @Nullable RestUser firstRestUser(@NonNull String name) {
    var users = this.findRestUsers(name);
    return users.isEmpty() ? null : users.get(0);
  }

  @Override
  public @NonNull List<RestUser> findRestUsers(@NonNull String name) {
    return this.localDatabase.find("name", name)
      .stream()
      .map(document -> document.toInstanceOf(DefaultRestUser.class))
      .collect(Collectors.toList());
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
}
