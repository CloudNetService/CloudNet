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

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Confirmation;
import cloud.commandframework.annotations.Flag;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.permission.AbstractPermissible;
import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.command.source.ConsoleCommandSource;
import eu.cloudnetservice.node.console.Console;
import eu.cloudnetservice.node.console.animation.progressbar.ConsoleProgressAnimation;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@CommandAlias("export")
@Deprecated(forRemoval = true)
@CommandPermission("cloudnet.command.export")
@Description("Exports cloudperms permission data")
public class ExportCloudPermsCommand {

  private static final String USER_DB_NAME = "cloudnet_permission_users";
  private static final Logger LOGGER = LogManager.logger(ExportCloudPermsCommand.class);
  private static final SimpleDateFormat EXPORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

  private final Console console;
  private final NodeDatabaseProvider databaseProvider;
  private final PermissionManagement permissionManagement;

  @Inject
  public ExportCloudPermsCommand(
    @NonNull Console console,
    @NonNull NodeDatabaseProvider databaseProvider,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.console = console;
    this.databaseProvider = databaseProvider;
    this.permissionManagement = permissionManagement;
  }

  @Confirmation
  @CommandMethod(value = "permissions|perms export", requiredSender = ConsoleCommandSource.class)
  public void exportCloudPermsData(@NonNull CommandSource source, @Flag("includeUsers") boolean includeUsers) {
    source.sendMessage("Starting export of cloudperms data. This might take a while.");

    var result = new JsonObject();
    var metadata = new JsonObject();
    metadata.add("generatedBy", new JsonPrimitive("CloudNet - CloudPerms Export"));
    metadata.add("generatedAt", new JsonPrimitive(EXPORT_DATE_FORMAT.format(new Date())));
    result.add("metadata", metadata);

    source.sendMessage("Exported metadata. Starting export of permission groups.");
    var groups = new JsonObject();
    for (var group : this.permissionManagement.groups().stream().sorted().toList()) {
      source.sendMessage("Starting export of group %s".formatted(group.name()));

      var groupJson = new JsonObject();
      var nodes = new JsonArray();

      this.writePermissions(group, nodes);
      this.writeGroups(group, nodes);

      this.writeExtra("prefix", "prefix.%d.%s".formatted(group.potency(), group.prefix()), nodes);
      this.writeExtra("suffix", "suffix.%d.%s".formatted(group.potency(), group.suffix()), nodes);
      this.writeExtra("weight", "weight.%d".formatted(group.potency()), nodes);

      groupJson.add("nodes", nodes);
      groups.add(StringUtil.toLower(group.name()), groupJson);
      source.sendMessage("Export of group %s was finished.".formatted(group.name()));
    }

    result.add("groups", groups);
    source.sendMessage("Export of all groups was finished.");

    if (includeUsers) {
      source.sendMessage("Starting to export all permission users. This might take a while. Please wait...");

      var users = new JsonObject();
      var database = this.databaseProvider.database(USER_DB_NAME);

      var progress = ConsoleProgressAnimation.createDefault("Export", "Users", 1, database.documentCount());
      this.console.startAnimation(progress);

      // select all users from the database, in 100 user chunks
      database.iterate(($, data) -> {
        // deserialize the permission user
        var user = data.toInstanceOf(PermissionUser.class);

        // remove all outdated groups and permissions
        this.permissionManagement.testPermissionUser(user);

        var userJson = new JsonObject();
        userJson.add("username", new JsonPrimitive(user.name()));

        var nodes = new JsonArray();
        this.writePermissions(user, nodes);
        this.writeGroups(user, nodes);

        userJson.add("nodes", nodes);
        users.add(user.uniqueId().toString(), userJson);

        progress.step();

      }, 100);

      result.add("users", users);
    }

    try {
      var gzipStream = new GZIPOutputStream(Files.newOutputStream(Path.of("cloudperms-export.json.gz")));
      var outputWriter = new OutputStreamWriter(gzipStream);
      outputWriter.write(result.toString());

      outputWriter.close();
      gzipStream.close();

      source.sendMessage("Export finished and was saved to cloudperms-export.json.gz");
    } catch (IOException exception) {
      LOGGER.severe("Unable to write export to file", exception);
    }
  }

  private void writeExtra(@NonNull String key, @NonNull String value, @NonNull JsonArray nodes) {
    var extra = new JsonObject();
    extra.add("type", new JsonPrimitive(key));
    extra.add("key", new JsonPrimitive(value));
    extra.add("value", new JsonPrimitive(true));

    nodes.add(extra);
  }

  private void writeGroups(@NonNull AbstractPermissible permissible, @NonNull JsonArray nodes) {
    for (var groupName : permissible.groupNames()) {
      var inheritanceJson = new JsonObject();
      inheritanceJson.add("type", new JsonPrimitive("inheritance"));
      inheritanceJson.add("key", new JsonPrimitive("group." + StringUtil.toLower(groupName)));
      inheritanceJson.add("value", new JsonPrimitive(true));

      if (permissible instanceof PermissionUser user) {
        var groupInfo = user.findAssignedGroup(groupName);
        if (groupInfo != null && groupInfo.timeOutMillis() >= 1) {
          // just make sure that we dont get anything else
          long timeOutSeconds = groupInfo.timeOutMillis() / 1000L;
          inheritanceJson.add("expiry", new JsonPrimitive(timeOutSeconds));
        }
      }

      nodes.add(inheritanceJson);
    }
  }

  private void writePermissions(@NonNull AbstractPermissible permissible, @NonNull JsonArray nodes) {
    for (var permission : permissible.permissions()) {
      this.writePermission(nodes, permission, null);
    }

    for (var groupName : permissible.groupPermissions().keySet()) {
      var permissions = permissible.groupPermissions().get(groupName);
      for (var permission : permissions) {
        this.writePermission(nodes, permission, groupName);
      }
    }
  }

  private void writePermission(@NonNull JsonArray array, @NonNull Permission permission, @Nullable String targetGroup) {
    var permissionJson = new JsonObject();
    permissionJson.add("type", new JsonPrimitive("permission"));
    permissionJson.add("key", new JsonPrimitive(permission.name()));
    permissionJson.add("value", new JsonPrimitive(permission.potency() >= 0));

    // we need to add the expiry in this case
    if (permission.timeOutMillis() >= 1) {
      // just make sure that we dont get anything else
      long timeOutSeconds = permission.timeOutMillis() / 1000L;
      permissionJson.add("expiry", new JsonPrimitive(timeOutSeconds));
    }

    if (targetGroup != null) {
      var context = new JsonObject();
      context.add("group", new JsonPrimitive(targetGroup));
      permissionJson.add("context", context);
    }

    array.add(permissionJson);
  }

}
