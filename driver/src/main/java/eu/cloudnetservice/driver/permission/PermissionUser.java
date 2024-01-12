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

package eu.cloudnetservice.driver.permission;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.common.hash.HashUtil;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * The permission user extends the permissible in that way that it has its own personal permissions. A permission user
 * can belong to several permission groups and inherits the permissions of those groups.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public class PermissionUser extends AbstractPermissible {

  private final UUID uniqueId;
  private final String hashedPassword;
  private final Set<PermissionUserGroupInfo> groups;

  /**
   * Constructs a new permission group. The group is not saved or updated in any way before
   * {@link PermissionManagement#addPermissionUser(PermissionUser)} is called.
   *
   * @param uniqueId         the unique id of the user.
   * @param hashedPassword   the hashed password of the user.
   * @param groups           all groups the user is in.
   * @param name             the name of the user.
   * @param potency          the potency of the user.
   * @param createdTime      the creation timestamp.
   * @param permissions      all permissions of the user.
   * @param groupPermissions all group specific permissions of the user.
   * @param properties       extra properties for the permission user.
   * @throws NullPointerException if any of the given parameters except the hashed password is null.
   */
  protected PermissionUser(
    @NonNull UUID uniqueId,
    @Nullable String hashedPassword,
    @NonNull Set<PermissionUserGroupInfo> groups,
    @NonNull String name,
    int potency,
    long createdTime,
    @NonNull Set<Permission> permissions,
    @NonNull Map<String, Set<Permission>> groupPermissions,
    @NonNull Document properties
  ) {
    super(name, potency, createdTime, permissions, groupPermissions, properties);
    this.uniqueId = uniqueId;
    this.hashedPassword = hashedPassword;
    this.groups = groups;
  }

  /**
   * Creates a new permission user builder instance with all default values.
   *
   * @return the new builder instance.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new permission user builder instance and copies all values from the given permission user.
   *
   * @param user the group to copy from.
   * @return the new builder instance.
   * @throws NullPointerException if the given user is null.
   */
  public static @NonNull Builder builder(@NonNull PermissionUser user) {
    return builder()
      .name(user.name())
      .uniqueId(user.uniqueId())
      .hashedPassword(user.hashedPassword())

      .potency(user.potency())
      .properties(user.propertyHolder())

      .permissions(user.permissions())
      .groups(user.groups())
      .groupPermissions(user.groupPermissions());
  }

  /**
   * Checks if the given password matches the stored password. The input is hashed using sha 256 and encoded using
   * base64 and then compared to the stored value.
   *
   * @param password the password to compare.
   * @return true if the password matches, false otherwise.
   */
  public boolean checkPassword(@Nullable String password) {
    return this.hashedPassword != null
      && password != null
      && this.hashedPassword.equals(Base64.getEncoder().encodeToString(HashUtil.toSha256(password)));
  }

  /**
   * Gets the unique id of the permission user.
   *
   * @return the unique id of the user.
   */
  public @NonNull UUID uniqueId() {
    return this.uniqueId;
  }

  /**
   * Gets all groups that the user belongs to.
   *
   * @return all groups of the user.
   */
  public @NonNull Collection<PermissionUserGroupInfo> groups() {
    return this.groups;
  }

  /**
   * Gets the password of the user hashed with sha 256. If no password is set null is returned.
   *
   * @return the hashed password.
   */
  public @Nullable String hashedPassword() {
    return this.hashedPassword;
  }

  /**
   * Gets the name of all groups the user is in.
   *
   * @return the name of all groups of the user.
   */
  @Override
  public @NonNull Collection<String> groupNames() {
    return this.groups().stream().map(PermissionUserGroupInfo::group).collect(Collectors.toList());
  }

  /**
   * Searches for any of the users groups with the given name.
   *
   * @param group the group to search for.
   * @return the information about the assigned group, null if not assigned to this user.
   * @throws NullPointerException if the given group is null.
   */
  public @Nullable PermissionUserGroupInfo findAssignedGroup(@NonNull String group) {
    return this.groups.stream().filter(info -> info.group().equalsIgnoreCase(group)).findFirst().orElse(null);
  }

  /**
   * Adds the given group without any time-out to this permission user.
   * <p>
   * Note: No update is done until the user is manually updated using
   * {@link PermissionManagement#updateUser(PermissionUser)}.
   *
   * @param group the group to add to the user.
   * @return the same user instance for chaining.
   * @throws NullPointerException if the given group is null.
   */
  public @NonNull PermissionUser addGroup(@NonNull String group) {
    return this.addGroup(group, 0L);
  }

  /**
   * Adds the given group with the given time-out to this permission user. The given time-out should be a unix timestamp
   * in the future or any negative int if the group should not expire.
   * <p>
   * Note: No update is done until the user is manually updated using
   * {@link PermissionManagement#updateUser(PermissionUser)}.
   *
   * @param group         the group to add to the user.
   * @param timeOutMillis the timestamp for the group expiry.
   * @return the same usr instance for chaining.
   * @throws NullPointerException if the given group is null.
   */
  public @NonNull PermissionUser addGroup(@NonNull String group, long timeOutMillis) {
    return this.addGroup(PermissionUserGroupInfo.builder().group(group).timeOutMillis(timeOutMillis).build());
  }

  /**
   * Adds the given group user info to this permission user. Other groups with the same name are replaced by the given
   * group info.
   * <p>
   * Note: No update is done until the user is manually updated using
   * {@link PermissionManagement#updateUser(PermissionUser)}.
   *
   * @param groupInfo the group info of the new group for the user.
   * @return the same user instance for chaining.
   * @throws NullPointerException if the given group is null.
   */
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

  /**
   * Removes every group that has the given name.
   * <p>
   * Note: No update is done until the user is manually updated using
   * {@link PermissionManagement#updateUser(PermissionUser)}.
   *
   * @param group the group to remove.
   * @return true if any group was removed, false otherwise.
   * @throws NullPointerException if the given group is null.
   */
  public boolean removeGroup(@NonNull String group) {
    return this.groups.removeIf(info -> info.group().equalsIgnoreCase(group));
  }

  /**
   * Checks if the user is in a permission group that has the given name.
   *
   * @param group the group to check if the user is in.
   * @return true if the user is in any group with the given name, false otherwise.
   * @throws NullPointerException if the given group is null.
   */
  public boolean inGroup(@NonNull String group) {
    return this.groups().stream().anyMatch(info -> info.group().equalsIgnoreCase(group));
  }

  /**
   * A builder for permission users.
   *
   * @since 4.0
   */
  public static final class Builder implements DefaultedDocPropertyHolder.Mutable.WithDirectModifier<Builder> {

    private String name;
    private UUID uniqueId;
    private String hashedPassword;

    private int potency;
    private Document.Mutable properties = Document.newJsonDocument();

    private Set<Permission> permissions = new HashSet<>();
    private Set<PermissionUserGroupInfo> groups = new HashSet<>();
    private Map<String, Set<Permission>> groupPermissions = new HashMap<>();

    /**
     * Sets the name of the new permission user.
     *
     * @param name the name for the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given name is null.
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the unique id of the new permission user.
     *
     * @param uniqueId the unique id for the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given unique id is null.
     */
    public @NonNull Builder uniqueId(@NonNull UUID uniqueId) {
      this.uniqueId = uniqueId;
      return this;
    }

    /**
     * Sets the raw password of the new permission user. The given input is hashed using SHA-256 and encoded using
     * Base64.
     *
     * @param password the password for the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given password is null.
     */
    public @NonNull Builder password(@NonNull String password) {
      this.hashedPassword = Base64.getEncoder().encodeToString(HashUtil.toSha256(password));
      return this;
    }

    /**
     * Sets the already hashed password for the new permission user. The password should be hashed using SHA-256 and
     * encoded using Base64.
     *
     * @param hashedPassword the hashed password for the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given hashed password is null.
     */
    public @NonNull Builder hashedPassword(@Nullable String hashedPassword) {
      this.hashedPassword = hashedPassword;
      return this;
    }

    /**
     * Sets the potency of the new permission user.
     *
     * @param potency the potency for the new user.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder potency(int potency) {
      this.potency = potency;
      return this;
    }

    /**
     * Sets the permissions of the new permission user.
     *
     * @param permissions the permissions for the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given permission collection is null.
     */
    public @NonNull Builder permissions(@NonNull Collection<Permission> permissions) {
      this.permissions = new HashSet<>(permissions);
      return this;
    }

    /**
     * Adds the given permission to the permissions of the new user.
     *
     * @param permission the permission to add to the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given permission is null.
     */
    public @NonNull Builder addPermission(@NonNull Permission permission) {
      this.permissions.add(permission);
      return this;
    }

    /**
     * Sets the groups of the new permission user.
     *
     * @param groups the groups for the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given group collection is null.
     */
    public @NonNull Builder groups(@NonNull Collection<PermissionUserGroupInfo> groups) {
      this.groups = new HashSet<>(groups);
      return this;
    }

    /**
     * Adds the given group to the groups of the new user.
     *
     * @param group the group to add to the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given group is null.
     */
    public @NonNull Builder addGroup(@NonNull PermissionUserGroupInfo group) {
      this.groups.add(group);
      return this;
    }

    /**
     * Sets the group permissions of the new permission user.
     *
     * @param groupPermissions the group specific permissions for the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given group permissions are null.
     */
    public @NonNull Builder groupPermissions(@NonNull Map<String, Set<Permission>> groupPermissions) {
      this.groupPermissions = new HashMap<>(groupPermissions);
      return this;
    }

    /**
     * Sets the properties of the new permission user.
     *
     * @param properties the properties for the new user.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given properties are null.
     */
    public @NonNull Builder properties(@NonNull Document properties) {
      this.properties = properties.mutableCopy();
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Document.Mutable propertyHolder() {
      return this.properties;
    }

    /**
     * Constructs the new permission user from this builder.
     *
     * @return the new permission user.
     * @throws NullPointerException if the name or unique id is missing.
     */
    public @NonNull PermissionUser build() {
      Preconditions.checkNotNull(this.name, "Name must be given");
      Preconditions.checkNotNull(this.uniqueId, "Unique id must be given");

      return new PermissionUser(
        this.uniqueId,
        this.hashedPassword,
        this.groups,
        this.name,
        this.potency,
        System.currentTimeMillis(),
        this.permissions,
        this.groupPermissions,
        this.properties.immutableCopy());
    }
  }
}
