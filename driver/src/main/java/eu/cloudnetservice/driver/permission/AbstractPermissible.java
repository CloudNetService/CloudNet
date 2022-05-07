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

package eu.cloudnetservice.driver.permission;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.document.property.JsonDocPropertyHolder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * An abstract implementation of a permissible for shared usage.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractPermissible extends JsonDocPropertyHolder implements Permissible {

  protected final String name;
  protected final int potency;
  protected final long createdTime;

  protected final Set<Permission> permissions;
  protected final Map<String, Set<Permission>> groupPermissions;

  /**
   * Constructs a new abstract permissible instance.
   *
   * @param name             the name of the permissible.
   * @param potency          the potency of the permissible.
   * @param createdTime      the creation time as unix timestamp.
   * @param permissions      all permissions of the permissible.
   * @param groupPermissions all permissions of the permissible that are per (service) group.
   * @param properties       extra properties for the permissible.
   * @throws NullPointerException if the given name, permissions, groupPermissions or properties is null.
   */
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

  /**
   * Adds the given permission into the set of permissions. If the collection contains a permission with the same name
   * it is removed before adding the new permission.
   *
   * @param permissions the set of permissions to add the given permission to.
   * @param permission  the permission to add to the other permissions.
   * @throws NullPointerException if the given permissions or the permission is null.
   */
  private void addPermission(@NonNull Set<Permission> permissions, @NonNull Permission permission) {
    permissions.removeIf(existingPermission -> existingPermission.name().equalsIgnoreCase(permission.name()));
    permissions.add(permission);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addPermission(@NonNull Permission permission) {
    this.addPermission(this.permissions, permission);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addPermission(@NonNull String group, @NonNull Permission permission) {
    this.addPermission(this.groupPermissions.computeIfAbsent(group, s -> new HashSet<>()), permission);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removePermission(@NonNull String permission) {
    var exist = this.permission(permission);

    if (exist != null) {
      return this.permissions.remove(exist);
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public long creationTime() {
    return this.createdTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int potency() {
    return this.potency;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<Permission> permissions() {
    return this.permissions;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, Set<Permission>> groupPermissions() {
    return this.groupPermissions;
  }
}
