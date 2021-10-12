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
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.google.common.primitives.Longs;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.permission.IPermissible;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUserGroupInfo;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandPermissions {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  @Parser
  public PermissionUser defaultPermissionUserParser(CommandContext<CommandSource> $, Queue<String> input) {
    String user = input.remove();

    PermissionUser permissionUser;
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
  public PermissionGroup defaultPermissionGroupParser(CommandContext<CommandSource> $, Queue<String> input) {
    String name = input.remove();

    PermissionGroup group = CloudNet.getInstance().getPermissionManagement().getGroup(name);
    if (group == null) {
      throw new ArgumentNotAvailableException("No group found");
    }

    return group;
  }

  @Suggestions("permissionGroup")
  public List<String> suggestPermissionGroup(CommandContext<CommandSource> $, String input) {
    return this.permissionManagement().getGroups().stream().map(INameable::getName).collect(Collectors.toList());
  }

  @Parser(name = "timeUnit")
  public long timeUnitParser(CommandContext<CommandSource> $, Queue<String> input) {
    String time = input.remove();

    if (time.equalsIgnoreCase("lifetime")) {
      return -1;
    }

    Long nonUnitTime = Longs.tryParse(time);
    if (nonUnitTime != null) {
      return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(nonUnitTime);
    }
    int length = time.length();
    if (length < 2) {
      return -1;
    }

    String actualTime = time.substring(0, length - 2);
    Long unitTime = Longs.tryParse(actualTime);
    if (unitTime == null) {
      return -1;
    }

    char unit = time.charAt(length - 1);

    switch (unit) {
      case 'm': {
        return TimeUnit.MINUTES.toMillis(unitTime) + System.currentTimeMillis();
      }
      case 'h': {
        return TimeUnit.HOURS.toMillis(unitTime) + System.currentTimeMillis();
      }
      default:
      case 'd': {
        return TimeUnit.DAYS.toMillis(unitTime) + System.currentTimeMillis();
      }
    }
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
  public void deleteUser(CommandSource source, @Argument("name") PermissionUser permissionUser) {
    this.permissionManagement().deleteUser(permissionUser);
  }

  @CommandMethod("permissions|perms delete group <name>")
  public void deleteGroup(CommandSource source, @Argument("name") PermissionGroup permissionGroup) {
    this.permissionManagement().deleteGroup(permissionGroup);
  }

  @CommandMethod("permissions|perms user <user>")
  public void displayUserInformation(CommandSource source, @Argument("user") PermissionUser permissionUser) {
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
  public void renameUser(CommandSource source, @Argument("user") PermissionUser permissionUser,
    @Argument("name") String newName) {
    this.updateUser(permissionUser, user -> user.setName(newName));
  }

  @CommandMethod("permissions|perms user <user> changePassword <password>")
  public void changeUserPassword(CommandSource source, @Argument("user") PermissionUser permissionUser,
    @Argument("password") String password) {
    this.updateUser(permissionUser, user -> user.changePassword(password));
  }

  @CommandMethod("permissions|perms user <user> check <password>")
  public void checkUserPassword(CommandSource source, @Argument("user") PermissionUser permissionUser,
    @Argument("password") String password) {
    if (permissionUser.checkPassword(password)) {
      source.sendMessage("Valid");
    } else {
      source.sendMessage("invalid");
    }
  }

  @CommandMethod("permissions|perms user <user> add group <name> [duration]")
  public void addGroupToUser(
    CommandSource source,
    @Argument("user") PermissionUser permissionUser,
    @Argument("name") PermissionGroup permissionGroup,
    @Argument(value = "duration", parserName = "timeUnit") Long time
  ) {
    this.updateUser(permissionUser,
      user -> user.addGroup(permissionGroup.getName(), time == null ? 0 : time));
  }

 /* @CommandMethod("permissions|perms user <user> add permission <permission> [targetGroup]")
  public void addUserPermission(CommandSource source,
    @Argument("user") PermissionUser permissionUser,
    @Argument("permission") String permission,
    @Argument("targetGroup") GroupConfiguration targetGroup
  ) {
    this.addPermission(permissionUser, permission, null, null, targetGroup);
  }*/

  @CommandMethod("permissions|perms user <user> add permission <permission> <potency> [targetGroup]")
  public void addUserPermission(CommandSource source,
    @Argument("user") PermissionUser permissionUser,
    @Argument("permission") String permission,
    @Argument("potency") int potency,
    @Argument("targetGroup") GroupConfiguration targetGroup
  ) {
    this.addPermission(permissionUser, permission, potency, null, targetGroup);
  }

  /*@CommandMethod("permissions|perms user <user> add permission <permission> <potency> <duration> [targetGroup]")
  public void addUserPermission(
    CommandSource source,
    @Argument("user") PermissionUser permissionUser,
    @Argument("permission") String rawPermission,
    @Argument("potency") int potency,
    @Argument(value = "duration", parserName = "timeUnit") Long timeOut,
    @Argument("targetGroup") GroupConfiguration targetGroup
  ) {
    this.addPermission(permissionUser, rawPermission, potency, timeOut, targetGroup);
  }

  @CommandMethod("permissions|perms user <user> remove permission <permission> [targetGroup]")
  public void removeUserPermission(
    CommandSource source,
    @Argument("user") PermissionUser permissionUser,
    @Argument("permission") String rawPermission,
    @Argument("targetGroup") GroupConfiguration groupConfiguration
  ) {
    this.removePermission(permissionUser, rawPermission, groupConfiguration);
  }

  @CommandMethod("permissions|perms user <user> remove group <group>")
  public void removeGroupFromUser(
    CommandSource source,
    @Argument("user") PermissionUser permissionUser,
    @Argument("group") PermissionGroup permissionGroup
  ) {
    this.updateUser(permissionUser, user -> user.removeGroup(permissionGroup.getName()));
  }

  @CommandMethod("permissions|perms group")
  public void displayGroupInformation(CommandSource source) {
    for (PermissionGroup group : this.permissionManagement().getGroups()) {
      this.displayGroup(source, group);
    }
  }

  @CommandMethod("permissions|perms group <group>")
  public void displayGroupInformation(CommandSource source, @Argument("group") PermissionGroup permissionGroup) {
    this.displayGroup(source, permissionGroup);
  }

  @CommandMethod("permissions|perms group <group> set sortId <sortId>")
  public void setSortId(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Argument("sortId") int sortId) {
    this.updateGroup(permissionGroup, group -> group.setSortId(sortId));
  }

  @CommandMethod("permissions|perms group <group> set display <display>")
  public void setDisplay(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Greedy @Argument("display") String display) {
    this.updateGroup(permissionGroup, group -> group.setDisplay(display));
  }

  @CommandMethod("permissions|perms group <group> set prefix <prefix>")
  public void setPrefix(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Greedy @Argument("prefix") String prefix) {
    this.updateGroup(permissionGroup, group -> group.setPrefix(prefix));
  }

  @CommandMethod("permissions|perms group <group> set suffix <suffix>")
  public void setSuffix(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Greedy @Argument("suffix") String suffix) {
    this.updateGroup(permissionGroup, group -> group.setSuffix(suffix));
  }

  @CommandMethod("permissions|perms group <group> set color <color>")
  public void setColor(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Greedy @Argument("color") String color) {
    this.updateGroup(permissionGroup, group -> group.setColor(color));
  }

  @CommandMethod("permissions|perms group <group> set defaultGroup <defaultGroup>")
  public void setDefaultGroup(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Argument("defaultGroup") boolean defaultGroup) {
    this.updateGroup(permissionGroup, group -> group.setDefaultGroup(defaultGroup));
  }

  @CommandMethod("permissions|perms group <group> add group <name>")
  public void addInheritGroup(
    CommandSource source,
    @Argument("group") PermissionGroup permissionGroup,
    @Argument("name") PermissionGroup targetGroup
  ) {
    this.updateGroup(permissionGroup, group -> permissionGroup.getGroups().add(targetGroup.getName()));
  }

  @CommandMethod("permissions|perms group <group> add permission <permission> [targetGroup]")
  public void addGroupPermission(CommandSource source,
    @Argument("group") PermissionGroup permissionGroup,
    @Argument("permission") String permission,
    @Argument("targetGroup") GroupConfiguration targetGroup
  ) {
    this.addPermission(permissionGroup, permission, null, null, targetGroup);
  }

  @CommandMethod("permissions|perms group <group> add permission <permission> <potency> [targetGroup]")
  public void addGroupPermission(CommandSource source,
    @Argument("group") PermissionGroup permissionGroup,
    @Argument("permission") String permission,
    @Argument("potency") int potency,
    @Argument("targetGroup") GroupConfiguration targetGroup
  ) {
    this.addPermission(permissionGroup, permission, potency, null, targetGroup);
  }

  @CommandMethod("permissions|perms group <group> add permission <permission> <potency> <duration> [targetGroup]")
  public void addGroupPermission(
    CommandSource source,
    @Argument("group") PermissionGroup permissionGroup,
    @Argument("permission") String rawPermission,
    @Argument("potency") int potency,
    @Argument(value = "duration", parserName = "timeUnit") Long timeOut,
    @Argument("targetGroup") GroupConfiguration targetGroup
  ) {
    this.addPermission(permissionGroup, rawPermission, potency, timeOut, targetGroup);
  }

  @CommandMethod("permissions|perms group <group> remove permission <permission> [targetGroup]")
  public void removeGroupPermission(
    CommandSource source,
    @Argument("group") PermissionGroup permissionGroup,
    @Argument("permission") String rawPermission,
    @Argument("targetGroup") GroupConfiguration groupConfiguration
  ) {
    this.removePermission(permissionGroup, rawPermission, groupConfiguration);
  }

  @CommandMethod("permissions|perms group <group> remove group <name>")
  public void removeInheritGroup(
    CommandSource source,
    @Argument("group") PermissionGroup permissionGroup,
    @Argument("name") PermissionGroup targetGroup
  ) {
    this.updateGroup(permissionGroup, group -> permissionGroup.getGroups().remove(targetGroup.getName()));
  }*/

  private void displayGroup(CommandSource source, PermissionGroup permissionGroup) {
    source.sendMessage(permissionGroup.getName() + " | Potency: " + permissionGroup.getPotency());
    source.sendMessage("Inherits: " + Arrays.toString(permissionGroup.getGroups().toArray()));
    source.sendMessage("Default:" + permissionGroup.isDefaultGroup());
    source.sendMessage("SortId: " + permissionGroup.getSortId());
    source.sendMessage("Prefix: " + permissionGroup.getPrefix());
    source.sendMessage("Color: " + permissionGroup.getColor());
    source.sendMessage("Suffix:" + permissionGroup.getSuffix());
    source.sendMessage("Chat-Display: " + permissionGroup.getDisplay());
    this.displayPermission(source, permissionGroup);
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

  private void addPermission(IPermissible permissible,
    @NotNull String rawPermission,
    @Nullable Integer potency,
    @Nullable Long timeOut,
    @Nullable GroupConfiguration targetGroup
  ) {
    Permission permission = new Permission(rawPermission);
    if (potency != null) {
      permission.setPotency(potency);
    }

    if (timeOut != null) {
      permission.setTimeOutMillis(timeOut);
    }

    if (targetGroup != null) {
      permissible.addPermission(targetGroup.getName(), permission);
    } else {
      permissible.addPermission(permission);
    }

    this.updatePermissible(permissible);
  }

  private void removePermission(IPermissible permissible, String permission, GroupConfiguration targetGroup) {
    if (targetGroup != null) {
      permissible.removePermission(permission, targetGroup.getName());
    } else {
      permissible.removePermission(permission);
    }

    this.updatePermissible(permissible);
  }

  private void updatePermissible(IPermissible permissible) {
    if (permissible instanceof PermissionUser) {
      this.permissionManagement().updateUser((PermissionUser) permissible);
    } else if (permissible instanceof PermissionGroup) {
      this.permissionManagement().updateGroup((PermissionGroup) permissible);
    }
  }

  private void updateGroup(PermissionGroup group, Consumer<PermissionGroup> groupConsumer) {
    groupConsumer.accept(group);
    this.permissionManagement().updateGroup(group);
  }

  private void updateUser(PermissionUser permissionUser, Consumer<PermissionUser> permissionUserConsumer) {
    permissionUserConsumer.accept(permissionUser);
    this.permissionManagement().updateUser(permissionUser);
  }

  private IPermissionManagement permissionManagement() {
    return CloudNet.getInstance().getPermissionManagement();
  }

}
