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

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * The default implementation of the IPermissionGroup class. This class should use if you want to add new
 * PermissionGroups into the IPermissionManagement implementation
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public class PermissionGroup extends AbstractPermissible implements IPermissionGroup {

  /**
   * The Gson TypeToken result of the PermissionGroup class
   */
  public static final Type TYPE = new TypeToken<PermissionGroup>() {
  }.getType();

  protected Collection<String> groups = new ArrayList<>();

  private String prefix = "&7";
  private String color = "&7";
  private String suffix = "&f";
  private String display = "&7";

  private int sortId = 0;

  private boolean defaultGroup = false;

  public PermissionGroup() {
  }

  public PermissionGroup(String name, int potency) {
    super();

    this.name = name;
    this.potency = potency;
  }

  public PermissionGroup(String name, int potency, Collection<String> groups, String prefix, String color,
    String suffix, String display, int sortId, boolean defaultGroup) {
    super();

    this.name = name;
    this.potency = potency;
    this.groups = groups;
    this.prefix = prefix;
    this.color = color;
    this.suffix = suffix;
    this.display = display;
    this.sortId = sortId;
    this.defaultGroup = defaultGroup;
  }

  public Collection<String> getGroups() {
    return this.groups;
  }

  public void setGroups(Collection<String> groups) {
    this.groups = groups;
  }

  public String getPrefix() {
    return this.prefix;
  }

  public void setPrefix(@NotNull String prefix) {
    this.prefix = prefix;
  }

  public String getColor() {
    return this.color;
  }

  public void setColor(@NotNull String color) {
    this.color = color;
  }

  public String getSuffix() {
    return this.suffix;
  }

  public void setSuffix(@NotNull String suffix) {
    this.suffix = suffix;
  }

  public String getDisplay() {
    return this.display;
  }

  public void setDisplay(@NotNull String display) {
    this.display = display;
  }

  public int getSortId() {
    return this.sortId;
  }

  public void setSortId(int sortId) {
    this.sortId = sortId;
  }

  public boolean isDefaultGroup() {
    return this.defaultGroup;
  }

  public void setDefaultGroup(boolean defaultGroup) {
    this.defaultGroup = defaultGroup;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    super.write(buffer);

    buffer.writeStringCollection(this.groups);

    buffer.writeString(this.prefix);
    buffer.writeString(this.color);
    buffer.writeString(this.suffix);
    buffer.writeString(this.display);

    buffer.writeInt(this.sortId);
    buffer.writeBoolean(this.defaultGroup);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    super.read(buffer);

    this.groups = buffer.readStringCollection();

    this.prefix = buffer.readString();
    this.color = buffer.readString();
    this.suffix = buffer.readString();
    this.display = buffer.readString();

    this.sortId = buffer.readInt();
    this.defaultGroup = buffer.readBoolean();
  }

  @Override
  public Collection<String> getGroupNames() {
    return this.getGroups();
  }
}
