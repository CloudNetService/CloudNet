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

package de.dytanic.cloudnet.ext.cloudperms.sponge.service.permissible.group;

import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.sponge.service.permissible.PermissibleSubjectData;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.service.permission.TransferMethod;

final class PermissionGroupData extends PermissibleSubjectData<PermissionGroup> {

  public PermissionGroupData(
    boolean allowModify,
    PermissionGroup permissible,
    Subject subject,
    PermissionManagement management
  ) {
    super(allowModify, permissible, subject, management);
  }

  @Override
  protected void update(@NonNull PermissionGroup data) {
    this.management.updateGroup(data);
  }

  @Override
  public CompletableFuture<Boolean> setParents(
    Set<Context> contexts,
    List<? extends SubjectReference> groups,
    TransferMethod method
  ) {
    return CompletableFuture.supplyAsync(() -> {
      this.permissible.groups(groups.stream().map(SubjectReference::subjectIdentifier).collect(Collectors.toList()));
      this.updateIfEnabled(this.permissible);
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> addParent(Set<Context> contexts, SubjectReference parent) {
    return CompletableFuture.supplyAsync(() -> {
      this.permissible.groupNames().add(parent.subjectIdentifier());
      this.updateIfEnabled(this.permissible);
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> removeParent(Set<Context> contexts, SubjectReference parent) {
    return CompletableFuture.supplyAsync(() -> {
      this.permissible.groupNames().remove(parent.subjectIdentifier());
      this.updateIfEnabled(this.permissible);
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> clearParents() {
    return CompletableFuture.supplyAsync(() -> {
      this.permissible.groupNames().clear();
      this.updateIfEnabled(this.permissible);
      return true;
    });
  }
}
