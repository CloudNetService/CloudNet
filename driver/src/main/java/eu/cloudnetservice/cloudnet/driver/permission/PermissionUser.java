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

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.hash.HashUtil;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public class PermissionUser extends AbstractPermissible {

  private final UUID uniqueId;
  private final String hashedPassword;
  private final Set<PermissionUserGroupInfo> groups;

  protected PermissionUser(
    @NonNull UUID uniqueId,
    @Nullable String hashedPassword,
    @NonNull Set<PermissionUserGroupInfo> groups,
    @NonNull String name,
    int potency,
    long createdTime,
    @NonNull Set<Permission> permissions,
    @NonNull Map<String, Set<Permission>> groupPermissions,
    @NonNull JsonDocument properties
  ) {
    super(name, potency, createdTime, permissions, groupPermissions, properties);
    this.uniqueId = uniqueId;
    this.hashedPassword = hashedPassword;
    this.groups = groups;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull PermissionUser user) {
    return builder()
      .name(user.name())
      .uniqueId(user.uniqueId())
      .hashedPassword(user.hashedPassword())

      .potency(user.potency())
      .properties(user.properties())

      .permissions(user.permissions())
      .groups(user.groups())
      .groupPermissions(user.groupPermissions());
  }

  public boolean checkPassword(@Nullable String password) {
    return this.hashedPassword != null
      && password != null
      && this.hashedPassword.equals(Base64.getEncoder().encodeToString(HashUtil.toSha256(password)));
  }

  public @NonNull UUID uniqueId() {
    return this.uniqueId;
  }

  public @NonNull Collection<PermissionUserGroupInfo> groups() {
    return this.groups;
  }

  public @Nullable String hashedPassword() {
    return this.hashedPassword;
  }

  @Override
  public @NonNull Collection<String> groupNames() {
    return this.groups().stream().map(PermissionUserGroupInfo::group).collect(Collectors.toList());
  }

  public @NonNull Optional<PermissionUserGroupInfo> findAssignedGroup(@NonNull String group) {
    return this.groups.stream().filter(info -> info.group().equalsIgnoreCase(group)).findFirst();
  }

  public @NonNull PermissionUser addGroup(@NonNull String group) {
    return this.addGroup(group, 0L);
  }

  public @NonNull PermissionUser addGroup(@NonNull String group, long timeOutMillis) {
    return this.addGroup(PermissionUserGroupInfo.builder().group(group).timeOutMillis(timeOutMillis).build());
  }

  public @NonNull PermissionUser addGroup(@NonNull PermissionUserGroupInfo groupInfo) {
    var oldInfo = this.groups().stream()
      .filter(info -> info.group().equalsIgnoreCase(groupInfo.group()))
      .findFirst()
      .orElse(null);
    // remove the old group before adding the new one
    if (oldInfo != null) {
      this.removeGroup(oldInfo.group());
    }
    this.groups().add(groupInfo);
    // for chaining
    return this;
  }

  public @NonNull PermissionUser removeGroup(@NonNull String group) {
    this.groups.removeIf(info -> info.group().equalsIgnoreCase(group));
    return this;
  }

  public boolean inGroup(@NonNull String group) {
    return this.groups().stream().anyMatch(info -> info.group().equalsIgnoreCase(group));
  }

  public static final class Builder {

    private String name;
    private UUID uniqueId;
    private String hashedPassword;

    private int potency;
    private JsonDocument properties = JsonDocument.newDocument();

    private Set<Permission> permissions = new HashSet<>();
    private Set<PermissionUserGroupInfo> groups = new HashSet<>();
    private Map<String, Set<Permission>> groupPermissions = new HashMap<>();

    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    public @NonNull Builder uniqueId(@NonNull UUID uniqueId) {
      this.uniqueId = uniqueId;
      return this;
    }

    public @NonNull Builder password(@NonNull String password) {
      this.hashedPassword = Base64.getEncoder().encodeToString(HashUtil.toSha256(password));
      return this;
    }

    public @NonNull Builder hashedPassword(@Nullable String hashedPassword) {
      this.hashedPassword = hashedPassword;
      return this;
    }

    public @NonNull Builder potency(int potency) {
      this.potency = potency;
      return this;
    }

    public @NonNull Builder permissions(@NonNull Collection<Permission> permissions) {
      this.permissions = new HashSet<>(permissions);
      return this;
    }

    public @NonNull Builder addPermission(@NonNull Permission permission) {
      this.permissions.add(permission);
      return this;
    }

    public @NonNull Builder groups(@NonNull Collection<PermissionUserGroupInfo> groups) {
      this.groups = new HashSet<>(groups);
      return this;
    }

    public @NonNull Builder addGroup(@NonNull PermissionUserGroupInfo group) {
      this.groups.add(group);
      return this;
    }

    public @NonNull Builder groupPermissions(@NonNull Map<String, Set<Permission>> groupPermissions) {
      this.groupPermissions = new HashMap<>(groupPermissions);
      return this;
    }

    public @NonNull Builder properties(@NonNull JsonDocument properties) {
      this.properties = properties.clone();
      return this;
    }

    public @NonNull PermissionUser build() {
      Verify.verifyNotNull(this.name, "Name must be given");
      Verify.verifyNotNull(this.uniqueId, "Unique id must be given");

      return new PermissionUser(
        this.uniqueId,
        this.hashedPassword,
        this.groups,
        this.name,
        this.potency,
        System.currentTimeMillis(),
        this.permissions,
        this.groupPermissions,
        this.properties);
    }
  }
}
