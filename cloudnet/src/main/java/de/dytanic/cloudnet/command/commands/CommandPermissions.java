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

package de.dytanic.cloudnet.command.commands;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.anyStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.bool;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.integer;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.positiveInteger;

import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.CommandInterrupt;
import de.dytanic.cloudnet.command.sub.SubCommandArgumentWrapper;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.collection.Triple;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.ConsoleColor;
import de.dytanic.cloudnet.driver.permission.IPermissible;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionUserGroupInfo;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommandPermissions extends SubCommandHandler {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  public CommandPermissions() {
    super(
      SubCommandBuilder.create()

        .preExecute(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            if (CloudNet.getInstance().getPermissionManagement() == null) {
              sender.sendMessage(LanguageManager.getMessage("command-permissions-not-enabled"));
              throw new CommandInterrupt();
            }
          }
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            CloudNet.getInstance().getPermissionManagement().reload();
            sender.sendMessage(LanguageManager.getMessage("command-permissions-reload-permissions-success"));
          },
          anyStringIgnoreCase("reload", "rl")
        )

        .prefix(anyStringIgnoreCase("create", "new"))
        .applyHandler(CommandPermissions::handleCreateCommands)
        .removeLastPrefix()

        .prefix(anyStringIgnoreCase("delete", "remove"))
        .applyHandler(CommandPermissions::handleDeleteCommands)
        .removeLastPrefix()

        .prefix(anyStringIgnoreCase("user", "u"))
        .applyHandler(CommandPermissions::handleUserCommands)
        .removeLastPrefix()

        .prefix(anyStringIgnoreCase("group", "g"))
        .applyHandler(CommandPermissions::handleGroupCommands)
        .removeLastPrefix()

        .getSubCommands(),
      "permissions", "perms"
    );
    super.prefix = "cloudnet";
    super.permission = "cloudnet.command." + super.names[0];
    super.description = LanguageManager.getMessage("command-description-permissions");
  }

  private static void handleCreateCommands(SubCommandBuilder builder) {
    builder
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionUser permissionUser = CloudNet.getInstance().getPermissionManagement()
            .addUser((String) args.argument(2), (String) args.argument(3), (int) args.argument(4));
          sender.sendMessage(
            LanguageManager.getMessage("command-permissions-create-user-successful")
              .replace("%name%", permissionUser.getName())
          );
        },
        anyStringIgnoreCase("user", "u"),
        dynamicString(
          "name",
          LanguageManager.getMessage("command-permissions-create-user-already-exists"),
          input -> !CloudNet.getInstance().getPermissionManagement().containsUser(input)
        ),
        dynamicString("password"),
        integer("potency")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionGroup group = CloudNet.getInstance().getPermissionManagement()
            .addGroup((String) args.argument(2), (int) args.argument(3));
          sender.sendMessage(
            LanguageManager.getMessage("command-permissions-create-group-successful")
              .replace("%name%", group.getName())
          );
        },
        anyStringIgnoreCase("group", "g"),
        dynamicString(
          "name",
          LanguageManager.getMessage("command-permissions-create-group-already-exists"),
          input -> !CloudNet.getInstance().getPermissionManagement().containsGroup(input)
        ),
        integer("potency")
      );
  }

  private static void handleDeleteCommands(SubCommandBuilder builder) {
    builder
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          CloudNet.getInstance().getPermissionManagement().deleteUser((String) args.argument(2));

          sender.sendMessage(LanguageManager.getMessage("command-permissions-delete-user-successful")
            .replace("%name%", (String) args.argument(2)));
        },
        anyStringIgnoreCase("user", "u"),
        dynamicString(
          "name",
          LanguageManager.getMessage("command-permissions-user-not-found"),
          input -> CloudNet.getInstance().getPermissionManagement().containsUser(input)
        )
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          CloudNet.getInstance().getPermissionManagement().deleteGroup((String) args.argument(2));

          sender.sendMessage(LanguageManager.getMessage("command-permissions-delete-group-successful")
            .replace("%name%", (String) args.argument(2)));
        },
        anyStringIgnoreCase("group", "g"),
        dynamicString(
          "name",
          LanguageManager.getMessage("command-permissions-group-not-found"),
          input -> CloudNet.getInstance().getPermissionManagement().containsGroup(input),
          () -> CloudNet.getInstance().getPermissionManagement().getGroups().stream().map(INameable::getName)
            .collect(Collectors.toList())
        )
      );
  }

  private static void handleUserCommands(SubCommandBuilder builder) {
    builder
      .prefix(dynamicString(
        "user",
        LanguageManager.getMessage("command-permissions-user-not-found"),
        input -> CloudNet.getInstance().getPermissionManagement().containsUser(input)
      ))
      .preExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) ->
        internalProperties
          .put("user", CloudNet.getInstance().getPermissionManagement().getFirstUser((String) args.argument(1)))
      )

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> displayUser(sender,
          (IPermissionUser) internalProperties.get("user"))
      )

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionUser user = (IPermissionUser) internalProperties.get("user");
          String oldName = user.getName();
          String name = (String) args.argument("name").get();

          user.setName(name);
          CloudNet.getInstance().getPermissionManagement().updateUser(user);

          sender.sendMessage(LanguageManager.getMessage("command-permissions-user-rename-success")
            .replace("%name%", oldName)
            .replace("%new_name%", name)
          );
        },
        exactStringIgnoreCase("rename"),
        dynamicString("name")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionUser user = (IPermissionUser) internalProperties.get("user");
          String password = (String) args.argument("password").get();

          user.changePassword(password);
          CloudNet.getInstance().getPermissionManagement().updateUser(user);

          sender.sendMessage(LanguageManager.getMessage("command-permissions-user-change-password-success")
            .replace("%name%", user.getName()));
        },
        exactStringIgnoreCase("changePassword"),
        dynamicString("password")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionUser user = (IPermissionUser) internalProperties.get("user");
          String password = (String) args.argument("password").get();

          sender.sendMessage(LanguageManager.getMessage("command-permissions-user-check-password")
            .replace("%name%", user.getName())
            .replace("%checked%", String.valueOf(user.checkPassword(password)))
          );
        },
        exactStringIgnoreCase("check"),
        dynamicString("password")
      )

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionUser user = ((IPermissionUser) internalProperties.get("user"));
          String group = (String) args.argument("name").get();
          user.addGroup(group);

          CloudNet.getInstance().getPermissionManagement().updateUser(user);
          sender.sendMessage(LanguageManager.getMessage("command-permissions-user-add-group-successful")
            .replace("%name%", user.getName())
            .replace("%group%", group)
          );
        },
        exactStringIgnoreCase("add"),
        anyStringIgnoreCase("group", "g"),
        dynamicString(
          "name",
          LanguageManager.getMessage("command-permissions-group-not-found"),
          input -> CloudNet.getInstance().getPermissionManagement().containsGroup(input)
        )
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionUser user = ((IPermissionUser) internalProperties.get("user"));
          String group = (String) args.argument("name").get();
          String timeString = (String) args.argument("time in days | lifetime").orElse("lifetime");
          long timeout = timeString.equalsIgnoreCase("lifetime") ? -1
            : TimeUnit.DAYS.toMillis(Long.parseLong(timeString)) + System.currentTimeMillis();
          user.addGroup(group, timeout);

          CloudNet.getInstance().getPermissionManagement().updateUser(user);
          sender.sendMessage(LanguageManager.getMessage("command-permissions-user-add-group-successful")
            .replace("%name%", user.getName())
            .replace("%group%", group)
          );
        },
        exactStringIgnoreCase("add"),
        anyStringIgnoreCase("group", "g"),
        dynamicString(
          "name",
          LanguageManager.getMessage("command-permissions-group-not-found"),
          input -> CloudNet.getInstance().getPermissionManagement().containsGroup(input)
        ),
        dynamicString(
          "time in days | lifetime",
          input -> input.equalsIgnoreCase("lifetime") || Ints.tryParse(input) != null
        )
      )

      .applyHandler(ignored -> handlePermissionCommands(
        builder,
        triple -> {
          IPermissionUser user = (IPermissionUser) triple.getSecond().get("user");

          Permission permission = addPermission(user, triple.getThird());

          CloudNet.getInstance().getPermissionManagement().updateUser(user);
          triple.getFirst().sendMessage(LanguageManager.getMessage("command-permissions-user-add-permission-successful")
            .replace("%name%", user.getName())
            .replace("%permission%", permission.getName())
            .replace("%potency%", String.valueOf(permission.getPotency()))
          );
        },
        triple -> {
          IPermissionUser user = (IPermissionUser) triple.getSecond().get("user");
          String permission = removePermission(user, triple.getThird());

          CloudNet.getInstance().getPermissionManagement().updateUser(user);
          triple.getFirst()
            .sendMessage(LanguageManager.getMessage("command-permissions-user-remove-permission-successful")
              .replace("%name%", user.getName())
              .replace("%permission%", permission)
            );
        })
      )

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionUser user = (IPermissionUser) internalProperties.get("user");
          String group = (String) args.argument("group").get();
          user.removeGroup(group);

          CloudNet.getInstance().getPermissionManagement().updateUser(user);
          sender.sendMessage(LanguageManager.getMessage("command-permissions-user-remove-group-successful")
            .replace("%name%", user.getName())
            .replace("%group%", group)
          );
        },
        anyStringIgnoreCase("remove", "rm"),
        anyStringIgnoreCase("group", "g"),
        dynamicString("group")

      )

      .removeLastPrefix()
      .removeLastPreHandler();
  }

  private static void handleGroupCommands(SubCommandBuilder builder) {
    builder
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          sender.sendMessage("Groups: ", " ");
          for (IPermissionGroup group : CloudNet.getInstance().getPermissionManagement().getGroups()) {
            displayGroup(sender, group);
            sender.sendMessage(" ");
          }
        }
      )

      .prefix(dynamicString(
        "group",
        LanguageManager.getMessage("command-permissions-group-not-found"),
        input -> CloudNet.getInstance().getPermissionManagement().containsGroup(input),
        () -> CloudNet.getInstance().getPermissionManagement().getGroups().stream().map(INameable::getName)
          .collect(Collectors.toList())
      ))
      .preExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) ->
        internalProperties
          .put("group", CloudNet.getInstance().getPermissionManagement().getGroup((String) args.argument(1)))
      )

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> displayGroup(sender,
          (IPermissionGroup) internalProperties.get("group"))
      )

      .prefix(exactStringIgnoreCase("set"))
      .postExecute(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionGroup group = (IPermissionGroup) internalProperties.get("group");
          CloudNet.getInstance().getPermissionManagement().updateGroup(group);

          sender.sendMessage(
            LanguageManager.getMessage("command-permissions-group-update-property")
              .replace("%group%", group.getName())
              .replace("%value%", String.valueOf(args.argument(4)))
              .replace("%property%", (String) args.argument(3))
          );
        }
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> ((IPermissionGroup) internalProperties
          .get("group")).setSortId((Integer) args.argument(4)),
        exactStringIgnoreCase("sortId"),
        positiveInteger("sortId")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> ((IPermissionGroup) internalProperties
          .get("group")).setDisplay((String) args.argument(4)),
        subCommand -> subCommand.setMinArgs(5).setMaxArgs(Integer.MAX_VALUE),
        exactStringIgnoreCase("display"),
        dynamicString("display")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> ((IPermissionGroup) internalProperties
          .get("group")).setPrefix((String) args.argument(4)),
        subCommand -> subCommand.setMinArgs(5).setMaxArgs(Integer.MAX_VALUE),
        exactStringIgnoreCase("prefix"),
        dynamicString("prefix")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> ((IPermissionGroup) internalProperties
          .get("group")).setSuffix((String) args.argument(4)),
        subCommand -> subCommand.setMinArgs(5).setMaxArgs(Integer.MAX_VALUE),
        exactStringIgnoreCase("suffix"),
        dynamicString("suffix")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> ((IPermissionGroup) internalProperties
          .get("group")).setDefaultGroup((boolean) args.argument(4)),
        exactStringIgnoreCase("defaultGroup"),
        bool("defaultGroup")
      )
      .removeLastPostHandler()

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionGroup group = (IPermissionGroup) internalProperties.get("group");

          String rawColor = (String) args.argument(4);

          group.setColor(rawColor);
          CloudNet.getInstance().getPermissionManagement().updateGroup(group);

          String rawColorCode = rawColor.replace("&", "").replace("§", "");
          ConsoleColor color = rawColorCode.length() == 0 ? null : ConsoleColor.getByChar(rawColorCode.charAt(0));

          sender.sendMessage(
            LanguageManager.getMessage("command-permissions-group-update-property")
              .replace("%group%", group.getName())
              .replace("%value%", color != null ? "&" + color.getIndex() + color.name() : rawColor)
              .replace("%property%", (String) args.argument(3))
          );
        },
        subCommand -> subCommand.appendUsage("| 1.13+"),
        exactStringIgnoreCase("color"),
        dynamicString("color", 2)
      )
      .removeLastPrefix()

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionGroup group = ((IPermissionGroup) internalProperties.get("group"));
          String name = (String) args.argument("name").get();
          group.getGroups().add(name);

          CloudNet.getInstance().getPermissionManagement().updateGroup(group);
          sender.sendMessage(LanguageManager.getMessage("command-permissions-group-add-group-successful")
            .replace("%name%", group.getName())
            .replace("%group%", name)
          );
        },
        exactStringIgnoreCase("add"),
        anyStringIgnoreCase("group", "g"),
        dynamicString(
          "name",
          LanguageManager.getMessage("command-permissions-group-not-found"),
          input -> CloudNet.getInstance().getPermissionManagement().containsGroup(input)
        )
      )

      .applyHandler(ignored -> handlePermissionCommands(
        builder,
        triple -> {
          IPermissionGroup group = (IPermissionGroup) triple.getSecond().get("group");

          Permission permission = addPermission(group, triple.getThird());

          CloudNet.getInstance().getPermissionManagement().updateGroup(group);
          triple.getFirst()
            .sendMessage(LanguageManager.getMessage("command-permissions-group-add-permission-successful")
              .replace("%name%", group.getName())
              .replace("%permission%", permission.getName())
              .replace("%potency%", String.valueOf(permission.getPotency()))
            );
        },
        triple -> {
          IPermissionGroup group = (IPermissionGroup) triple.getSecond().get("group");
          String permission = removePermission(group, triple.getThird());

          CloudNet.getInstance().getPermissionManagement().updateGroup(group);
          triple.getFirst()
            .sendMessage(LanguageManager.getMessage("command-permissions-group-remove-permission-successful")
              .replace("%name%", group.getName())
              .replace("%permission%", permission)
            );
        })
      )

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          IPermissionGroup group = (IPermissionGroup) internalProperties.get("group");
          String name = (String) args.argument("name").get();
          group.getGroups().remove(name);

          CloudNet.getInstance().getPermissionManagement().updateGroup(group);
          sender.sendMessage(LanguageManager.getMessage("command-permissions-group-remove-group-successful")
            .replace("%name%", name)
            .replace("%group%", group.getName())
          );
        },
        anyStringIgnoreCase("remove", "rm"),
        anyStringIgnoreCase("group", "g"),
        dynamicString("name")
      )

      .removeLastPrefix()
      .removeLastPreHandler();
  }

  private static void handlePermissionCommands(SubCommandBuilder builder,
    Consumer<Triple<ICommandSender, Map<String, Object>, SubCommandArgumentWrapper>> addPermissionHandler,
    Consumer<Triple<ICommandSender, Map<String, Object>, SubCommandArgumentWrapper>> removePermissionHandler) {
    builder
      .prefix(exactStringIgnoreCase("add"))
      .prefix(anyStringIgnoreCase("permission", "perm"))
      .prefix(dynamicString("permission"))

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> addPermissionHandler
          .accept(new Triple<>(sender, internalProperties, args))
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> addPermissionHandler
          .accept(new Triple<>(sender, internalProperties, args)),
        integer("potency")
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> addPermissionHandler
          .accept(new Triple<>(sender, internalProperties, args)),
        integer("potency"),
        dynamicString(
          "targetGroup",
          LanguageManager.getMessage("command-permissions-target-group-not-found"),
          input -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(input)
        )
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> addPermissionHandler
          .accept(new Triple<>(sender, internalProperties, args)),
        integer("potency"),
        dynamicString(
          "time in days | lifetime",
          input -> input.equalsIgnoreCase("lifetime") || Ints.tryParse(input) != null
        )
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> addPermissionHandler
          .accept(new Triple<>(sender, internalProperties, args)),
        dynamicString(
          "targetGroup",
          LanguageManager.getMessage("command-permissions-target-group-not-found"),
          input -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(input),
          () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
            .map(GroupConfiguration::getName).collect(Collectors.toList())
        )
      )
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> addPermissionHandler
          .accept(new Triple<>(sender, internalProperties, args)),
        integer("potency"),
        dynamicString(
          "time in days | lifetime",
          input -> input.equalsIgnoreCase("lifetime") || Ints.tryParse(input) != null
        ),
        dynamicString(
          "targetGroup",
          LanguageManager.getMessage("command-permissions-target-group-not-found"),
          input -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(input),
          () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
            .map(GroupConfiguration::getName).collect(Collectors.toList())
        )
      )
      .executeMultipleTimes(3, SubCommandBuilder::removeLastPrefix) //add permission <permission>

      .prefix(anyStringIgnoreCase("remove", "rm"))

      .prefix(anyStringIgnoreCase("permission", "perm"))
      .prefix(dynamicString("permission"))
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> removePermissionHandler
          .accept(new Triple<>(sender, internalProperties, args)))
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> removePermissionHandler
          .accept(new Triple<>(sender, internalProperties, args)),
        dynamicString(
          "targetGroup",
          LanguageManager.getMessage("command-permissions-target-group-not-found"),
          input -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(input),
          () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
            .map(GroupConfiguration::getName).collect(Collectors.toList())
        )
      )
      .executeMultipleTimes(3, SubCommandBuilder::removeLastPrefix); //remove permission <permission>
  }

  private static void displayUser(ICommandSender sender, IPermissionUser permissionUser) {
    sender.sendMessage(
      "* " + permissionUser.getUniqueId() + ":" + permissionUser.getName() + " | " + permissionUser.getPotency(),
      "Groups: "
    );

    for (PermissionUserGroupInfo groupInfo : permissionUser.getGroups()) {
      sender.sendMessage("- " + groupInfo.getGroup() + ": " + (groupInfo.getTimeOutMillis() > 0 ?
        DATE_FORMAT.format(groupInfo.getTimeOutMillis()) : "LIFETIME"));
    }
    if (permissionUser.getGroups().isEmpty()) {
      sender.sendMessage(
        "- " + CloudNet.getInstance().getPermissionManagement().getDefaultPermissionGroup().getName() + ": LIFETIME");
    }

    sender.sendMessage(" ");
    displayPermissions(sender, permissionUser);
  }

  private static void displayGroup(ICommandSender sender, IPermissionGroup permissionGroup) {
    sender.sendMessage(
      "* " + permissionGroup.getName() + " | " + permissionGroup.getPotency(),
      "Inherits: " + permissionGroup.getGroups(),
      "Default: " + permissionGroup.isDefaultGroup() + " | SortId: " + permissionGroup.getSortId(),
      "Prefix: " + (sender instanceof ConsoleCommandSender ? permissionGroup.getPrefix()
        : permissionGroup.getPrefix().replace("&", "§")),
      "Color: " + permissionGroup.getColor().replace("&", "[color]"),
      "Suffix: " + (sender instanceof ConsoleCommandSender ? permissionGroup.getSuffix()
        : permissionGroup.getSuffix().replace("&", "§")),
      "Display: " + (sender instanceof ConsoleCommandSender ? permissionGroup.getDisplay()
        : permissionGroup.getDisplay().replace("&", "§"))
    );

    displayPermissions(sender, permissionGroup);
  }

  private static void displayPermissions(ICommandSender sender, IPermissible permissible) {
    sender.sendMessage("Permissions: ");
    for (Permission permission : permissible.getPermissions()) {
      sender.sendMessage("- " + permission.getName() + ":" + permission.getPotency() + " | Timeout " +
        (permission.getTimeOutMillis() > 0 ?
          DATE_FORMAT.format(permission.getTimeOutMillis()) : "LIFETIME"));
    }

    sender.sendMessage(" ");

    for (Map.Entry<String, Collection<Permission>> groupPermissions : permissible.getGroupPermissions().entrySet()) {
      sender.sendMessage("* " + groupPermissions.getKey());

      for (Permission permission : groupPermissions.getValue()) {
        sender.sendMessage("- " + permission.getName() + ":" + permission.getPotency() + " | Timeout " +
          (permission.getTimeOutMillis() > 0 ?
            DATE_FORMAT.format(permission.getTimeOutMillis()) : "LIFETIME"));
      }
    }
  }

  private static Permission addPermission(IPermissible permissible, SubCommandArgumentWrapper args) {
    String permissionName = (String) args.argument("permission").get();
    int potency = (int) args.argument("potency").orElse(1);
    String timeString = (String) args.argument("time in days | lifetime").orElse("lifetime");
    long timeout = timeString.equalsIgnoreCase("lifetime") ? -1
      : TimeUnit.DAYS.toMillis(Long.parseLong(timeString)) + System.currentTimeMillis();
    String targetGroup = (String) args.argument("targetGroup").orElse(null);

    Permission permission = new Permission(permissionName, potency, timeout);

    if (targetGroup != null) {
      permissible.addPermission(targetGroup, permission);
    } else {
      permissible.addPermission(permission);
    }

    return permission;
  }

  private static String removePermission(IPermissible permissible, SubCommandArgumentWrapper args) {
    String permission = (String) args.argument("permission").get();
    Optional<Object> optionalTargetGroup = args.argument("targetGroup");

    if (optionalTargetGroup.isPresent()) {
      String targetGroup = (String) optionalTargetGroup.get();
      permissible.removePermission(targetGroup, permission);
    } else {
      permissible.removePermission(permission);
    }

    return permission;
  }

}
