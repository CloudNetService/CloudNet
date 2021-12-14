/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.cloudperms.sponge.service.permissible.user;

import static de.dytanic.cloudnet.ext.cloudperms.sponge.service.permissible.user.UUIDUtil.parseFromString;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.sponge.service.permissible.AbstractSubjectCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;

public final class CloudUserCollection extends AbstractSubjectCollection {

  public CloudUserCollection(
    String identifier,
    PermissionService service,
    IPermissionManagement management
  ) {
    super(identifier, service, management);
  }

  @Override
  public CompletableFuture<? extends Subject> loadSubject(String identifier) {
    return CompletableFuture.supplyAsync(() -> {
      var user = this.management.getUser(parseFromString(identifier));
      Verify.verifyNotNull(user, "No user with identifier " + identifier);
      return new PermissionUserSubject(identifier, this, user, this.management);
    });
  }

  @Override
  public Optional<? extends Subject> subject(String identifier) {
    var management = CloudPermissionsHelper.asCachedPermissionManagement(this.management);
    // if the permission management support caches try to load the user from the cache
    if (management != null) {
      var user = management.getCachedUser(parseFromString(identifier));
      if (user != null) {
        return Optional.of(new PermissionUserSubject(identifier, this, user, this.management));
      }
    }
    // no such user or the permission management supports no caches
    return Optional.empty();
  }

  @Override
  public CompletableFuture<Boolean> hasSubject(String id) {
    return CompletableFuture.supplyAsync(() -> this.management.getUser(parseFromString(id)) != null);
  }

  @Override
  public Collection<? extends Subject> loadedSubjects() {
    return Collections.emptySet(); // not supported
  }

  @Override
  public CompletableFuture<? extends Set<String>> allIdentifiers() {
    return CompletableFuture.supplyAsync(() -> this.management.getUsers().stream()
      .map(PermissionUser::getName)
      .collect(Collectors.toSet()));
  }

  @Override
  public CompletableFuture<? extends Map<? extends SubjectReference, Boolean>> allWithPermission(
    String permission,
    Cause cause
  ) {
    return CompletableFuture.completedFuture(Collections.emptyMap()); // not supported
  }

  @Override
  public Map<? extends Subject, Boolean> loadedWithPermission(String permission, Cause cause) {
    return Collections.emptyMap(); // not supported
  }

  @Override
  public Predicate<String> identifierValidityPredicate() {
    return id -> UUIDUtil.parseFromStringOrNull(id) != null;
  }
}
