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

package de.dytanic.cloudnet.permission;

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.database.LocalDatabase;
import de.dytanic.cloudnet.driver.permission.DefaultPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.network.listener.message.PermissionChannelMessageListener;
import de.dytanic.cloudnet.permission.handler.IPermissionManagementHandler;
import de.dytanic.cloudnet.permission.handler.PermissionManagementHandlerAdapter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultDatabasePermissionManagement extends DefaultPermissionManagement
  implements NodePermissionManagement {

  private static final String USER_DB_NAME = "cloudnet_permission_users";
  private static final Path GROUPS_FILE = Paths.get(
    System.getProperty("cloudnet.permissions.json.path", "local/permissions.json"));

  protected final CloudNet nodeInstance;
  protected final Map<String, PermissionGroup> groups;
  protected final PermissionChannelMessageListener networkListener;

  protected volatile IPermissionManagementHandler handler = PermissionManagementHandlerAdapter.NO_OP;

  public DefaultDatabasePermissionManagement(@NotNull CloudNet nodeInstance) {
    this.nodeInstance = nodeInstance;
    this.groups = new ConcurrentHashMap<>();
    this.networkListener = new PermissionChannelMessageListener(nodeInstance.getEventManager(), this);
  }

  @Override
  public PermissionUser getFirstUser(String name) {
    return Iterables.getFirst(this.getUsersByName(name), null);
  }

  @Override
  public void init() {
    FileUtils.createDirectoryReported(GROUPS_FILE.getParent());
    this.loadGroups();

    this.nodeInstance.getEventManager().registerListener(this.networkListener);
    this.nodeInstance.getRPCProviderFactory().newHandler(IPermissionManagement.class, this).registerToDefaultRegistry();
  }

  @Override
  public void close() {
    this.nodeInstance.getEventManager().unregisterListener(this.networkListener);
  }

  @Override
  public boolean reload() {
    // clear the cache & update
    this.groups.clear();
    this.loadGroups();
    // push to the handler
    this.handler.handleReloaded(this);
    // success
    return true;
  }

  @Override
  public PermissionGroup getDefaultPermissionGroup() {
    return this.groups.values().stream().filter(PermissionGroup::isDefaultGroup).findFirst().orElse(null);
  }

  @Override
  public @NotNull PermissionUser addUser(@NotNull String name, @NotNull String password, int potency) {
    return this.addPermissionUser(new PermissionUser(UUID.randomUUID(), name, password, potency));
  }

  @Override
  public @NotNull PermissionGroup addGroup(@NotNull String role, int potency) {
    return this.addPermissionGroup(new PermissionGroup(role, potency));
  }

  @Override
  public @NotNull PermissionUser addPermissionUser(@NotNull PermissionUser user) {
    // insert the user into the database
    this.getUserDatabaseTable().insert(user.getUniqueId().toString(), JsonDocument.newDocument(user));
    // notify the listener
    this.handler.handleAddUser(this, user);
    return user;
  }

  @Override
  public void updateUser(@NotNull PermissionUser user) {
    // update in the database
    this.getUserDatabaseTable().update(user.getUniqueId().toString(), JsonDocument.newDocument(user));
    // notify the listener
    this.handler.handleUpdateUser(this, user);
  }

  @Override
  public boolean deleteUser(@NotNull String name) {
    // get all users with the name
    Collection<PermissionUser> users = this.getUsersByName(name);
    // delete all the users if there are any
    if (!users.isEmpty()) {
      boolean success = false;
      for (PermissionUser user : users) {
        success |= this.deletePermissionUser(user);
      }
      // all users deleted
      return success;
    }
    // no users
    return false;
  }

  @Override
  public boolean deletePermissionUser(@NotNull PermissionUser permissionUser) {
    if (this.getUserDatabaseTable().delete(permissionUser.getUniqueId().toString())) {
      // notify the listener
      this.handler.handleDeleteUser(this, permissionUser);
      return true;
    }
    return false;
  }

  @Override
  public boolean containsUser(@NotNull UUID uniqueId) {
    return this.getUserDatabaseTable().contains(uniqueId.toString());
  }

  @Override
  public boolean containsOneUser(@NotNull String name) {
    return this.getFirstUser(name) != null;
  }

  @Override
  public @Nullable PermissionUser getUser(@NotNull UUID uniqueId) {
    // try to find the user in the database
    JsonDocument user = this.getUserDatabaseTable().get(uniqueId.toString());
    // check if the user is in the database
    if (user == null) {
      return null;
    } else {
      // Deserialize the user from the document
      PermissionUser permissionUser = user.toInstanceOf(PermissionUser.class);
      // update the user info if necessary
      if (this.testPermissible(permissionUser)) {
        this.updateUserAsync(permissionUser);
      }
      // return the user
      return permissionUser;
    }
  }

  @Override
  public @NotNull PermissionUser getOrCreateUser(@NotNull UUID uniqueId, @NotNull String name) {
    // try to get the permission user
    PermissionUser user = this.getUser(uniqueId);
    // create a new user if the current one is not present
    if (user == null) {
      user = new PermissionUser(uniqueId, name, null, 0);
      this.addPermissionUserAsync(user);
    }
    // return the created or old user
    return user;
  }

  @Override
  public @NotNull List<PermissionUser> getUsersByName(@NotNull String name) {
    return this.getUserDatabaseTable().get("name", name).stream()
      .map(userData -> {
        // deserialize the permission user
        PermissionUser user = userData.toInstanceOf(PermissionUser.class);
        // check if we need to update the user
        if (this.testPermissible(user)) {
          this.updateUserAsync(user);
        }
        // use the user instance
        return user;
      }).collect(Collectors.toList());
  }

  @Override
  public @NotNull Collection<PermissionUser> getUsers() {
    Collection<PermissionUser> users = new ArrayList<>();
    // select all users from the database
    this.getUserDatabaseTable().iterate(($, data) -> {
      // deserialize the permission user
      PermissionUser user = data.toInstanceOf(PermissionUser.class);
      // check if we need to update the user
      if (this.testPermissible(user)) {
        this.updateUserAsync(user);
      }
      // use the user instance
      users.add(user);
    });
    // the collected users
    return users;
  }

  @Override
  public @NotNull Collection<PermissionUser> getUsersByGroup(@NotNull String group) {
    Collection<PermissionUser> users = new ArrayList<>();
    // select all users from the database
    this.getUserDatabaseTable().iterate(($, data) -> {
      // deserialize the permission user
      PermissionUser user = data.toInstanceOf(PermissionUser.class);
      // check if we need to update the user
      if (this.testPermissible(user)) {
        this.updateUserAsync(user);
      }
      // use the user instance if in the group
      if (user.inGroup(group)) {
        users.add(user);
      }
    });
    // the collected users
    return users;
  }

  @Override
  public @NotNull PermissionGroup addPermissionGroup(@NotNull PermissionGroup permissionGroup) {
    this.addGroupSilently(permissionGroup);
    // notify the listener
    this.handler.handleAddGroup(this, permissionGroup);
    return permissionGroup;
  }

  @Override
  public void updateGroup(@NotNull PermissionGroup permissionGroup) {
    this.updateGroupSilently(permissionGroup);
    // notify the listener
    this.handler.handleUpdateGroup(this, permissionGroup);
  }

  @Override
  public boolean deleteGroup(@NotNull String name) {
    PermissionGroup group = this.groups.get(name);
    if (group != null) {
      return this.deletePermissionGroup(group);
    }
    return false;
  }

  @Override
  public boolean deletePermissionGroup(@NotNull PermissionGroup permissionGroup) {
    this.deleteGroupSilently(permissionGroup);
    this.handler.handleDeleteGroup(this, permissionGroup);
    return true;
  }

  @Override
  public boolean containsGroup(@NotNull String group) {
    return this.groups.containsKey(group);
  }

  @Override
  public @Nullable PermissionGroup getGroup(@NotNull String name) {
    return this.groups.get(name);
  }

  @Override
  public @NotNull Collection<PermissionGroup> getGroups() {
    return Collections.unmodifiableCollection(this.groups.values());
  }

  @Override
  public void setGroups(@Nullable Collection<? extends PermissionGroup> groups) {
    // handle the setGroups
    this.setGroupsSilently(groups);
    // publish to the listeners
    if (groups != null) {
      this.handler.handleSetGroups(this, groups);
    }
  }

  @Override
  public void addGroupSilently(@NotNull PermissionGroup permissionGroup) {
    this.groups.put(permissionGroup.getName(), permissionGroup);
    // save the groups
    this.saveGroups();
  }

  @Override
  public void updateGroupSilently(@NotNull PermissionGroup permissionGroup) {
    this.groups.put(permissionGroup.getName(), permissionGroup);
    // save the groups
    this.saveGroups();
  }

  @Override
  public void deleteGroupSilently(@NotNull PermissionGroup permissionGroup) {
    this.groups.remove(permissionGroup.getName());
    // save the groups
    this.saveGroups();
  }

  @Override
  public void setGroupsSilently(@Nullable Collection<? extends PermissionGroup> groups) {
    this.groups.clear();
    // set the provided groups
    if (groups != null) {
      for (PermissionGroup group : groups) {
        this.groups.put(group.getName(), group);
      }
      // save the groups
      this.saveGroups();
    }
  }

  @Override
  public @NotNull IPermissionManagementHandler getPermissionManagementHandler() {
    return this.handler;
  }

  @Override
  public void setPermissionManagementHandler(@NotNull IPermissionManagementHandler handler) {
    this.handler = handler;
  }

  protected @NotNull LocalDatabase getUserDatabaseTable() {
    return this.nodeInstance.getDatabaseProvider().getDatabase(USER_DB_NAME);
  }

  protected void saveGroups() {
    // sort the groups
    List<PermissionGroup> groups = new ArrayList<>(this.groups.values());
    Collections.sort(groups);
    // write to the file
    JsonDocument.newDocument("groups", groups).write(GROUPS_FILE);
  }

  protected void loadGroups() {
    Collection<PermissionGroup> groups = JsonDocument.newDocument(GROUPS_FILE).get(
      "groups",
      PermissionGroup.COL_GROUPS);
    if (groups != null) {
      // add all groups
      for (PermissionGroup group : groups) {
        this.groups.put(group.getName(), group);
      }
      // save the file again to update the fields in the permission group
      this.saveGroups();
    }
  }
}
