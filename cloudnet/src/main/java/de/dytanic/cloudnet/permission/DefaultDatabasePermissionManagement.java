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

import com.google.common.base.Preconditions;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.NullCompletableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionManagementHandler;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultDatabasePermissionManagement extends ClusterSynchronizedPermissionManagement implements
  IPermissionManagement {

  private static final Logger LOGGER = LogManager.getLogger(DefaultDatabasePermissionManagement.class);
  private static final String DATABASE_USERS_NAME = "cloudnet_permission_users";
  private static final Type COLLECTION_GROUP_TYPE = TypeToken.getParameterized(Collection.class, PermissionGroup.class)
    .getType();

  private final Path file = Paths.get(System.getProperty("cloudnet.permissions.json.path", "local/permissions.json"));

  private final Callable<AbstractDatabaseProvider> databaseProviderCallable;
  private IPermissionManagementHandler permissionManagementHandler;

  public DefaultDatabasePermissionManagement(Callable<AbstractDatabaseProvider> databaseProviderCallable) {
    this.databaseProviderCallable = databaseProviderCallable;
  }

  @Override
  public void init() {
    FileUtils.createDirectoryReported(this.file.getParent());
    this.loadGroups();
  }

  @Override
  public ITask<IPermissionUser> addUserWithoutClusterSyncAsync(IPermissionUser permissionUser) {
    Preconditions.checkNotNull(permissionUser);

    CompletableTask<IPermissionUser> task = new CompletableTask<>();
    this.getDatabase().insertAsync(permissionUser.getUniqueId().toString(), new JsonDocument(permissionUser))
      .onComplete(success -> task.complete(permissionUser))
      .onCancelled(booleanITask -> task.cancel(true))
      .onFailure(throwable -> task.complete(null));
    this.permissionUserCache.put(permissionUser.getUniqueId(), permissionUser);

    return task;
  }

  @Override
  public ITask<Void> updateUserWithoutClusterSyncAsync(IPermissionUser permissionUser) {
    Preconditions.checkNotNull(permissionUser);

    CompletableTask<Void> task = new NullCompletableTask<>();

    this.getDatabase().updateAsync(permissionUser.getUniqueId().toString(), new JsonDocument(permissionUser))
      .onComplete(success -> task.call())
      .onCancelled(booleanITask -> task.call())
      .onFailure(throwable -> task.call());
    this.permissionUserCache.put(permissionUser.getUniqueId(), permissionUser);

    return task;
  }

  @Override
  public ITask<Boolean> deleteUserWithoutClusterSyncAsync(IPermissionUser permissionUser) {
    Preconditions.checkNotNull(permissionUser);

    this.permissionUserCache.invalidate(permissionUser.getUniqueId());
    return this.getDatabase().deleteAsync(permissionUser.getUniqueId().toString());
  }

  @Override
  public boolean containsUser(@NotNull UUID uniqueId) {
    Preconditions.checkNotNull(uniqueId);

    IPermissionUser user = this.permissionUserCache.getIfPresent(uniqueId);
    return user != null || this.getDatabase().contains(uniqueId.toString());
  }

  @Override
  public boolean containsUser(@NotNull String name) {
    Preconditions.checkNotNull(name);

    for (IPermissionUser permissionUser : this.permissionUserCache.asMap().values()) {
      if (permissionUser.getName().equals(name)) {
        return true;
      }
    }

    for (IPermissionUser permissionUser : this.getUsers(name)) {
      if (permissionUser.getName().equals(name)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public @Nullable IPermissionUser getUser(@NotNull UUID uniqueId) {
    Preconditions.checkNotNull(uniqueId);

    IPermissionUser user = this.permissionUserCache.getIfPresent(uniqueId);
    if (user != null) {
      return user;
    }

    JsonDocument document = this.getDatabase().get(uniqueId.toString());
    if (document == null) {
      return null;
    }

    IPermissionUser permissionUser = document.toInstanceOf(PermissionUser.class);
    if (this.testPermissionUser(permissionUser)) {
      this.updateUser(permissionUser);
    }

    this.permissionUserCache.put(permissionUser.getUniqueId(), permissionUser);
    return permissionUser;
  }

  @Override
  public @NotNull IPermissionUser getOrCreateUser(@NotNull UUID uniqueId, @NotNull String name) {
    Preconditions.checkNotNull(uniqueId);
    Preconditions.checkNotNull(name);

    IPermissionUser permissionUser = this.permissionUserCache.getIfPresent(uniqueId);
    if (permissionUser != null) {
      return permissionUser;
    }

    permissionUser = this.getUser(uniqueId);
    if (permissionUser == null) {
      PermissionUser newUser = new PermissionUser(uniqueId, name, null, 0);
      this.addUser(newUser);
      return newUser;
    } else {
      if (this.testPermissionUser(permissionUser)) {
        this.updateUser(permissionUser);
      }

      this.permissionUserCache.put(uniqueId, permissionUser);
      return permissionUser;
    }
  }

  @Override
  public @NotNull List<IPermissionUser> getUsers(@NotNull String name) {
    Preconditions.checkNotNull(name);

    List<IPermissionUser> permissionUsers = new ArrayList<>();
    for (JsonDocument jsonDocument : this.getDatabase().get("name", name)) {
      IPermissionUser permissionUser = jsonDocument.toInstanceOf(PermissionUser.class);

      if (this.testPermissionUser(permissionUser)) {
        this.updateUser(permissionUser);
      }

      this.permissionUserCache.put(permissionUser.getUniqueId(), permissionUser);
      permissionUsers.add(permissionUser);
    }

    return permissionUsers;
  }

  @Override
  public @Nullable IPermissionUser getFirstUser(String name) {
    for (IPermissionUser permissionUser : this.permissionUserCache.asMap().values()) {
      if (permissionUser.getName().equals(name)) {
        return permissionUser;
      }
    }

    List<IPermissionUser> permissionUsers = this.getUsers(name);

    return permissionUsers.isEmpty() ? null : permissionUsers.get(0);
  }

  @Override
  public @NotNull Collection<IPermissionUser> getUsers() {
    Collection<IPermissionUser> permissionUsers = new ArrayList<>();

    this.getDatabase().iterate((key, document) -> {
      IPermissionUser permissionUser = document.toInstanceOf(PermissionUser.class);
      this.testPermissionUser(permissionUser);

      permissionUsers.add(permissionUser);
    });

    return permissionUsers;
  }

  @Override
  public void setUsers(@NotNull Collection<? extends IPermissionUser> users) {

  }

  @Override
  public ITask<Void> setUsersWithoutClusterSyncAsync(Collection<? extends IPermissionUser> users) {
    Preconditions.checkNotNull(users);

    CompletableTask<Void> task = new NullCompletableTask<>();
    this.getDatabase().clearAsync().onComplete($ -> {
      CountDownLatch latch = new CountDownLatch(users.size());

      for (IPermissionUser permissionUser : users) {
        if (permissionUser != null) {
          this.getDatabase().insertAsync(permissionUser.getUniqueId().toString(), new JsonDocument(permissionUser))
            .onComplete(success -> latch.countDown())
            .onFailure(throwable -> latch.countDown())
            .onCancelled($1 -> latch.countDown());
        }
      }

      try {
        latch.await();
      } catch (InterruptedException exception) {
        LOGGER.severe("Exception while awaiting latch", exception);
      }

      task.call();
    });
    return task;
  }

  @Override
  public @NotNull Collection<IPermissionUser> getUsersByGroup(@NotNull String group) {
    Preconditions.checkNotNull(group);

    Collection<IPermissionUser> permissionUsers = new ArrayList<>();

    this.getDatabase().iterate((key, document) -> {
      IPermissionUser permissionUser = document.toInstanceOf(PermissionUser.class);

      this.testPermissionUser(permissionUser);
      if (permissionUser.inGroup(group)) {
        permissionUsers.add(permissionUser);
      }
    });

    return permissionUsers;
  }

  @Override
  public IPermissionGroup addGroup(
    @NotNull IPermissionGroup permissionGroup) {
    return null;
  }

  @Override
  public void updateGroup(
    @NotNull IPermissionGroup permissionGroup) {

  }

  @Override
  public void deleteGroup(@NotNull String name) {

  }

  @Override
  public void deleteGroup(
    @NotNull IPermissionGroup permissionGroup) {

  }

  @Override
  public @NotNull IPermissionGroup addGroup(@NotNull String role, int potency) {
    return this.addGroup(new PermissionGroup(role, potency));
  }

  @Override
  public IPermissionUser addUser(
    @NotNull IPermissionUser permissionUser) {
    return null;
  }

  @Override
  public void updateUser(
    @NotNull IPermissionUser permissionUser) {

  }

  @Override
  public boolean deleteUser(@NotNull String name) {
    return false;
  }

  @Override
  public boolean deleteUser(
    @NotNull IPermissionUser permissionUser) {
    return false;
  }

  @Override
  public boolean containsGroup(@NotNull String group) {
    Preconditions.checkNotNull(group);

    return this.permissionGroupCache.getIfPresent(group) != null;
  }

  @Override
  public ITask<IPermissionGroup> addGroupWithoutClusterSyncAsync(IPermissionGroup permissionGroup) {
    Preconditions.checkNotNull(permissionGroup);

    this.permissionGroupCache.put(permissionGroup.getName(), permissionGroup);
    this.saveGroups();

    return CompletedTask.create(permissionGroup);
  }

  @Override
  public ITask<Void> updateGroupWithoutClusterSyncAsync(IPermissionGroup permissionGroup) {
    Preconditions.checkNotNull(permissionGroup);

    this.permissionGroupCache.put(permissionGroup.getName(), permissionGroup);
    this.saveGroups();

    return CompletedTask.emptyTask();
  }

  @Override
  public ITask<Void> deleteGroupWithoutClusterSyncAsync(String group) {
    Preconditions.checkNotNull(group);

    this.permissionGroupCache.invalidate(group);
    this.saveGroups();

    return CompletedTask.emptyTask();
  }

  @Override
  public ITask<Void> deleteGroupWithoutClusterSyncAsync(IPermissionGroup group) {
    Preconditions.checkNotNull(group);

    return this.deleteGroupWithoutClusterSyncAsync(group.getName());
  }

  @Override
  public @Nullable IPermissionGroup getGroup(@NotNull String name) {
    Preconditions.checkNotNull(name);

    IPermissionGroup permissionGroup = this.permissionGroupCache.getIfPresent(name);
    if (permissionGroup != null && this.testPermissible(permissionGroup)) {
      this.updateGroup(permissionGroup);
    }

    return permissionGroup;
  }

  @Override
  public @Nullable IPermissionGroup getDefaultPermissionGroup() {
    for (IPermissionGroup group : this.permissionGroupCache.asMap().values()) {
      if (group != null && group.isDefaultGroup()) {
        return group;
      }
    }

    return null;
  }

  @Override
  public Collection<IPermissionGroup> getGroups() {
    Collection<IPermissionGroup> groups = this.permissionGroupCache.asMap().values();
    for (IPermissionGroup permissionGroup : groups) {
      if (this.testPermissible(permissionGroup)) {
        this.updateGroup(permissionGroup);
      }
    }

    return groups;
  }

  @Override
  public @NotNull IPermissionUser addUser(@NotNull String name, @NotNull String password, int potency) {
    return this.addUser(new PermissionUser(UUID.randomUUID(), name, password, potency));
  }

  @Override
  public ITask<Void> setGroupsWithoutClusterSyncAsync(Collection<? extends IPermissionGroup> groups) {
    Preconditions.checkNotNull(groups);

    this.permissionGroupLocks.clear();
    this.permissionGroupCache.invalidateAll();

    for (IPermissionGroup group : groups) {
      this.testPermissible(group);
      this.permissionGroupCache.put(group.getName(), group);
    }

    this.saveGroups();
    return CompletedTask.emptyTask();
  }

  @Override
  public boolean needsDatabaseSync() {
    return !this.getDatabase().isSynced();
  }

  @Override
  public boolean reload() {
    this.loadGroups();

    if (this.permissionManagementHandler != null) {
      this.permissionManagementHandler.handleReloaded(this);
    }

    return true;
  }

  private void saveGroups() {
    List<IPermissionGroup> permissionGroups = new ArrayList<>(this.permissionGroupCache.asMap().values());
    Collections.sort(permissionGroups);
    new JsonDocument("groups", permissionGroups).write(this.file);
  }

  private void loadGroups() {
    JsonDocument document;
    try {
      document = JsonDocument.newDocumentExceptionally(this.file);
    } catch (Exception exception) {
      throw new JsonParseException(
        "Exception while parsing permissions.json. The file is invalid, cannot load groups.");
    }

    if (document.contains("groups")) {
      Collection<PermissionGroup> permissionGroups = document.get("groups", COLLECTION_GROUP_TYPE);

      this.permissionGroupLocks.clear();
      this.permissionGroupCache.invalidateAll();

      for (PermissionGroup group : permissionGroups) {
        this.permissionGroupCache.put(group.getName(), group);
      }

      // saving the groups again to be sure that new fields in the permission group are in the file too
      document.append("groups", this.permissionGroupCache.asMap().values());
      document.write(this.file);
    }
  }

  public Database getDatabase() {
    return this.getDatabaseProvider().getDatabase(DATABASE_USERS_NAME);
  }

  private AbstractDatabaseProvider getDatabaseProvider() {
    try {
      return this.databaseProviderCallable.call();
    } catch (Exception exception) {
      throw new Error("An error occurred while attempting to get the database provider", exception);
    }
  }

  public Map<String, IPermissionGroup> getPermissionGroupsMap() {
    return this.permissionGroupCache.asMap();
  }

  public Callable<AbstractDatabaseProvider> getDatabaseProviderCallable() {
    return this.databaseProviderCallable;
  }

  public IPermissionManagementHandler getPermissionManagementHandler() {
    return this.permissionManagementHandler;
  }

  public void setPermissionManagementHandler(IPermissionManagementHandler permissionManagementHandler) {
    this.permissionManagementHandler = permissionManagementHandler;
  }
}
