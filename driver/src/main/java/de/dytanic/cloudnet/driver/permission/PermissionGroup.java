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

package de.dytanic.cloudnet.driver.permission;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

/**
 * This interfaces provides access to the properties of a permission group
 */
public class PermissionGroup extends AbstractPermissible {

  public static final Type COL_GROUPS = TypeToken.getParameterized(Collection.class, PermissionGroup.class).getType();

  protected String color = "&7";
  protected String prefix = "&7";
  protected String suffix = "&f";
  protected String display = "&7";

  protected int sortId = 0;
  protected boolean defaultGroup = false;

  protected Collection<String> groups = new ArrayList<>();

  public PermissionGroup(@NonNull String name, int potency) {
    this.name = name;
    this.potency = potency;
  }

  public PermissionGroup(
    @NonNull String color,
    @NonNull String prefix,
    @NonNull String suffix,
    @NonNull String display,
    int sortId,
    boolean defaultGroup,
    @NonNull Collection<String> groups,
    @NonNull String name,
    int potency,
    long createdTime,
    @NonNull List<Permission> permissions,
    @NonNull Map<String, Collection<Permission>> groupPermissions,
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

  public @NonNull Collection<String> groups() {
    return this.groups;
  }

  public void groups(@NonNull Collection<String> groups) {
    this.groups = groups;
  }

  public @NonNull String prefix() {
    return this.prefix;
  }

  public void prefix(@NonNull String prefix) {
    this.prefix = prefix;
  }

  public @NonNull String color() {
    return this.color;
  }

  public void color(@NonNull String color) {
    this.color = color;
  }

  public @NonNull String suffix() {
    return this.suffix;
  }

  public void suffix(@NonNull String suffix) {
    this.suffix = suffix;
  }

  public @NonNull String display() {
    return this.display;
  }

  public void display(@NonNull String display) {
    this.display = display;
  }

  public int sortId() {
    return this.sortId;
  }

  public void sortId(int sortId) {
    this.sortId = sortId;
  }

  public boolean defaultGroup() {
    return this.defaultGroup;
  }

  public void defaultGroup(boolean defaultGroup) {
    this.defaultGroup = defaultGroup;
  }

  @Override
  public @NonNull Collection<String> groupNames() {
    return this.groups();
  }
}
