/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.permission;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractPermissible extends JsonDocPropertyHolder implements Permissible {

  protected String name;
  protected int potency;
  protected long createdTime;

  protected Set<Permission> permissions;
  protected Map<String, Set<Permission>> groupPermissions;

  public AbstractPermissible() {
    super(JsonDocument.newDocument());
    this.createdTime = System.currentTimeMillis();
    this.permissions = new HashSet<>();
    this.groupPermissions = new HashMap<>();
  }

  public AbstractPermissible(
    @NonNull String name,
    int potency,
    long createdTime,
    @NonNull Set<Permission> permissions,
    @NonNull Map<String, Set<Permission>> groupPermissions,
    @NonNull JsonDocument properties
  ) {
    super(properties);
    this.name = name;
    this.potency = potency;
    this.createdTime = createdTime;
    this.permissions = permissions;
    this.groupPermissions = groupPermissions;
  }

  private boolean addPermission(@NonNull Collection<Permission> permissions, @Nullable Permission permission) {
    if (permission == null) {
      return false;
    }

    permissions.removeIf(existingPermission -> existingPermission.name().equalsIgnoreCase(permission.name()));
    permissions.add(permission);

    return true;
  }

  @Override
  public boolean addPermission(@NonNull Permission permission) {
    return this.addPermission(this.permissions, permission);
  }

  @Override
  public boolean addPermission(@NonNull String group, @NonNull Permission permission) {
    return this.addPermission(this.groupPermissions.computeIfAbsent(group, s -> new HashSet<>()), permission);
  }

  @Override
  public boolean removePermission(@NonNull String permission) {
    var exist = this.permission(permission);

    if (exist != null) {
      return this.permissions.remove(exist);
    } else {
      return false;
    }
  }

  @Override
  public boolean removePermission(@NonNull String group, @NonNull String permission) {
    if (this.groupPermissions.containsKey(group)) {
      var removed = this.groupPermissions.get(group).removeIf(perm -> perm.name().equalsIgnoreCase(permission));
      if (removed && this.groupPermissions.get(group).isEmpty()) {
        this.groupPermissions.remove(group);
      }

      return removed;
    }

    return false;
  }

  public long createdTime() {
    return this.createdTime;
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  @Override
  public int potency() {
    return this.potency;
  }

  @Override
  public @NonNull Collection<Permission> permissions() {
    return this.permissions;
  }

  @Override
  public @NonNull Map<String, Set<Permission>> groupPermissions() {
    return this.groupPermissions;
  }
}
