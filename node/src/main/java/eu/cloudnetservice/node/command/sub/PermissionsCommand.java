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

package eu.cloudnetservice.node.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.permission.Permissible;
import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.driver.permission.PermissionUserGroupInfo;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@CommandAlias("perms")
@CommandPermission("cloudnet.command.permissions")
@Description("command-permissions-description")
public final class PermissionsCommand {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  private final PermissionManagement permissionManagement;

  @Inject
  public PermissionsCommand(@NonNull PermissionManagement permissionManagement) {
    this.permissionManagement = permissionManagement;
  }

  @Parser
  public @NonNull PermissionUser defaultPermissionUserParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var user = input.remove();

    PermissionUser permissionUser;
    try {
      var uniqueId = UUID.fromString(user);
      permissionUser = this.permissionManagement.user(uniqueId);
    } catch (IllegalArgumentException exception) {
      permissionUser = this.permissionManagement.firstUser(user);
    }

    if (permissionUser == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-permissions-user-not-found"));
    }

    return permissionUser;
  }

  @Parser(suggestions = "permissionGroup")
  public @NonNull PermissionGroup defaultPermissionGroupParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var name = input.remove();

    var group = this.permissionManagement.group(name);
    if (group == null) {
      throw new ArgumentNotAvailableException(I18n.trans("command-permissions-group-not-found"));
    }

    return group;
  }

  @Suggestions("permissionGroup")
  public @NonNull List<String> suggestPermissionGroup(@NonNull CommandContext<?> $, @NonNull String input) {
    return this.permissionManagement.groups().stream().map(Named::name).toList();
  }

  @CommandMethod("permissions|perms reload")
  public void reloadPermissionSystem(@NonNull CommandSource source) {
    this.permissionManagement.reload();
    source.sendMessage(I18n.trans("command-permissions-reload-permissions-success"));
  }

  @CommandMethod("permissions|perms create user <name> <password> <potency>")
  public void createUser(
    @NonNull CommandSource source,
    @NonNull @Argument("name") String name,
    @NonNull @Argument("password") String password,
    @Argument("potency") int potency
  ) {
    if (this.permissionManagement.firstUser(name) != null) {
      source.sendMessage(I18n.trans("command-permissions-create-user-already-exists"));
    } else {
      this.permissionManagement.addPermissionUser(name, password, potency);
      source.sendMessage(I18n.trans("command-permissions-create-user-successful", name));
    }
  }

  @CommandMethod("permissions|perms create group <name> <potency>")
  public void createGroup(
    @NonNull CommandSource source,
    @NonNull @Argument("name") String name,
    @Argument("potency") int potency
  ) {
    if (this.permissionManagement.group(name) != null) {
      source.sendMessage(I18n.trans("command-permissions-create-group-already-exists"));
    } else {
      this.permissionManagement.addPermissionGroup(name, potency);
      source.sendMessage(I18n.trans("command-permissions-create-group-successful", name));
    }
  }

  @CommandMethod("permissions|perms delete user <name>")
  public void deleteUser(@NonNull CommandSource source, @NonNull @Argument("name") PermissionUser user) {
    if (this.permissionManagement.deletePermissionUser(user)) {
      source.sendMessage(I18n.trans("command-permissions-delete-user-successful", user.name()));
    } else {
      source.sendMessage(I18n.trans("command-permissions-user-not-found"));
    }
  }

  @CommandMethod("permissions|perms delete group <name>")
  public void deleteGroup(@NonNull CommandSource source, @NonNull @Argument("name") PermissionGroup group) {
    if (this.permissionManagement.deletePermissionGroup(group)) {
      source.sendMessage(I18n.trans("command-permissions-delete-group-successful", group.name()));
    } else {
      source.sendMessage(I18n.trans("command-permissions-group-not-found"));
    }
  }

  @CommandMethod("permissions|perms user <user>")
  public void displayUserInformation(@NonNull CommandSource source, @NonNull @Argument("user") PermissionUser user) {
    source.sendMessage(user.uniqueId() + ":" + user.name());
    source.sendMessage("Potency: " + user.potency());
    source.sendMessage("Groups:");

    for (var group : user.groups()) {
      var timeout = "LIFETIME";
      if (group.timeOutMillis() > 0) {
        var timeoutTime = Instant.ofEpochMilli(group.timeOutMillis()).atZone(ZoneId.systemDefault());
        timeout = DATE_TIME_FORMATTER.format(timeoutTime);
      }
      source.sendMessage("- " + group.group() + ": " + timeout);
    }

    if (user.groups().isEmpty()) {
      var defaultGroup = this.permissionManagement.defaultPermissionGroup();
      if (defaultGroup != null) {
        source.sendMessage(defaultGroup.name() + ": LIFETIME");
      }
    }

    this.displayPermission(source, user);
  }

  @CommandMethod("permissions|perms user <user> rename <name>")
  public void renameUser(
    @NonNull CommandSource source,
    @NonNull @Argument("user") PermissionUser permissionUser,
    @NonNull @Argument("name") String newName
  ) {
    this.updateUser(permissionUser, user -> user.name(newName));
    source.sendMessage(I18n.trans("command-permissions-user-rename-success", permissionUser.name(), newName));
  }

  @CommandMethod("permissions|perms user <user> changePassword <password>")
  public void changeUserPassword(
    @NonNull CommandSource source,
    @NonNull @Argument("user") PermissionUser permissionUser,
    @NonNull @Argument("password") String password
  ) {
    this.updateUser(permissionUser, user -> user.password(password));
    source.sendMessage(I18n.trans("command-permissions-user-change-password-success", permissionUser.name()));
  }

  @CommandMethod("permissions|perms user <user> check <password>")
  public void checkUserPassword(
    @NonNull CommandSource source,
    @NonNull @Argument("user") PermissionUser permissionUser,
    @NonNull @Argument("password") String password
  ) {
    source.sendMessage(I18n.trans("command-permissions-user-check-password", permissionUser.name(),
      permissionUser.checkPassword(password) ? 1 : 0));
  }

  @CommandMethod("permissions|perms user <user> add group <name> [duration]")
  public void addGroupToUser(
    @NonNull CommandSource source,
    @NonNull @Argument("user") PermissionUser permissionUser,
    @NonNull @Argument("name") PermissionGroup permissionGroup,
    @Nullable @Argument("duration") Duration time
  ) {
    this.updateUser(permissionUser, user -> user.addGroup(PermissionUserGroupInfo.builder()
      .group(permissionGroup.name())
      .timeOutMillis(time == null ? 0 : System.currentTimeMillis() + time.toMillis())
      .build()));
    source.sendMessage(I18n.trans("command-permissions-user-add-group-successful",
      permissionGroup.name(),
      permissionUser.name()));
  }

  @CommandMethod("permissions|perms user <user> add permission <permission>")
  public void addUserPermission(
    @NonNull CommandSource source,
    @NonNull @Argument("user") PermissionUser permissionUser,
    @NonNull @Argument("permission") String rawPermission,
    @Nullable @Flag("potency") Integer potency,
    @Nullable @Flag("duration") Duration timeOut,
    @Nullable @Flag("targetGroup") GroupConfiguration targetGroup
  ) {
    this.addPermission(permissionUser, rawPermission, potency, timeOut, targetGroup);
    source.sendMessage(I18n.trans("command-permissions-user-add-permission-successful",
      rawPermission,
      permissionUser.name()));
  }

  @CommandMethod("permissions|perms user <user> remove permission <permission> [targetGroup]")
  public void removeUserPermission(
    @NonNull CommandSource source,
    @NonNull @Argument("user") PermissionUser permissionUser,
    @NonNull @Argument("permission") String rawPermission,
    @Nullable @Argument("targetGroup") GroupConfiguration groupConfiguration
  ) {
    this.removePermission(permissionUser, rawPermission, groupConfiguration);
    source.sendMessage(I18n.trans("command-permissions-user-remove-permission-successful",
      rawPermission,
      permissionUser.name()));
  }

  @CommandMethod("permissions|perms user <user> remove group <group>")
  public void removeGroupFromUser(
    @NonNull CommandSource source,
    @NonNull @Argument("user") PermissionUser permissionUser,
    @NonNull @Argument(value = "group", suggestions = "permissionGroup") @Quoted String permissionGroup
  ) {
    this.updateUserDirect(permissionUser, user -> user.removeGroup(permissionGroup));
    source.sendMessage(I18n.trans("command-permissions-user-remove-group-successful",
      permissionUser.name(),
      permissionGroup));
  }

  @CommandMethod("permissions|perms group")
  public void displayGroupInformation(@NonNull CommandSource source) {
    for (var group : this.permissionManagement.groups()) {
      this.displayGroup(source, group);
      source.sendMessage(" ");
    }
  }

  @CommandMethod("permissions|perms group <group>")
  public void displayGroupInformation(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup
  ) {
    this.displayGroup(source, permissionGroup);
  }

  @CommandMethod("permissions|perms group <group> set sortId <sortId>")
  public void setSortId(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup,
    @Argument("sortId") int sortId
  ) {
    this.updateGroup(permissionGroup, group -> group.sortId(sortId));
    source.sendMessage(I18n.trans("command-permissions-group-set-property",
      "sortId",
      permissionGroup.name(),
      sortId));
  }

  @CommandMethod("permissions|perms group <group> set display <display>")
  public void setDisplay(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup,
    @NonNull @Quoted @Argument("display") String display
  ) {
    this.updateGroup(permissionGroup, group -> group.display(display));
    source.sendMessage(I18n.trans("command-permissions-group-set-property",
      "display",
      permissionGroup.name(),
      display));
  }

  @CommandMethod("permissions|perms group <group> set prefix <prefix>")
  public void setPrefix(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup,
    @NonNull @Quoted @Argument("prefix") String prefix
  ) {
    this.updateGroup(permissionGroup, group -> group.prefix(prefix));
    source.sendMessage(I18n.trans("command-permissions-group-set-property",
      "prefix",
      permissionGroup.name(),
      prefix));
  }

  @CommandMethod("permissions|perms group <group> set suffix <suffix>")
  public void setSuffix(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup,
    @NonNull @Quoted @Argument("suffix") String suffix
  ) {
    this.updateGroup(permissionGroup, group -> group.suffix(suffix));
    source.sendMessage(I18n.trans("command-permissions-group-set-property",
      "suffix",
      permissionGroup.name(),
      suffix));
  }

  @CommandMethod("permissions|perms group <group> set color <color>")
  public void setColor(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup,
    @NonNull @Quoted @Argument("color") String color
  ) {
    this.updateGroup(permissionGroup, group -> group.color(color));
    source.sendMessage(I18n.trans("command-permissions-group-set-property", "color", permissionGroup.name(), color));
  }

  @CommandMethod("permissions|perms group <group> set defaultGroup <defaultGroup>")
  public void setDefaultGroup(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup,
    @Argument("defaultGroup") boolean defaultGroup
  ) {
    this.updateGroup(permissionGroup, group -> group.defaultGroup(defaultGroup));
    source.sendMessage(I18n.trans("command-permissions-group-set-property",
      "defaultGroup",
      permissionGroup.name(),
      defaultGroup));
  }

  @CommandMethod("permissions|perms group <group> add group <name>")
  public void addInheritGroup(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup,
    @NonNull @Argument("name") PermissionGroup targetGroup
  ) {
    this.updateGroup(permissionGroup, group -> group.addGroup(targetGroup));
    source.sendMessage(I18n.trans("command-permissions-group-add-group-successful",
      permissionGroup.name(),
      targetGroup.name()));
  }

  @CommandMethod("permissions|perms group <group> add permission <permission>")
  public void addGroupPermission(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup,
    @NonNull @Argument("permission") String rawPermission,
    @Nullable @Flag("potency") Integer potency,
    @Nullable @Flag("duration") Duration timeOut,
    @Nullable @Flag("targetGroup") GroupConfiguration targetGroup
  ) {
    this.addPermission(permissionGroup, rawPermission, potency, timeOut, targetGroup);
    source.sendMessage(I18n.trans("command-permissions-group-add-permission-successful",
      rawPermission,
      permissionGroup.name()));
  }

  @CommandMethod("permissions|perms group <group> remove permission <permission> [targetGroup]")
  public void removeGroupPermission(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup,
    @NonNull @Argument("permission") String rawPermission,
    @Nullable @Argument("targetGroup") GroupConfiguration groupConfiguration
  ) {
    this.removePermission(permissionGroup, rawPermission, groupConfiguration);
    source.sendMessage(I18n.trans("command-permissions-group-remove-permission-successful",
      rawPermission,
      permissionGroup.name()));
  }

  @CommandMethod("permissions|perms group <group> remove group <name>")
  public void removeInheritGroup(
    @NonNull CommandSource source,
    @NonNull @Argument("group") PermissionGroup permissionGroup,
    @NonNull @Argument(value = "name", suggestions = "permissionGroup") @Quoted String targetGroup
  ) {
    this.updateGroupDirect(permissionGroup, group -> group.groupNames().remove(targetGroup));
    source.sendMessage(I18n.trans("command-permissions-group-remove-group-successful",
      targetGroup,
      permissionGroup.name()));
  }

  private void displayGroup(@NonNull CommandSource source, @NonNull PermissionGroup permissionGroup) {
    source.sendMessage("- " + permissionGroup.name() + " | Potency: " + permissionGroup.potency());
    source.sendMessage(" - Inherits: " + Arrays.toString(permissionGroup.groupNames().toArray()));
    source.sendMessage(" - Default: " + permissionGroup.defaultGroup());
    source.sendMessage(" - SortId: " + permissionGroup.sortId());
    source.sendMessage(" - Prefix: " + permissionGroup.prefix());
    source.sendMessage(" - Color: " + permissionGroup.color());
    source.sendMessage(" - Suffix: " + permissionGroup.suffix());
    source.sendMessage(" - Chat-Display: " + permissionGroup.display());
    this.displayPermission(source, permissionGroup);
  }

  private void displayPermission(@NonNull CommandSource source, @NonNull Permissible permissible) {
    source.sendMessage(" - Permissions:");
    for (var permission : permissible.permissions()) {
      source.sendMessage("  " + this.formatPermission(permission));
    }

    for (var groupPermission : permissible.groupPermissions().entrySet()) {
      source.sendMessage(" * " + groupPermission.getKey());

      for (var permission : groupPermission.getValue()) {
        source.sendMessage("  " + this.formatPermission(permission));
      }
    }
  }

  private @NonNull String formatPermission(@NonNull Permission permission) {
    var timeout = "LIFETIME";
    if (permission.timeOutMillis() > 0) {
      var timeoutTime = Instant.ofEpochMilli(permission.timeOutMillis()).atZone(ZoneId.systemDefault());
      timeout = DATE_TIME_FORMATTER.format(timeoutTime);
    }

    return "- " + permission.name() + " | Potency: " + permission.potency() + " | Timeout: " + timeout;
  }

  private void addPermission(
    @NonNull Permissible permissible,
    @NonNull String rawPermission,
    @Nullable Integer potency,
    @Nullable Duration timeOut,
    @Nullable GroupConfiguration targetGroup
  ) {
    var permission = Permission.builder().name(rawPermission);
    if (potency != null) {
      permission.potency(potency);
    }

    if (timeOut != null) {
      permission.timeOutMillis(System.currentTimeMillis() + timeOut.toMillis());
    }

    if (targetGroup != null) {
      permissible.addPermission(targetGroup.name(), permission.build());
    } else {
      permissible.addPermission(permission.build());
    }

    this.updatePermissible(permissible);
  }

  private void removePermission(
    @NonNull Permissible permissible,
    @NonNull String permission,
    @Nullable GroupConfiguration targetGroup
  ) {
    if (targetGroup != null) {
      permissible.removePermission(permission, targetGroup.name());
    } else {
      permissible.removePermission(permission);
    }

    this.updatePermissible(permissible);
  }

  private void updatePermissible(@NonNull Permissible permissible) {
    if (permissible instanceof PermissionUser user) {
      this.permissionManagement.updateUser(user);
    } else if (permissible instanceof PermissionGroup group) {
      this.permissionManagement.updateGroup(group);
    }
  }

  private void updateGroup(@NonNull PermissionGroup group, @NonNull Consumer<PermissionGroup.Builder> groupConsumer) {
    var builder = PermissionGroup.builder(group);
    groupConsumer.accept(builder);
    this.permissionManagement.updateGroup(builder.build());
  }

  private void updateGroupDirect(@NonNull PermissionGroup group, @NonNull Consumer<PermissionGroup> groupConsumer) {
    groupConsumer.accept(group);
    this.permissionManagement.updateGroup(group);
  }

  private void updateUser(
    @NonNull PermissionUser permissionUser,
    @NonNull Consumer<PermissionUser.Builder> permissionUserConsumer
  ) {
    var builder = PermissionUser.builder(permissionUser);
    // apply the change action
    permissionUserConsumer.accept(builder);
    // update the user
    this.permissionManagement.updateUser(builder.build());
  }

  private void updateUserDirect(
    @NonNull PermissionUser permissionUser,
    @NonNull Consumer<PermissionUser> permissionUserConsumer
  ) {
    permissionUserConsumer.accept(permissionUser);
    // update the user
    this.permissionManagement.updateUser(permissionUser);
  }
}
