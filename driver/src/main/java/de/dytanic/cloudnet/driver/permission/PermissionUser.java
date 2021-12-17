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
import de.dytanic.cloudnet.common.encrypt.EncryptTo;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public class PermissionUser extends AbstractPermissible {

  protected UUID uniqueId;
  protected String hashedPassword;
  protected Collection<PermissionUserGroupInfo> groups;

  public PermissionUser(@NotNull UUID uniqueId, @NotNull String name, @Nullable String password, int potency) {
    this.uniqueId = uniqueId;
    this.name = name;
    this.hashedPassword = password == null
      ? null
      : Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(password));
    this.potency = potency;
    this.groups = new ArrayList<>();
  }

  public PermissionUser(
    @NotNull UUID uniqueId,
    @NotNull String hashedPassword,
    @NotNull Collection<PermissionUserGroupInfo> groups,
    @NotNull String name,
    int potency,
    long createdTime,
    @NotNull List<Permission> permissions,
    @NotNull Map<String, Collection<Permission>> groupPermissions,
    @NotNull JsonDocument properties
  ) {
    super(name, potency, createdTime, permissions, groupPermissions, properties);
    this.uniqueId = uniqueId;
    this.hashedPassword = hashedPassword;
    this.groups = groups;
  }

  public void changePassword(@Nullable String password) {
    this.hashedPassword = password == null
      ? null
      : Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(password));
  }

  public boolean checkPassword(@Nullable String password) {
    return this.hashedPassword != null
      && password != null
      && this.hashedPassword.equals(Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(password)));
  }

  public @NotNull UUID uniqueId() {
    return this.uniqueId;
  }

  public @NotNull Collection<PermissionUserGroupInfo> groups() {
    return this.groups;
  }

  public @NotNull String hashedPassword() {
    return this.hashedPassword;
  }

  @Override
  public @NotNull Collection<String> groupNames() {
    return this.groups().stream().map(PermissionUserGroupInfo::group).collect(Collectors.toList());
  }

  public @NotNull Optional<PermissionUserGroupInfo> findAssignedGroup(@NotNull String group) {
    return this.groups.stream().filter(info -> info.group().equalsIgnoreCase(group)).findFirst();
  }

  public @NotNull PermissionUser addGroup(@NotNull String group) {
    return this.addGroup(group, 0L);
  }

  public @NotNull PermissionUser addGroup(@NotNull String group, long time, @NotNull TimeUnit timeUnit) {
    return this.addGroup(group, System.currentTimeMillis() + timeUnit.toMillis(time));
  }

  public @NotNull PermissionUser addGroup(@NotNull String group, long timeOutMillis) {
    var groupInfo = this.groups().stream()
      .filter(info -> info.group().equalsIgnoreCase(group))
      .findFirst()
      .orElse(null);
    // remove the old group before adding the new one
    if (groupInfo != null) {
      this.removeGroup(groupInfo.group());
    }
    // add the new group info
    groupInfo = new PermissionUserGroupInfo(group, timeOutMillis);
    this.groups().add(groupInfo);
    // for chaining
    return this;
  }

  public @NotNull PermissionUser removeGroup(@NotNull String group) {
    this.groups.removeIf(info -> info.group().equalsIgnoreCase(group));
    return this;
  }

  public boolean inGroup(@NotNull String group) {
    return this.groups().stream().anyMatch(info -> info.group().equalsIgnoreCase(group));
  }
}
