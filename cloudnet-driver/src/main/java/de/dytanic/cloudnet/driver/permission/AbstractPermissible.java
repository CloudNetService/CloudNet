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

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractPermissible extends SerializableJsonDocPropertyable implements IPermissible {

  protected long createdTime;
  protected String name;
  protected int potency;
  protected List<Permission> permissions;

  protected Map<String, Collection<Permission>> groupPermissions;

  public AbstractPermissible() {
    this.createdTime = System.currentTimeMillis();
    this.permissions = new ArrayList<>();
    this.groupPermissions = new HashMap<>();
  }

  private boolean addPermission(Collection<Permission> permissions, Permission permission) {
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
    Permission exist = this.getPermission(permission);

    if (exist != null) {
      return this.permissions.remove(exist);
    } else {
      return false;
    }
  }

  @Override
  public boolean removePermission(@NotNull String group, @NotNull String permission) {
    if (this.groupPermissions.containsKey(group)) {
      Optional<Permission> optionalPermission = this.groupPermissions.get(group).stream()
        .filter(perm -> perm.getName().equalsIgnoreCase(permission)).findFirst();
      if (optionalPermission.isPresent()) {
        this.groupPermissions.get(group).remove(optionalPermission.get());
        if (this.groupPermissions.get(group).isEmpty()) {
          this.groupPermissions.remove(group);
        }
        return true;
      }
    }

    return false;
  }

  public long getCreatedTime() {
    return this.createdTime;
  }

  public String getName() {
    return this.name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  public int getPotency() {
    return this.potency;
  }

  public void setPotency(int potency) {
    this.potency = potency;
  }

  public List<Permission> getPermissions() {
    return this.permissions;
  }

  public void setPermissions(List<Permission> permissions) {
    this.permissions = permissions;
  }

  public Map<String, Collection<Permission>> getGroupPermissions() {
    return this.groupPermissions;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeLong(this.createdTime);
    buffer.writeString(this.name);
    buffer.writeInt(this.potency);
    buffer.writeObjectCollection(this.permissions);

    buffer.writeVarInt(this.groupPermissions.size());
    this.groupPermissions.forEach((group, permissions) -> {
      buffer.writeString(group);
      buffer.writeObjectCollection(permissions);
    });

    super.write(buffer);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.createdTime = buffer.readLong();
    this.name = buffer.readString();
    this.potency = buffer.readInt();
    this.permissions = new ArrayList<>(buffer.readObjectCollection(Permission.class));

    int size = buffer.readVarInt();
    this.groupPermissions = new HashMap<>(size);
    for (int i = 0; i < size; i++) {
      this.groupPermissions.put(buffer.readString(), buffer.readObjectCollection(Permission.class));
    }

    super.read(buffer);
  }
}
