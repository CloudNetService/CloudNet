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

package eu.cloudnetservice.modules.cloudperms.sponge.service.permissible.user;

import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.modules.cloudperms.sponge.service.permissible.PermissibleSubjectData;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.service.permission.TransferMethod;

final class PermissionUserData extends PermissibleSubjectData<PermissionUser> {

  public PermissionUserData(
    boolean allowModify,
    PermissionUser permissible,
    Subject subject,
    PermissionManagement management
  ) {
    super(allowModify, permissible, subject, management);
  }

  @Override
  protected void update(@NonNull PermissionUser data) {
    this.management.updateUser(data);
  }

  @Override
  public CompletableFuture<Boolean> setParents(
    Set<Context> contexts,
    List<? extends SubjectReference> parents,
    TransferMethod method
  ) {
    return CompletableFuture.completedFuture(false); // no parent system for users
  }

  @Override
  public CompletableFuture<Boolean> addParent(Set<Context> contexts, SubjectReference parent) {
    return CompletableFuture.completedFuture(false); // no parent system for users
  }

  @Override
  public CompletableFuture<Boolean> removeParent(Set<Context> contexts, SubjectReference parent) {
    return CompletableFuture.completedFuture(false); // no parent system for users
  }

  @Override
  public CompletableFuture<Boolean> clearParents() {
    return CompletableFuture.completedFuture(false); // no parent system for users
  }
}
