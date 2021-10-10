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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.parsers.Parser;
import com.google.common.primitives.Longs;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.driver.permission.IPermissible;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionUserGroupInfo;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.Nullable;

public class CommandPermissions {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  @Parser
  public IPermissionUser defaultPermissionUserParser(Queue<String> input) {
    String user = input.remove();

    IPermissionUser permissionUser;
    try {
      UUID uniqueId = UUID.fromString(user);
      permissionUser = this.permissionManagement().getUser(uniqueId);
    } catch (IllegalArgumentException exception) {
      permissionUser = this.permissionManagement().getFirstUser(user);
    }

    if (permissionUser == null) {
      throw new ArgumentNotAvailableException("No user found");
    }

    return permissionUser;
  }

  @Parser
  public IPermissionGroup defaultPermissionGroupParser(Queue<String> input) {
    String name = input.remove();

    IPermissionGroup group = CloudNet.getInstance().getPermissionManagement().getGroup(name);
    if (group == null) {
      throw new ArgumentNotAvailableException("No group found");
    }

    return group;
  }

  @CommandMethod("permissions|perms reload")
  public void reloadPermissionSystem(CommandSource source) {
    this.permissionManagement().reload();
  }

  @CommandMethod("permissions|perms create user <name> <password> <potency>")
  public void createUser(
    CommandSource source,
    @Argument("name") String name,
    @Argument("password") String password,
    @Argument("potency") Integer potency
  ) {
    if (this.permissionManagement().getFirstUser(name) != null) {
      source.sendMessage("User already exists");
      return;
    }

    this.permissionManagement().addUser(name, password, potency);
  }

  @CommandMethod("permissions|perms create group <name> <potency>")
  public void createGroup(
    CommandSource source,
    @Argument("name") String name,
    @Argument("potency") Integer potency
  ) {
    if (this.permissionManagement().getGroup(name) != null) {
      source.sendMessage("Group already exists");
      return;
    }

    this.permissionManagement().addGroup(name, potency);
  }

  @CommandMethod("permissions|perms delete user <name>")
  public void deleteUser(CommandSource source, @Argument("name") IPermissionUser permissionUser) {
    this.permissionManagement().deleteUser(permissionUser);
  }

  @CommandMethod("permissions|perms delete group <name>")
  public void deleteGroup(CommandSource source, @Argument("name") IPermissionGroup permissionGroup) {
    this.permissionManagement().deleteGroup(permissionGroup);
  }

  @CommandMethod("permissions|perms user <user>")
  public void displayUserInformation(CommandSource source, @Argument("user") IPermissionUser permissionUser) {
    source.sendMessage(permissionUser.getUniqueId() + ":" + permissionUser.getName());
    source.sendMessage("Potency: " + permissionUser.getPotency());
    source.sendMessage("Groups:");

    for (PermissionUserGroupInfo group : permissionUser.getGroups()) {
      String timeout = "LIFETIME";
      if (group.getTimeOutMillis() > 0) {
        timeout = DATE_FORMAT.format(group.getTimeOutMillis());
      }
      source.sendMessage("- " + group.getGroup() + ": " + timeout);
    }

    if (permissionUser.getGroups().isEmpty()) {
      source.sendMessage(this.permissionManagement().getDefaultPermissionGroup().getName() + ": LIFETIME");
    }

    this.displayPermission(source, permissionUser);
  }

  @CommandMethod("permissions|perms user <user> rename <name>")
  public void renameUser(CommandSource source, @Argument("user") IPermissionUser permissionUser,
    @Argument("name") String newName) {
    permissionUser.setName(newName);
    this.updatePermissible(permissionUser);
  }

  @CommandMethod("permissions|perms user <user> changePassword <password>")
  public void changeUserPassword(CommandSource source, @Argument("user") IPermissionUser permissionUser,
    @Argument("password") String password) {
    permissionUser.changePassword(password);
    this.updatePermissible(permissionUser);
  }

  @CommandMethod("permissions|perms user <user> check <password>")
  public void checkUserPassword(CommandSource source, @Argument("user") IPermissionUser permissionUser,
    @Argument("password") String password) {
    if (permissionUser.checkPassword(password)) {
      source.sendMessage("Valid");
    } else {
      source.sendMessage("invalid");
    }
  }

  @CommandMethod("permissions|perms user <user> add group <name> [time in days | lifetime]")
  public void addGroupToUser(
    CommandSource source,
    @Argument("user") IPermissionUser permissionUser,
    @Argument("name") IPermissionGroup permissionGroup,
    @Argument("time in days | lifetime") String time
  ) {
    permissionUser.addGroup(permissionGroup.getName(), this.parseTimeOutDays(time), TimeUnit.DAYS);
    this.updatePermissible(permissionUser);
  }

