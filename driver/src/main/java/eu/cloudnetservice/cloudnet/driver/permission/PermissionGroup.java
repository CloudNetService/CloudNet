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
import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

/**
 * This interfaces provides access to the properties of a permission group
 */
public class PermissionGroup extends AbstractPermissible {

  public static final Type COL_GROUPS = TypeToken.getParameterized(Collection.class, PermissionGroup.class).getType();

  private final String color;
  private final String prefix;
  private final String suffix;
  private final String display;

  private final int sortId;
  private final boolean defaultGroup;

  private final Set<String> groups;

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

  public static @NonNull Builder builder() {
    return new Builder();
  }

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

      .properties(group.properties())
      .groupPermissions(group.groupPermissions());
  }

  public @NonNull String prefix() {
    return this.prefix;
  }

  public @NonNull String color() {
    return this.color;
  }

  public @NonNull String suffix() {
    return this.suffix;
  }

  public @NonNull String display() {
    return this.display;
  }

  public int sortId() {
    return this.sortId;
  }

  public boolean defaultGroup() {
    return this.defaultGroup;
  }

  @Override
  public @NonNull Collection<String> groupNames() {
    return this.groups;
  }

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

    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    public @NonNull Builder potency(int potency) {
      this.potency = potency;
      return this;
    }

    public @NonNull Builder color(@NonNull String color) {
      this.color = color;
      return this;
    }

    public @NonNull Builder prefix(@NonNull String prefix) {
      this.prefix = prefix;
      return this;
    }

    public @NonNull Builder suffix(@NonNull String suffix) {
      this.suffix = suffix;
      return this;
    }

    public @NonNull Builder display(@NonNull String display) {
      this.display = display;
      return this;
    }

    public @NonNull Builder sortId(int sortId) {
      this.sortId = sortId;
      return this;
    }

    public @NonNull Builder defaultGroup(boolean defaultGroup) {
      this.defaultGroup = defaultGroup;
      return this;
    }

    public @NonNull Builder groups(@NonNull Collection<String> groups) {
      this.groups = new HashSet<>(groups);
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

    public @NonNull Builder groupPermissions(@NonNull Map<String, Set<Permission>> groupPermissions) {
      this.groupPermissions = new HashMap<>(groupPermissions);
      return this;
    }

    public @NonNull Builder addGroup(@NonNull PermissionGroup group) {
      this.groups.add(group.name());
      return this;
    }

    public @NonNull Builder properties(@NonNull JsonDocument properties) {
      this.properties = properties.clone();
      return this;
    }

    public @NonNull PermissionGroup build() {
      Verify.verifyNotNull(this.name, "No name given");
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
