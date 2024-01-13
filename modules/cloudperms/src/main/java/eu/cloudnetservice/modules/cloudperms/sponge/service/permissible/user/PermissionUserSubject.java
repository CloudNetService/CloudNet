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
import eu.cloudnetservice.modules.cloudperms.sponge.service.permissible.AbstractSubject;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;

final class PermissionUserSubject extends AbstractSubject<PermissionUser> {

  public PermissionUserSubject(
    String identifier,
    SubjectCollection source,
    PermissionUser data,
    PermissionManagement management
  ) {
    super(identifier, source, data, management);
  }

  @Override
  protected boolean isChild(@NonNull String parent) {
    return false;
  }

  @Override
  protected @NonNull List<? extends SubjectReference> getParents() {
    return Collections.emptyList();
  }

  @Override
  public boolean isSubjectDataPersisted() {
    return true;
  }

  @Override
  public SubjectData subjectData() {
    return new PermissionUserData(true, this.data, this, this.permissionManagement);
  }

  @Override
  public SubjectData transientSubjectData() {
    return new PermissionUserData(false, this.data, this, this.permissionManagement);
  }
}
