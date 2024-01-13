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

package eu.cloudnetservice.modules.cloudperms.sponge.service.permissible.group;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.modules.cloudperms.sponge.service.permissible.AbstractSubjectCollection;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;

public final class CloudGroupCollection extends AbstractSubjectCollection {

  public CloudGroupCollection(String identifier, PermissionService service, PermissionManagement management) {
    super(identifier, service, management);
  }

  @Override
  public CompletableFuture<? extends Subject> loadSubject(String identifier) {
    return CompletableFuture.supplyAsync(() -> {
      var group = this.management.group(identifier);
      Preconditions.checkNotNull(group, "No group identified by " + identifier);
      return new PermissionGroupSubject(identifier, this, group, this.management);
    });
  }

  @Override
  public Optional<? extends Subject> subject(String identifier) {
    var group = this.management.group(identifier);
    return group == null
      ? Optional.empty()
      : Optional.of(new PermissionGroupSubject(identifier, this, group, this.management));
  }

  @Override
  public CompletableFuture<Boolean> hasSubject(String identifier) {
    return CompletableFuture.completedFuture(this.management.group(identifier) != null);
  }

  @Override
  public Collection<? extends Subject> loadedSubjects() {
    return this.management.groups().stream()
      .map(group -> new PermissionGroupSubject(group.name(), this, group, this.management))
      .collect(Collectors.toList());
  }

  @Override
  public CompletableFuture<? extends Set<String>> allIdentifiers() {
    return CompletableFuture.completedFuture(this.management.groups().stream()
      .map(PermissionGroup::name)
      .collect(Collectors.toSet()));
  }

  @Override
  public CompletableFuture<? extends Map<? extends SubjectReference, Boolean>> allWithPermission(
    String permission,
    Cause cause
  ) {
    return CompletableFuture.completedFuture(this.management.groups().stream().collect(Collectors.toMap(
      group -> this.newSubjectReference(group.name()),
      group -> this.management.hasPermission(group, Permission.of(permission)))));
  }

  @Override
  public Map<? extends Subject, Boolean> loadedWithPermission(String permission, Cause cause) {
    return this.management.groups().stream().collect(Collectors.toMap(
      group -> new PermissionGroupSubject(group.name(), this, group, this.management),
      group -> this.management.hasPermission(group, Permission.of(permission))));
  }
}
