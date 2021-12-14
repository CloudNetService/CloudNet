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

package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractPermissible extends JsonDocPropertyHolder implements IPermissible {

  protected String name;
  protected int potency;
  protected long createdTime;

  protected List<Permission> permissions;
  protected Map<String, Collection<Permission>> groupPermissions;

  public AbstractPermissible() {
    this.createdTime = System.currentTimeMillis();
    this.permissions = new ArrayList<>();
    this.groupPermissions = new HashMap<>();
  }

  public AbstractPermissible(
    @NotNull String name,
    int potency,
    long createdTime,
    @NotNull List<Permission> permissions,
    @NotNull Map<String, Collection<Permission>> groupPermissions,
    @NotNull JsonDocument properties
  ) {
    this.name = name;
    this.potency = potency;
    this.createdTime = createdTime;
    this.permissions = permissions;
    this.groupPermissions = groupPermissions;
    this.properties = properties;
  }

  private boolean addPermission(@NotNull Collection<Permission> permissions, @Nullable Permission permission) {
    if (permission == null) {
      return false;
    }

    permissions.removeIf(existingPermission -> existingPermission.getName().equalsIgnoreCase(permission.getName()));
    permissions.add(permission);

    return true;
  }

  @Override
  public boolean addPermission(@NotNull Permission permission) {
    return this.addPermission(this.permissions, permission);
  }

  @Override
  public boolean addPermission(@NotNull String group, @NotNull Permission permission) {
    return this.addPermission(this.groupPermissions.computeIfAbsent(group, s -> new ArrayList<>()), permission);
  }

  @Override
  public boolean removePermission(@NotNull String permission) {
    var exist = this.getPermission(permission);

    if (exist != null) {
      return this.permissions.remove(exist);
    } else {
      return false;
    }
  }

  @Override
  public boolean removePermission(@NotNull String group, @NotNull String permission) {
    if (this.groupPermissions.containsKey(group)) {
      var removed = this.groupPermissions.get(group).removeIf(perm -> perm.getName().equalsIgnoreCase(permission));
      if (removed && this.groupPermissions.get(group).isEmpty()) {
        this.groupPermissions.remove(group);
      }

      return removed;
    }

    return false;
  }

  public long getCreatedTime() {
    return this.createdTime;
  }

  @Override
  public @NotNull String name() {
    return this.name;
  }

  @Override
  public void setName(@NotNull String name) {
    this.name = name;
  }

  @Override
  public int getPotency() {
    return this.potency;
  }

  @Override
  public void setPotency(int potency) {
    this.potency = potency;
  }

  @Override
  public @NotNull List<Permission> getPermissions() {
    return this.permissions;
  }

  public void setPermissions(List<Permission> permissions) {
    this.permissions = permissions;
  }

  @Override
  public @NotNull Map<String, Collection<Permission>> getGroupPermissions() {
    return this.groupPermissions;
  }
}
