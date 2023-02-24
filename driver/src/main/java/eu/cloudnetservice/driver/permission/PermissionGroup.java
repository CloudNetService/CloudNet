/*
 * Copyright 2019-2023 CloudNetService team & contributors
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
import eu.cloudnetservice.common.document.gson.JsonDocument;
import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

/**
 * A permission group is an implementation of the permissible that extends the permissible in the sense that it adds
 * cosmetic options that can be used by other systems. The permission group allows permission inheritance by specifying
 * the parents using {@link #groupNames()}.
 *
 * @see Permissible
 * @see PermissionManagement
 * @since 4.0
 */
public class PermissionGroup extends AbstractPermissible {

  public static final Type COL_GROUPS = TypeFactory.parameterizedClass(Collection.class, PermissionGroup.class);

  private final String color;
  private final String prefix;
  private final String suffix;
  private final String display;

  private final int sortId;
  private final boolean defaultGroup;

  private final Set<String> groups;

  /**
   * Constructs a new permission group. The group is not saved or updated in any way before
   * {@link PermissionManagement#addPermissionGroup(PermissionGroup)} is called.
   *
   * @param color            the color of the group.
   * @param prefix           the prefix of the group.
   * @param suffix           the suffix of the group.
   * @param display          the chat display of the group.
   * @param sortId           the sort id of the group.
   * @param defaultGroup     whether the group is the default group or not.
   * @param groups           the parent groups used for permission inheritance.
   * @param name             the name of the group.
   * @param potency          the potency of the group.
   * @param createdTime      the timestamp at the creation.
   * @param permissions      all permissions the group has.
   * @param groupPermissions all group specific permissions the group has.
   * @param properties       extra properties for the group.
   * @throws NullPointerException if one of the given parameters is null.
   */
  protected PermissionGroup(
    @NonNull String color,
    @NonNull String prefix,
    @NonNull String suffix,
    @NonNull String display,
    int sortId,
    boolean defaultGroup,
    @NonNull Set<String> groups,
    @NonNull String name,
    int potency,
    long createdTime,
    @NonNull Set<Permission> permissions,
    @NonNull Map<String, Set<Permission>> groupPermissions,
    @NonNull JsonDocument properties
  ) {
    super(name, potency, createdTime, permissions, groupPermissions, properties);
    this.color = color;
    this.prefix = prefix;
    this.suffix = suffix;
    this.display = display;
    this.sortId = sortId;
    this.defaultGroup = defaultGroup;
    this.groups = groups;
  }

  /**
   * Creates a new permission group builder instance with all default values.
   *
   * @return the new builder instance.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new permission group builder instance and copies all values from the given permission group.
   *
   * @param group the group to copy from.
   * @return the new builder instance.
   * @throws NullPointerException if the given group is null.
   */
  public static @NonNull Builder builder(@NonNull PermissionGroup group) {
    return builder()
      .name(group.name())
      .potency(group.potency())

      .color(group.color())
      .prefix(group.prefix())
      .suffix(group.suffix())
      .display(group.display())

      .sortId(group.sortId())
      .defaultGroup(group.defaultGroup())

      .groups(group.groupNames())
      .permissions(group.permissions())

      .properties(group.propertyHolder())
      .groupPermissions(group.groupPermissions());
  }

  /**
   * Gets the prefix of the group.
   *
   * @return the prefix of the group.
   */
  public @NonNull String prefix() {
    return this.prefix;
  }

  /**
   * Gets the color of the group.
   *
   * @return the color of the group.
   */
  public @NonNull String color() {
    return this.color;
  }

  /**
   * Gets the suffix of the group.
   *
   * @return the suffix of the group.
   */
  public @NonNull String suffix() {
    return this.suffix;
  }

  /**
   * Gets the display of the group.
   *
   * @return the display of the group.
   */
  public @NonNull String display() {
    return this.display;
  }

  /**
   * Gets the prefix of the group.
   *
   * @return the prefix of the group.
   */
  public int sortId() {
    return this.sortId;
  }

  /**
   * Gets whether this group is the default group or not.
   * <p>
   * Note: There might be multiple default groups, but always the first one is chosen as default group without being
   * deterministic.
   *
   * @return whether this group is the default group or not.
   */
  public boolean defaultGroup() {
    return this.defaultGroup;
  }

