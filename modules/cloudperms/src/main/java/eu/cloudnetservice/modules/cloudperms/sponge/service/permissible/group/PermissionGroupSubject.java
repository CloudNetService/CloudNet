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

import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.modules.cloudperms.sponge.service.permissible.AbstractSubject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;

final class PermissionGroupSubject extends AbstractSubject<PermissionGroup> {

  public PermissionGroupSubject(
    String identifier,
    SubjectCollection source,
    PermissionGroup data,
    PermissionManagement management
  ) {
    super(identifier, source, data, management);
  }

  @Override
  protected boolean isChild(@NonNull String parent) {
    var other = this.permissionManagement.group(parent);
    return other != null && other.groupNames().stream().anyMatch(parent::equals);
  }

  @Override
  protected @NonNull List<? extends SubjectReference> getParents() {
    return this.data.groupNames().stream()
      .map(this.permissionManagement::group)
      .filter(Objects::nonNull)
      .map(group -> this.source.newSubjectReference(group.name()))
      .collect(Collectors.toList());
  }

  @Override
  public boolean isSubjectDataPersisted() {
    return true;
  }

  @Override
  public SubjectData subjectData() {
    return new PermissionGroupData(true, this.data, this, this.permissionManagement);
  }

  @Override
  public SubjectData transientSubjectData() {
    return new PermissionGroupData(false, this.data, this, this.permissionManagement);
  }
}
