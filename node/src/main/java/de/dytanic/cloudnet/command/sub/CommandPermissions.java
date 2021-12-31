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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.google.common.primitives.Longs;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.Nameable;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.driver.permission.Permissible;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandAlias("perms")
@CommandPermission("cloudnet.command.permissions")
@Description("Manages the permissions of users and groups")
public final class CommandPermissions {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  @Parser
  public PermissionUser defaultPermissionUserParser(CommandContext<CommandSource> $, Queue<String> input) {
    var user = input.remove();

    PermissionUser permissionUser;
    try {
      var uniqueId = UUID.fromString(user);
      permissionUser = this.permissionManagement().user(uniqueId);
    } catch (IllegalArgumentException exception) {
      permissionUser = this.permissionManagement().firstUser(user);
    }

    if (permissionUser == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-permissions-user-not-found"));
    }

    return permissionUser;
  }

  @Parser
  public PermissionGroup defaultPermissionGroupParser(CommandContext<CommandSource> $, Queue<String> input) {
    var name = input.remove();

    var group = CloudNet.instance().permissionManagement().group(name);
    if (group == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-permissions-group-not-found"));
    }

    return group;
  }

  @Suggestions("permissionGroup")
  public List<String> suggestPermissionGroup(CommandContext<CommandSource> $, String input) {
    return this.permissionManagement().groups().stream().map(Nameable::name).toList();
  }

  @Parser(name = "timeUnit")
  public long timeUnitParser(CommandContext<CommandSource> $, Queue<String> input) {
    var time = input.remove();
    // lifetime is represented as -1
    if (time.equalsIgnoreCase("lifetime")) {
      return -1;
    }
    // try to parse the raw input since the user may not have specified a unit
    var nonUnitTime = Longs.tryParse(time);
    if (nonUnitTime != null) {
      // no unit found, use days as fallback
      return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(nonUnitTime);
    }
    var length = time.length();
    // check if there even is something to parse e.g. "1h"
    if (length < 2) {
      // unable to parse anything from that, use lifetime
      return -1;
    }
    // remove the last char from the input as it is the unit char
    var actualTime = time.substring(0, length - 2);
    var unitTime = Longs.tryParse(actualTime);
    // check if we successfully parsed the time from the input
    if (unitTime == null) {
      // unable to parse anything from that, use lifetime
      return -1;
    }
    // get the unit the user used
    var unit = time.charAt(length - 1);
    // select the timeunit based on the entered unit of the user
    switch (unit) {
      case 'm' -> {
        return TimeUnit.MINUTES.toMillis(unitTime) + System.currentTimeMillis();
      }
      case 'h' -> {
        return TimeUnit.HOURS.toMillis(unitTime) + System.currentTimeMillis();
      }
      default -> {
        return TimeUnit.DAYS.toMillis(unitTime) + System.currentTimeMillis();
      }
    }
  }

  @CommandMethod("permissions|perms reload")
  public void reloadPermissionSystem(CommandSource source) {
    this.permissionManagement().reload();
    source.sendMessage(I18n.trans("command-permissions-reload-permissions-success"));
  }

  @CommandMethod("permissions|perms create user <name> <password> <potency>")
  public void createUser(
    CommandSource source,
    @Argument("name") String name,
    @Argument("password") String password,
    @Argument("potency") Integer potency
  ) {
    if (this.permissionManagement().firstUser(name) != null) {
      source.sendMessage(I18n.trans("command-permissions-create-user-already-exists"));
      return;
    }
    this.permissionManagement().addUser(name, password, potency);
    source.sendMessage();
  }

  @CommandMethod("permissions|perms create group <name> <potency>")
  public void createGroup(
    CommandSource source,
    @Argument("name") String name,
    @Argument("potency") Integer potency
  ) {
    if (this.permissionManagement().group(name) != null) {
      source.sendMessage(I18n.trans("command-permissions-create-group-already-exists"));
      return;
    }

    this.permissionManagement().addGroup(name, potency);
    source.sendMessage(
      I18n.trans("command-permissions-create-group-successful").replace("%name%", name));
  }