  @CommandMethod("permissions|perms user <user> add permission <permission>")
  public void addUserPermission(CommandSource source,
    @Argument("user") IPermissionUser permissionUser,
    @Argument("permission") String permission,
    @Argument("targetGroup") IPermissionGroup targetGroup
  ) {
    this.addPermission(permissionUser, permission, null, null, targetGroup);
  }

  @CommandMethod("permissions|perms user <user> add permission <permission> <potency> [targetGroup]")
  public void addUserPermission(CommandSource source,
    @Argument("user") IPermissionUser permissionUser,
    @Argument("permission") String permission,
    @Argument("potency") int potency,
    @Argument("targetGroup") IPermissionGroup targetGroup
  ) {
    this.addPermission(permissionUser, permission, potency, null, targetGroup);
  }

  @CommandMethod("permissions|perms user <user> add permission <permission> <potency> <time in days | lifetime> [targetGroup]")
  public void addUserPermission(
    CommandSource source,
    @Argument("user") IPermissionUser permissionUser,
    @Argument("permission") String rawPermission,
    @Argument("potency") int potency,
    @Argument("time in days | lifetime") String timeOut,
    @Argument("targetGroup") IPermissionGroup targetGroup
  ) {
    this.addPermission(permissionUser, rawPermission, potency, timeOut, targetGroup);
  }

  @CommandMethod("permissions|perms user <user> remove permission <permission> [targetGroup]")
  public void removeUserPermission(
    CommandSource source,
    @Argument("user") IPermissionUser permissionUser,
    @Argument("permission") String rawPermission,
    @Argument("targetGroup") IPermissionGroup groupConfiguration
  ) {
    this.removePermission(permissionUser, rawPermission, groupConfiguration);
  }

  @CommandMethod("permissions|perms user <user> remove group <group>")
  public void removeGroupFromUser(
    CommandSource source,
    @Argument("user") IPermissionUser permissionUser,
    @Argument("group") IPermissionGroup permissionGroup
  ) {
    permissionUser.removeGroup(permissionGroup.getName());
    this.updatePermissible(permissionUser);
  }

  private void displayPermission(CommandSource source, IPermissible permissible) {
    source.sendMessage("Permissions:");
    for (Permission permission : permissible.getPermissions()) {
      source.sendMessage(this.formatPermission(permission));
    }

    for (Entry<String, Collection<Permission>> groupPermission : permissible.getGroupPermissions().entrySet()) {
      source.sendMessage("* " + groupPermission.getKey());

      for (Permission permission : groupPermission.getValue()) {
        source.sendMessage(this.formatPermission(permission));
      }
    }
  }

  private String formatPermission(Permission permission) {
    String timeout = "LIFETIME";
    if (permission.getTimeOutMillis() > 0) {
      timeout = DATE_FORMAT.format(permission.getTimeOutMillis());
    }

    return "- " + permission.getName() + " | Potency: " + permission.getPotency() + " | Timeout: " + timeout;
  }

  //TODO: write an actual parser to parse minutes, hours and days
  private long parseTimeOutDays(String input) {
    long timeOutDays = -1;
    if (input != null && !input.equalsIgnoreCase("lifetime")) {
      Long parsedTime = Longs.tryParse(input);
      if (parsedTime != null) {
        timeOutDays = parsedTime;
      }

    }

    return timeOutDays;
  }

  private void addPermission(IPermissible permissible,
    String rawPermission,
    @Nullable Integer potency,
    @Nullable String timeOut,
    IPermissionGroup targetGroup
  ) {
    Permission permission = new Permission(rawPermission);
    if (potency != null) {
      permission.setPotency(potency);
    }

    permission.setTimeOutMillis(this.parseTimeOutDays(timeOut));
    if (targetGroup != null) {
      permissible.addPermission(targetGroup.getName(), permission);
    } else {
      permissible.addPermission(permission);
    }

    this.updatePermissible(permissible);
  }

  private void removePermission(IPermissible permissible, String permission, IPermissionGroup targetGroup) {
    if (targetGroup != null) {
      permissible.removePermission(permission, targetGroup.getName());
    } else {
      permissible.removePermission(permission);
    }

    this.updatePermissible(permissible);
  }

  private void updatePermissible(IPermissible permissible) {
    if (permissible instanceof IPermissionUser) {
      this.permissionManagement().updateUser((IPermissionUser) permissible);
    } else if (permissible instanceof IPermissionGroup) {
      this.permissionManagement().updateGroup((IPermissionGroup) permissible);
    }
  }

  private IPermissionManagement permissionManagement() {
    return CloudNet.getInstance().getPermissionManagement();
  }

}
