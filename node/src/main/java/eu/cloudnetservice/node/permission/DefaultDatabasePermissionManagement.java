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

package eu.cloudnetservice.node.permission;

import com.google.common.collect.Iterables;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.driver.permission.DefaultPermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import eu.cloudnetservice.node.network.listener.message.PermissionChannelMessageListener;
import eu.cloudnetservice.node.permission.command.PermissionUserCommandSource;
import eu.cloudnetservice.node.permission.handler.PermissionManagementHandler;
import eu.cloudnetservice.node.permission.handler.PermissionManagementHandlerAdapter;
import eu.cloudnetservice.node.setup.DefaultInstallation;
import eu.cloudnetservice.node.setup.PermissionGroupSetup;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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

@Singleton
@Provides({NodePermissionManagement.class, PermissionManagement.class})
public class DefaultDatabasePermissionManagement
  extends DefaultPermissionManagement
  implements NodePermissionManagement {

  private static final String USER_DB_NAME = "cloudnet_permission_users";
  private static final Path GROUPS_FILE = Path.of(System.getProperty(
    "cloudnet.permissions.json.path",
    "local/permissions.json"));

  protected final RPCFactory rpcFactory;
  protected final EventManager eventManager;
  protected final CommandProvider commandProvider;
  protected final DefaultInstallation installation;
  protected final RPCHandlerRegistry handlerRegistry;
  protected final NodeDatabaseProvider databaseProvider;

  protected final Map<String, PermissionGroup> groups;
  protected final PermissionChannelMessageListener networkListener;

  protected volatile PermissionManagementHandler handler = PermissionManagementHandlerAdapter.NO_OP;

  @Inject
  public DefaultDatabasePermissionManagement(
    @NonNull RPCFactory rpcFactory,
    @NonNull EventManager eventManager,
    @NonNull CommandProvider commandProvider,
    @NonNull DefaultInstallation installation,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull RPCHandlerRegistry handlerRegistry,
    @NonNull NodeDatabaseProvider databaseProvider,
    @NonNull PermissionChannelMessageListener networkListener
  ) {
    this.rpcFactory = rpcFactory;
    this.eventManager = eventManager;
    this.installation = installation;
    this.networkListener = networkListener;
    this.commandProvider = commandProvider;
    this.handlerRegistry = handlerRegistry;
    this.databaseProvider = databaseProvider;
    this.groups = new ConcurrentHashMap<>();

    // sync permission groups into the cluster
    dataSyncRegistry.registerHandler(DataSyncHandler.<PermissionGroup>builder()
      .alwaysForce()
      .key("perms-groups")
      .nameExtractor(PermissionGroup::name)
      .convertObject(PermissionGroup.class)
      .dataCollector(this::groups)
      .writer(this::addGroupSilently)
      .currentGetter(group -> this.group(group.name()))
      .build());
  }

  @Override
  public @Nullable PermissionUser firstUser(@NonNull String name) {
    return Iterables.getFirst(this.usersByName(name), null);
  }

  @Override
  public void init() {
    if (Files.notExists(GROUPS_FILE)) {
      this.installation.registerSetup(PermissionGroupSetup.class);
      this.saveGroups(); // write an empty file to the groups file location
    } else {
      this.loadGroups();
    }

    this.eventManager.registerListener(this.networkListener);
    this.rpcFactory.newHandler(PermissionManagement.class, this).registerTo(this.handlerRegistry);
  }

  @Override
  public void close() {
    this.eventManager.unregisterListener(this.networkListener);
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
    this.userDatabaseTable().insert(user.uniqueId().toString(), Document.newJsonDocument().appendTree(user));
    // notify the listener
    this.handler.handleAddUser(this, user);
    return user;
  }

  @Override
  public void updateUser(@NonNull PermissionUser user) {
    // update in the database
    this.userDatabaseTable().insert(user.uniqueId().toString(), Document.newJsonDocument().appendTree(user));
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
      if (this.testPermissionUser(permissionUser)) {
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
        if (this.testPermissionUser(user)) {
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
      if (this.testPermissionUser(user)) {
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
      if (this.testPermissionUser(user)) {
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
    var group = this.groups.get(name);
    if (group == null) {
      return null;
    }

    // test and remove any outdated permissions - update afterwards
    if (this.testPermissible(group)) {
      this.updateGroupAsync(group);
    }

    return group;
  }

  @Override
  public @NonNull Collection<PermissionGroup> groups() {
    return this.groups.values().stream().peek(group -> {
      // test and remove any outdated permissions - update afterwards
      if (this.testPermissible(group)) {
        this.updateGroupAsync(group);
      }
    }).collect(Collectors.toSet());
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
    this.commandProvider.execute(source, commandLine).getOrNull();
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
      for (var group : groups) {
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
    return this.databaseProvider.database(USER_DB_NAME);
  }

  protected void saveGroups() {
    // sort the groups
    List<PermissionGroup> groups = new ArrayList<>(this.groups.values());
    Collections.sort(groups);
    // write to the file
    Document.newJsonDocument().append("groups", groups).writeTo(GROUPS_FILE);
  }

  protected void loadGroups() {
    Collection<PermissionGroup> groups = DocumentFactory.json().parse(GROUPS_FILE).readObject(
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