  @CommandMethod("permissions|perms delete user <name>")
  public void deleteUser(CommandSource source, @Argument("name") PermissionUser user) {
    if (this.permissionManagement().deletePermissionUser(user)) {
      source.sendMessage(I18n.trans("command-permissions-delete-user-successful").replace("%name%", user.name()));
    } else {
      source.sendMessage(I18n.trans("command-permissions-user-not-found"));
    }
  }

  @CommandMethod("permissions|perms delete group <name>")
  public void deleteGroup(CommandSource source, @Argument("name") PermissionGroup group) {
    if (this.permissionManagement().deletePermissionGroup(group)) {
      source.sendMessage(I18n.trans("command-permissions-delete-group-successful").replace("%name%", group.name()));
    } else {
      source.sendMessage(I18n.trans("command-permissions-group-not-found"));
    }
  }

  @CommandMethod("permissions|perms user <user>")
  public void displayUserInformation(CommandSource source, @Argument("user") PermissionUser permissionUser) {
    source.sendMessage(permissionUser.uniqueId() + ":" + permissionUser.name());
    source.sendMessage("Potency: " + permissionUser.potency());
    source.sendMessage("Groups:");

    for (var group : permissionUser.groups()) {
      var timeout = "LIFETIME";
      if (group.timeOutMillis() > 0) {
        timeout = DATE_FORMAT.format(group.timeOutMillis());
      }
      source.sendMessage("- " + group.group() + ": " + timeout);
    }

    if (permissionUser.groups().isEmpty()) {
      var defaultGroup = this.permissionManagement().defaultPermissionGroup();
      if (defaultGroup != null) {
        source.sendMessage(defaultGroup.name() + ": LIFETIME");
      }
    }

    this.displayPermission(source, permissionUser);
  }

  @CommandMethod("permissions|perms user <user> rename <name>")
  public void renameUser(CommandSource source, @Argument("user") PermissionUser permissionUser,
    @Argument("name") String newName) {
    this.updateUser(permissionUser, user -> user.name(newName), source);
  }

