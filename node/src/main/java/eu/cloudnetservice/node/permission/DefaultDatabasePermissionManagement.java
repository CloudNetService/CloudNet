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

package eu.cloudnetservice.node.permission;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.permission.DefaultPermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.network.listener.message.PermissionChannelMessageListener;
import eu.cloudnetservice.node.permission.command.PermissionUserCommandSource;
import eu.cloudnetservice.node.permission.handler.PermissionManagementHandler;
import eu.cloudnetservice.node.permission.handler.PermissionManagementHandlerAdapter;
import eu.cloudnetservice.node.setup.PermissionGroupSetup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class DefaultDatabasePermissionManagement extends DefaultPermissionManagement
  implements NodePermissionManagement {

  private static final String USER_DB_NAME = "cloudnet_permission_users";
  private static final Path GROUPS_FILE = Path.of(
    System.getProperty("cloudnet.permissions.json.path", "local/permissions.json"));

  protected final Node nodeInstance;
  protected final Map<String, PermissionGroup> groups;
  protected final PermissionChannelMessageListener networkListener;

  protected volatile PermissionManagementHandler handler = PermissionManagementHandlerAdapter.NO_OP;

  public DefaultDatabasePermissionManagement(@NonNull Node nodeInstance) {
    this.nodeInstance = nodeInstance;
    this.groups = new ConcurrentHashMap<>();
    this.networkListener = new PermissionChannelMessageListener(nodeInstance.eventManager(), this);
    // sync permission groups into the cluster
    Node.instance().dataSyncRegistry().registerHandler(DataSyncHandler.<PermissionGroup>builder()
      .alwaysForce()
      .key("perms-groups")
      .nameExtractor(PermissionGroup::name)
      .convertObject(PermissionGroup.class)
      .dataCollector(() -> Node.instance().permissionManagement().groups())
      .writer(group -> Node.instance().permissionManagement().addGroupSilently(group))
      .currentGetter(group -> Node.instance().permissionManagement().group(group.name()))
      .build());
  }

  @Override
  public @Nullable PermissionUser firstUser(@NonNull String name) {
    return Iterables.getFirst(this.usersByName(name), null);
  }

  @Override
  public void init() {
    if (Files.notExists(GROUPS_FILE)) {
      this.nodeInstance.installation().registerSetup(new PermissionGroupSetup());
      this.saveGroups(); // write an empty file to the groups file location
    } else {
      this.loadGroups();
    }

    this.nodeInstance.eventManager().registerListener(this.networkListener);
    this.nodeInstance.rpcFactory().newHandler(PermissionManagement.class, this).registerToDefaultRegistry();
  }

  @Override
  public void close() {
    this.nodeInstance.eventManager().unregisterListener(this.networkListener);
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
  public PermissionGroup defaultPermissionGroup() {
    return this.groups.values().stream().filter(PermissionGroup::defaultGroup).findFirst().orElse(null);
  }

  @Override
  public @NonNull PermissionUser addPermissionUser(@NonNull String name, @NonNull String password, int potency) {
    return this.addPermissionUser(PermissionUser.builder()
      .name(name)
      .uniqueId(UUID.randomUUID())
      .password(password)
      .potency(potency)
      .build());
  }

  @Override
  public @NonNull PermissionGroup addPermissionGroup(@NonNull String name, int potency) {
    return this.addPermissionGroup(PermissionGroup.builder().name(name).potency(potency).build());
  }

  @Override
  public @NonNull PermissionUser addPermissionUser(@NonNull PermissionUser user) {
    // insert the user into the database
    this.userDatabaseTable().insert(user.uniqueId().toString(), JsonDocument.newDocument(user));
    // notify the listener
    this.handler.handleAddUser(this, user);
    return user;
  }

  @Override
  public void updateUser(@NonNull PermissionUser user) {
    // update in the database
    this.userDatabaseTable().insert(user.uniqueId().toString(), JsonDocument.newDocument(user));
    // notify the listener
    this.handler.handleUpdateUser(this, user);
  }

  @Override
  public boolean deleteUser(@NonNull String name) {
    // get all users with the name
    var users = this.usersByName(name);
    // delete all the users if there are any
    if (!users.isEmpty()) {
      var success = false;
      for (var user : users) {
        success |= this.deletePermissionUser(user);
      }
      // all users deleted
      return success;
    }
    // no users
    return false;
  }

  @Override
  public boolean deletePermissionUser(@NonNull PermissionUser permissionUser) {
    if (this.userDatabaseTable().delete(permissionUser.uniqueId().toString())) {
      // notify the listener
      this.handler.handleDeleteUser(this, permissionUser);
      return true;
    }
    return false;
  }

  @Override
  public boolean containsUser(@NonNull UUID uniqueId) {
    return this.userDatabaseTable().contains(uniqueId.toString());
  }

  @Override
  public boolean containsOneUser(@NonNull String name) {
    return this.firstUser(name) != null;
  }

  @Override
  public @Nullable PermissionUser user(@NonNull UUID uniqueId) {
    // try to find the user in the database
    var user = this.userDatabaseTable().get(uniqueId.toString());
    // check if the user is in the database
    if (user == null) {
      return null;
    } else {
      // Deserialize the user from the document
      var permissionUser = user.toInstanceOf(PermissionUser.class);
      // update the user info if necessary
      if (this.testPermissible(permissionUser)) {
        this.updateUserAsync(permissionUser);
      }
      // return the user
      return permissionUser;
    }
  }

  @Override
  public @NonNull PermissionUser getOrCreateUser(@NonNull UUID uniqueId, @NonNull String name) {
    // try to get the permission user
    var user = this.user(uniqueId);
    // create a new user if the current one is not present
    if (user == null) {
      user = PermissionUser.builder().uniqueId(uniqueId).name(name).build();
      this.addPermissionUserAsync(user);
    }
    // return the created or old user
    return user;
  }

  @Override
  public @NonNull List<PermissionUser> usersByName(@NonNull String name) {
    return this.userDatabaseTable().find("name", name).stream()
      .map(userData -> {
        // deserialize the permission user
        var user = userData.toInstanceOf(PermissionUser.class);
        // check if we need to update the user
        if (this.testPermissible(user)) {
          this.updateUserAsync(user);
        }
        // use the user instance
        return user;
      }).collect(Collectors.toList());
  }

  @Override
  public @NonNull Collection<PermissionUser> users() {
    Collection<PermissionUser> users = new ArrayList<>();
    // select all users from the database
    this.userDatabaseTable().iterate(($, data) -> {
      // deserialize the permission user
      var user = data.toInstanceOf(PermissionUser.class);
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
  public @NonNull Collection<PermissionUser> usersByGroup(@NonNull String group) {
    Collection<PermissionUser> users = new ArrayList<>();
    // select all users from the database
    this.userDatabaseTable().iterate(($, data) -> {
      // deserialize the permission user
      var user = data.toInstanceOf(PermissionUser.class);
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
  public @NonNull PermissionGroup addPermissionGroup(@NonNull PermissionGroup permissionGroup) {
    this.addGroupSilently(permissionGroup);
    // notify the listener
    this.handler.handleAddGroup(this, permissionGroup);
    return permissionGroup;
  }

  @Override
  public void updateGroup(@NonNull PermissionGroup permissionGroup) {
    this.updateGroupSilently(permissionGroup);
    // notify the listener
    this.handler.handleUpdateGroup(this, permissionGroup);
  }

  @Override
  public boolean deleteGroup(@NonNull String name) {
    var group = this.groups.get(name);
    if (group != null) {
      return this.deletePermissionGroup(group);
    }
    return false;
  }

  @Override
  public boolean deletePermissionGroup(@NonNull PermissionGroup permissionGroup) {
    this.deleteGroupSilently(permissionGroup);
    this.handler.handleDeleteGroup(this, permissionGroup);
    return true;
  }

  @Override
  public boolean containsGroup(@NonNull String group) {
    return this.groups.containsKey(group);
  }

  @Override
  public @Nullable PermissionGroup group(@NonNull String name) {
    return this.groups.get(name);
  }

  @Override
  public @NonNull Collection<PermissionGroup> groups() {
    return Collections.unmodifiableCollection(this.groups.values());
  }

  @Override
  public void groups(@Nullable Collection<PermissionGroup> groups) {
    // handle the setGroups
    this.setGroupsSilently(groups);
    // publish to the listeners
    if (groups != null) {
      this.handler.handleSetGroups(this, groups);
    }
  }

  @Override
  public @NonNull Collection<String> sendCommandLine(@NonNull PermissionUser user, @NonNull String commandLine) {
    var source = new PermissionUserCommandSource(user, this);
    Node.instance().commandProvider().execute(source, commandLine).getOrNull();
    return source.messages();
  }

  @Override
  public void addGroupSilently(@NonNull PermissionGroup permissionGroup) {
    this.groups.put(permissionGroup.name(), permissionGroup);
    // save the groups
    this.saveGroups();
  }

  @Override
  public void updateGroupSilently(@NonNull PermissionGroup permissionGroup) {
    this.groups.put(permissionGroup.name(), permissionGroup);
    // save the groups
    this.saveGroups();
  }

  @Override
  public void deleteGroupSilently(@NonNull PermissionGroup permissionGroup) {
    this.groups.remove(permissionGroup.name());
    // save the groups
    this.saveGroups();
  }

  @Override
  public void setGroupsSilently(@Nullable Collection<PermissionGroup> groups) {
    this.groups.clear();
    // set the provided groups
    if (groups != null) {
      for (PermissionGroup group : groups) {
        this.groups.put(group.name(), group);
      }
      // save the groups
      this.saveGroups();
    }
  }

  @Override
  public @NonNull PermissionManagementHandler permissionManagementHandler() {
    return this.handler;
  }

  @Override
  public void permissionManagementHandler(@NonNull PermissionManagementHandler handler) {
    this.handler = handler;
  }

  protected @NonNull LocalDatabase userDatabaseTable() {
    return this.nodeInstance.databaseProvider().database(USER_DB_NAME);
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
      for (var group : groups) {
        this.groups.put(group.name(), group);
      }
      // save the file again to update the fields in the permission group
      this.saveGroups();
    }
  }
}
