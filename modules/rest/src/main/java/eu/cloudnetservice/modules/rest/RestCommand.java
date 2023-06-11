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

package eu.cloudnetservice.modules.rest;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.context.CommandContext;
import com.google.common.hash.Hashing;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.registry.injection.Service;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.http.RestUser;
import eu.cloudnetservice.node.http.RestUserManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@CommandPermission("cloudnet.command.rest")
@Description("module-rest-command-description")
public final class RestCommand {

  private final RestUserManagement restUserManagement;

  @Inject
  public RestCommand(@NonNull @Service RestUserManagement restUserManagement) {
    this.restUserManagement = restUserManagement;
  }

  @Parser
  public @NonNull RestUser defaultRestUserParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var userName = input.remove();
    var user = this.restUserManagement.restUser(userName);
    if (user == null) {
      throw new ArgumentNotAvailableException(I18n.trans("module-rest-user-not-found", userName));
    }

    return user;
  }

  @Parser(name = "restUserScope")
  public @NonNull String restUserScopeParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var scope = input.remove();
    if (scope.equalsIgnoreCase("admin") || RestUserManagement.SCOPE_NAMING_PATTERN.matcher(scope).matches()) {
      return scope;
    }

    throw new ArgumentNotAvailableException(I18n.trans(
      "argument-parse-failure-regex",
      RestUserManagement.SCOPE_NAMING_REGEX,
      scope));
  }

  @CommandMethod("rest user create <id> [password]")
  public void createRestUser(
    @NonNull CommandSource source,
    @Argument("id") @NonNull String userId,
    @Argument("password") @Nullable String password
  ) {
    if (this.restUserManagement.restUser(userId) != null) {
      source.sendMessage(I18n.trans("module-rest-user-already-existing", userId));
      return;
    }

    this.restUserManagement.saveRestUser(this.restUserManagement.builder().id(userId).password(password).build());
    source.sendMessage(I18n.trans("module-rest-user-create-successful", userId));
  }

  @CommandMethod("rest user delete <id>")
  public void deleteRestUser(@NonNull CommandSource source, @Argument("id") @NonNull RestUser restUser) {
    this.restUserManagement.deleteRestUser(restUser);
    source.sendMessage(I18n.trans("module-rest-user-delete-successful", restUser.id()));
  }

  @CommandMethod("rest user <id>")
  public void displayUser(@NonNull CommandSource source, @Argument("id") @NonNull RestUser restUser) {
    source.sendMessage("RestUser " + restUser.id());
    source.sendMessage("Scopes:");
    for (var scope : restUser.scopes()) {
      source.sendMessage(" - " + scope);
    }
  }

  @CommandMethod("rest user <id> add scope <scope>")
  public void addScope(
    @NonNull CommandSource source,
    @Argument("id") @NonNull RestUser restUser,
    @Argument(value = "scope", parserName = "restUserScope") @NonNull String scope
  ) {
    this.updateRestUser(restUser, builder -> builder.addScope(scope));
    source.sendMessage(I18n.trans("module-rest-user-add-scope-successful", restUser.id(), scope));
  }

  @CommandMethod("rest user <id> clear scopes")
  public void clearScopes(@NonNull CommandSource source, @Argument("id") @NonNull RestUser restUser) {
    this.updateRestUser(restUser, builder -> builder.scopes(Set.of()));
    source.sendMessage(I18n.trans("module-rest-user-clear-scopes-successful", restUser.id()));
  }

  @CommandMethod("rest user <id> remove scope <scope>")
  public void removeScope(
    @NonNull CommandSource source,
    @Argument("id") @NonNull RestUser restUser,
    @Argument("scope") @NonNull String scope
  ) {
    this.updateRestUser(restUser, builder -> builder.removeScope(scope));
    source.sendMessage(I18n.trans("module-rest-user-remove-scope-successful", restUser.id(), scope));
  }

  @CommandMethod("rest user <id> set password <password>")
  public void setPassword(
    @NonNull CommandSource source,
    @Argument("id") @NonNull RestUser restUser,
    @Argument("password") @NonNull String password
  ) {
    this.updateRestUser(restUser, builder -> builder.password(password));
    source.sendMessage(I18n.trans("module-rest-user-password-changed", restUser.id()));
  }

  @CommandMethod("rest user <id> verifyPassword <password>")
  public void verifyPassword(
    @NonNull CommandSource source,
    @Argument("id") @NonNull RestUser restUser,
    @Argument("password") @NonNull String password
  ) {
    var hash = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).asBytes();

    if (Base64.getEncoder().encodeToString(hash).equals(restUser.passwordHash())) {
      source.sendMessage(I18n.trans("module-rest-user-password-match", restUser.id()));
    } else {
      source.sendMessage(I18n.trans("module-rest-user-password-mismatch", restUser.id()));
    }
  }

  private void updateRestUser(@NonNull RestUser user, @NonNull Consumer<RestUser.Builder> consumer) {
    var builder = this.restUserManagement.builder(user);
    consumer.accept(builder);
    this.restUserManagement.saveRestUser(builder.build());
  }
}