  @CommandMethod("permissions|perms user <user> changePassword <password>")
  public void changeUserPassword(CommandSource source, @Argument("user") PermissionUser permissionUser,
    @Argument("password") String password) {
    this.updateUser(permissionUser, user -> user.changePassword(password), source);
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
      user -> user.addGroup(permissionGroup.name(), time == null ? 0 : time), source);
  }

  @CommandMethod("permissions|perms user <user> add permission <permission> [potency] [targetGroup] [duration]")
  public void addUserPermission(
    CommandSource source,
    @Argument("user") PermissionUser permissionUser,
    @Argument("permission") String rawPermission,
    @Argument("potency") Integer potency,
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
    this.updateUser(permissionUser, user -> user.removeGroup(permissionGroup.name()), source);
  }

  @CommandMethod("permissions|perms group")
  public void displayGroupInformation(CommandSource source) {
    for (var group : this.permissionManagement().groups()) {
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
    this.updateGroup(permissionGroup, group -> group.sortId(sortId));
  }

  @CommandMethod("permissions|perms group <group> set display <display>")
  public void setDisplay(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Quoted @Argument("display") String display) {
    this.updateGroup(permissionGroup, group -> group.display(display));
  }

  @CommandMethod("permissions|perms group <group> set prefix <prefix>")
  public void setPrefix(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Quoted @Argument("prefix") String prefix) {
    this.updateGroup(permissionGroup, group -> group.prefix(prefix));
  }

  @CommandMethod("permissions|perms group <group> set suffix <suffix>")
  public void setSuffix(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Quoted @Argument("suffix") String suffix) {
    this.updateGroup(permissionGroup, group -> group.suffix(suffix));
  }

  @CommandMethod("permissions|perms group <group> set color <color>")
  public void setColor(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Greedy @Argument("color") String color) {
    this.updateGroup(permissionGroup, group -> group.color(color));
  }

  @CommandMethod("permissions|perms group <group> set defaultGroup <defaultGroup>")
  public void setDefaultGroup(CommandSource source, @Argument("group") PermissionGroup permissionGroup,
    @Argument("defaultGroup") boolean defaultGroup) {
    this.updateGroup(permissionGroup, group -> group.defaultGroup(defaultGroup));
  }

  @CommandMethod("permissions|perms group <group> add group <name>")
  public void addInheritGroup(
    CommandSource source,
    @Argument("group") PermissionGroup permissionGroup,
    @Argument("name") PermissionGroup targetGroup
  ) {
    this.updateGroup(permissionGroup, group -> permissionGroup.groups().add(targetGroup.name()));
  }

  @CommandMethod("permissions|perms group <group> add permission <permission> [potency] [targetGroup] [duration]")
  public void addGroupPermission(
    CommandSource source,
    @Argument("group") PermissionGroup permissionGroup,
    @Argument("permission") String rawPermission,
    @Argument("potency") Integer potency,
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
    this.updateGroup(permissionGroup, group -> permissionGroup.groups().remove(targetGroup.name()));
  }

  private void displayGroup(CommandSource source, PermissionGroup permissionGroup) {
    source.sendMessage("- " + permissionGroup.name() + " | Potency: " + permissionGroup.potency());
    source.sendMessage("- Inherits: " + Arrays.toString(permissionGroup.groups().toArray()));
    source.sendMessage("- Default:" + permissionGroup.defaultGroup());
    source.sendMessage("- SortId: " + permissionGroup.sortId());
    source.sendMessage("- Prefix: " + permissionGroup.prefix());
    source.sendMessage("- Color: " + permissionGroup.color());
    source.sendMessage("- Suffix:" + permissionGroup.suffix());
    source.sendMessage("- Chat-Display: " + permissionGroup.display());
    this.displayPermission(source, permissionGroup);
  }

  private void displayPermission(CommandSource source, Permissible permissible) {
    source.sendMessage("- Permissions:");
    for (var permission : permissible.permissions()) {
      source.sendMessage(this.formatPermission(permission));
    }

    for (var groupPermission : permissible.groupPermissions().entrySet()) {
      source.sendMessage("* " + groupPermission.getKey());

      for (var permission : groupPermission.getValue()) {
        source.sendMessage(this.formatPermission(permission));
      }
    }
  }

  private String formatPermission(Permission permission) {
    var timeout = "LIFETIME";
    if (permission.timeOutMillis() > 0) {
      timeout = DATE_FORMAT.format(permission.timeOutMillis());
    }

    return "- " + permission.name() + " | Potency: " + permission.potency() + " | Timeout: " + timeout;
  }

  private void addPermission(Permissible permissible,
    @NonNull String rawPermission,
    @Nullable Integer potency,
    @Nullable Long timeOut,
    @Nullable GroupConfiguration targetGroup
  ) {
    var permission = Permission.builder().name(rawPermission);
    if (potency != null) {
      permission.potency(potency);
    }

    if (timeOut != null) {
      permission.timeOutMillis(timeOut);
    }

    if (targetGroup != null) {
      permissible.addPermission(targetGroup.name(), permission.build());
    } else {
      permissible.addPermission(permission.build());
    }

    this.updatePermissible(permissible);
  }

  private void removePermission(Permissible permissible, String permission, GroupConfiguration targetGroup) {
    if (targetGroup != null) {
      permissible.removePermission(permission, targetGroup.name());
    } else {
      permissible.removePermission(permission);
    }

    this.updatePermissible(permissible);
  }

  private void updatePermissible(Permissible permissible) {
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

  private void updateUser(
    PermissionUser permissionUser,
    Consumer<PermissionUser> permissionUserConsumer,
    CommandSource source
  ) {
    permissionUserConsumer.accept(permissionUser);
    this.permissionManagement().updateUser(permissionUser);
    source.sendMessage(
      I18n.trans("command-permissions-user-update").replace("%name%", permissionUser.name()));
  }

  private PermissionManagement permissionManagement() {
    return CloudNet.instance().permissionManagement();
  }

}