  /**
   * Gets all parent groups of this group. This group inherits all permissions of the parent groups.
   *
   * @return all parent groups.
   */
  @Override
  public @NonNull Collection<String> groupNames() {
    return this.groups;
  }

  /**
   * A builder for permission groups.
   *
   * @since 4.0
   */
  public static final class Builder {

    private String name;
    private int potency;

    private String color = "&7";
    private String prefix = "&7";
    private String suffix = "&f";
    private String display = "&7";

    private int sortId = 0;
    private boolean defaultGroup = false;

    private Set<String> groups = new HashSet<>();
    private Set<Permission> permissions = new HashSet<>();

    private JsonDocument properties = JsonDocument.newDocument();
    private Map<String, Set<Permission>> groupPermissions = new HashMap<>();

    /**
     * Sets the required name of the permission group.
     *
     * @param name the name of the new group.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given name is null.
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the potency of the permission group.
     *
     * @param potency the potency of the new group.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder potency(int potency) {
      this.potency = potency;
      return this;
    }

    /**
     * Sets the color of the permission group.
     *
     * @param color the color of the new group.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given color is null.
     */
    public @NonNull Builder color(@NonNull String color) {
      this.color = color;
      return this;
    }

    /**
     * Sets the prefix of the permission group.
     *
     * @param prefix the prefix of the new group.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given prefix is null.
     */
    public @NonNull Builder prefix(@NonNull String prefix) {
      this.prefix = prefix;
      return this;
    }

    /**
     * Sets the suffix of the permission group.
     *
     * @param suffix the suffix of the new group.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given suffix is null.
     */
    public @NonNull Builder suffix(@NonNull String suffix) {
      this.suffix = suffix;
      return this;
    }

    /**
     * Sets the display of the permission group.
     *
     * @param display the display of the new group.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given display is null.
     */
    public @NonNull Builder display(@NonNull String display) {
      this.display = display;
      return this;
    }

    /**
     * Sets the sort id of the permission group.
     *
     * @param sortId the sort id of the new group.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder sortId(int sortId) {
      this.sortId = sortId;
      return this;
    }

    /**
     * Sets whether the permission group is the default group or not.
     *
     * @param defaultGroup whether the permission group is the default group or not.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder defaultGroup(boolean defaultGroup) {
      this.defaultGroup = defaultGroup;
      return this;
    }

    /**
     * Adds the given group to the parent groups of the permission group.
     *
     * @param group the parent group to add.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given group is null.
     */
    public @NonNull Builder addGroup(@NonNull PermissionGroup group) {
      this.groups.add(group.name());
      return this;
    }

    /**
     * Sets the parent groups of the permission group.
     *
     * @param groups the parent groups of the new group.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given group collection is null.
     */
    public @NonNull Builder groups(@NonNull Collection<String> groups) {
      this.groups = new HashSet<>(groups);
      return this;
    }

    /**
     * Adds the given permission to the permissions of the group.
     *
     * @param permission the permission to add.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given permission is null.
     */
    public @NonNull Builder addPermission(@NonNull Permission permission) {
      this.permissions.add(permission);
      return this;
    }

    /**
     * Sets the permissions of the permission group.
     *
     * @param permissions the permissions of the new group.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given permissions are null.
     */
    public @NonNull Builder permissions(@NonNull Collection<Permission> permissions) {
      this.permissions = new HashSet<>(permissions);
      return this;
    }

    /**
     * Sets the group permissions of the permission group.
     *
     * @param groupPermissions the group permissions for the new group.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given group permissions are null.
     */
    public @NonNull Builder groupPermissions(@NonNull Map<String, Set<Permission>> groupPermissions) {
      this.groupPermissions = new HashMap<>(groupPermissions);
      return this;
    }

    /**
     * Sets the properties of the permission group.
     *
     * @param properties the properties of the new group.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given properties are null.
     */
    public @NonNull Builder properties(@NonNull JsonDocument properties) {
      this.properties = properties.clone();
      return this;
    }

    /**
     * Constructs the new permission group from this builder.
     *
     * @return the new permission group.
     * @throws NullPointerException if the name is missing.
     */
    public @NonNull PermissionGroup build() {
      Preconditions.checkNotNull(this.name, "No name given");
      return new PermissionGroup(
        this.color,
        this.prefix,
        this.suffix,
        this.display,
        this.sortId,
        this.defaultGroup,
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
