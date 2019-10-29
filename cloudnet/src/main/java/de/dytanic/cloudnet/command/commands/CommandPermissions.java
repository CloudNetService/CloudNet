package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.permission.*;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class CommandPermissions extends CommandDefault implements ITabCompleter {

    private final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public CommandPermissions() {
        super("permissions", "perms");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    "perms users group <group>",
                    "perms create user <name> <password> <potency>",
                    "perms delete user <uniqueId>",
                    "perms create group <name> <potency>",
                    "perms delete group <name>",
                    "perms user <name>",
                    "perms user <name> password <password>",
                    "perms user <name> rename <name>",
                    "perms user <name> check <password>",
                    "perms user <name> add group <name>",
                    "perms user <name> add group <name> <time in days>",
                    "perms user <name> remove group <name>",
                    "perms user <name> add permission <permission> <potency>",
                    "perms user <name> add permission <permission> <potency> <time in days : lifetime>",
                    "perms user <name> add permission <permission> <potency> <time in days : lifetime> <target>",
                    "perms user <name> remove permission <permission>",
                    "perms user <name> remove permission <permission> <target>",
                    "perms group",
                    "perms group <name>",
                    "perms group <name> setSortId <sortId>",
                    "perms group <name> setDisplay <sortId>",
                    "perms group <name> setDefaultGroup <true : false>",
                    "perms group <name> setPrefix <prefix> | '_' as ' ' ",
                    "perms group <name> setSuffix <suffix> | '_' as ' ' ",
                    "perms group <name> setColor <color>",
                    "perms group <name> add permission <permission> <potency>",
                    "perms group <name> add permission <permission> <potency> <time in days : lifetime>",
                    "perms group <name> add permission <permission> <potency> <time in days : lifetime> <target>",
                    "perms group <name> remove permission <permission>",
                    "perms group <name> remove permission <permission> <target>",
                    "perms group <name> add group <name>",
                    "perms group <name> remove group <name>"
            );
            return;
        }

        IPermissionManagement permissionManagement = getCloudNet().getPermissionManagement();

        switch (args[0].toLowerCase()) {
            case "reload":
                getCloudNet().getPermissionManagement().reload();
                sender.sendMessage(LanguageManager.getMessage("command-permissions-reload-permissions-success"));
                break;
            case "users":
                if (args.length >= 3) {
                    if (args[1].equalsIgnoreCase("group")) {
                        for (IPermissionUser permissionUser : permissionManagement.getUserByGroup(args[2])) {
                            this.displayUser(sender, permissionUser);
                        }
                    }
                }
                break;
            case "create":
                if (args.length > 2) {
                    if (args[1].equalsIgnoreCase("user")) {
                        if (args.length == 5) {
                            if (permissionManagement.containsUser(args[2])) {
                                sender.sendMessage(LanguageManager.getMessage("command-permissions-create-user-already-exists"));
                                return;
                            }

                            try {
                                IPermissionUser permissionUser = permissionManagement.addUser(args[2], args[3], Integer.parseInt(args[4]));
                                sender.sendMessage(
                                        LanguageManager.getMessage("command-permissions-create-user-successful")
                                                .replace("%name%", permissionUser.getName())
                                );

                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }

                            return;
                        }
                    }

                    if (args[1].equalsIgnoreCase("group")) {
                        if (args.length == 4) {
                            if (permissionManagement.getGroup(args[2]) != null) {
                                sender.sendMessage(LanguageManager.getMessage("command-permissions-create-group-already-exists"));
                                return;
                            }

                            try {
                                IPermissionGroup group = permissionManagement.addGroup(args[2], Integer.parseInt(args[3]));
                                sender.sendMessage(
                                        LanguageManager.getMessage("command-permissions-create-group-successful")
                                                .replace("%name%", group.getName())
                                );

                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }

                            return;
                        }
                    }
                }
                break;
            case "delete":
                if (args.length == 3) {
                    if (args[1].equalsIgnoreCase("user")) {
                        permissionManagement.deleteUser(args[2]);

                        sender.sendMessage(LanguageManager.getMessage("command-permissions-delete-user-successful").replace("%name%", args[2]));
                        return;
                    }

                    if (args[1].equalsIgnoreCase("group")) {
                        permissionManagement.deleteGroup(args[2]);

                        sender.sendMessage(LanguageManager.getMessage("command-permissions-delete-group-successful").replace("%name%", args[2]));
                        return;
                    }
                }
                break;
            case "user":

                if (args.length == 1) {
                    return;
                }

                List<IPermissionUser> permissionUsers = permissionManagement.getUser(args[1]);
                IPermissionUser permissionUser = permissionUsers.isEmpty() ? null : permissionUsers.get(0);

                if (permissionUser != null) {
                    if (args.length == 2) {
                        this.displayUser(sender, permissionUser);
                        return;
                    }

                    if (args.length == 4) {
                        if (args[2].equalsIgnoreCase("password")) {
                            permissionUser.changePassword(args[3]);
                            permissionManagement.updateUser(permissionUser);

                            sender.sendMessage(LanguageManager.getMessage("command-permissions-user-change-password-success")
                                    .replace("%name%", args[1])
                            );
                            return;
                        }
                        if (args[2].equalsIgnoreCase("check")) {
                            sender.sendMessage(LanguageManager.getMessage("command-permissions-user-check-password")
                                    .replace("%name%", permissionUser.getName())
                                    .replace("%checked%", String.valueOf(permissionUser.checkPassword(args[3])))
                            );
                        }

                        if (args[2].equalsIgnoreCase("rename")) {
                            permissionUser.setName(args[3]);
                            permissionManagement.updateUser(permissionUser);

                            sender.sendMessage(LanguageManager.getMessage("command-permissions-user-rename-success")
                                    .replace("%name%", args[1])
                                    .replace("%new_name%", args[3])
                            );
                            return;
                        }
                        return;
                    }
                    if (args.length >= 5) {
                        if (args[2].equalsIgnoreCase("add")) {
                            if (args[3].equalsIgnoreCase("group")) {
                                if (permissionManagement.getGroup(args[4]) != null) {
                                    if (args.length == 6 && Validate.testStringParseToInt(args[5])) {
                                        permissionUser.addGroup(args[4], Integer.parseInt(args[5]), TimeUnit.DAYS);
                                    } else {
                                        permissionUser.addGroup(args[4]);
                                    }

                                    permissionManagement.updateUser(permissionUser);
                                    sender.sendMessage(LanguageManager.getMessage("command-permissions-user-add-group-successful")
                                            .replace("%name%", args[1])
                                            .replace("%group%", args[4])
                                    );
                                } else {
                                    sender.sendMessage(LanguageManager.getMessage("command-permissions-user-add-group-group-not-exists")
                                            .replace("%group%", args[4])
                                    );
                                }
                                return;
                            }
                        }
                        if (args[2].equalsIgnoreCase("remove")) {
                            if (args[3].equalsIgnoreCase("group")) {
                                permissionUser.removeGroup(args[4]);

                                permissionManagement.updateUser(permissionUser);
                                sender.sendMessage(LanguageManager.getMessage("command-permissions-user-remove-group-successful")
                                        .replace("%name%", args[1])
                                        .replace("%group%", args[4])
                                );
                                return;
                            }

                            if (args[3].equalsIgnoreCase("permission")) {
                                if (args.length == 6) {
                                    permissionUser.removePermission(args[5], args[4]);
                                } else {
                                    permissionUser.removePermission(args[4]);
                                }

                                permissionManagement.updateUser(permissionUser);

                                sender.sendMessage(LanguageManager.getMessage("command-permissions-user-remove-permission-successful")
                                        .replace("%name%", permissionUser.getName())
                                        .replace("%permission%", args[4])
                                );
                                return;
                            }
                        }
                    }
                    if (args.length >= 6 && args[3].equalsIgnoreCase("permission")) {
                        if (args[2].equalsIgnoreCase("add")) {
                            try {
                                if (!Validate.testStringParseToInt(args[5])) {
                                    return;
                                }

                                switch (args.length) {
                                    case 6:
                                        permissionUser.addPermission(new Permission(args[4], Integer.parseInt(args[5])));
                                        break;
                                    case 7:
                                        if (Validate.testStringParseToInt(args[6])) {
                                            permissionUser.addPermission(new Permission(args[4], Integer.parseInt(args[5]), Integer.parseInt(args[6]), TimeUnit.DAYS));
                                        } else {
                                            permissionUser.addPermission(new Permission(args[4], Integer.parseInt(args[5])));
                                        }

                                        break;
                                    case 8:
                                        if (Validate.testStringParseToInt(args[6])) {
                                            permissionUser.addPermission(args[7], new Permission(args[4], Integer.parseInt(args[5]), Integer.parseInt(args[6]), TimeUnit.DAYS));
                                        } else {
                                            permissionUser.addPermission(args[7], new Permission(args[4], Integer.parseInt(args[5])));
                                        }

                                        break;
                                }

                                permissionManagement.updateUser(permissionUser);

                                sender.sendMessage(LanguageManager.getMessage("command-permissions-user-add-permission-successful")
                                        .replace("%name%", permissionUser.getName())
                                        .replace("%permission%", args[4])
                                        .replace("%potency%", args[5])
                                );
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                            return;
                        }
                    }
                } else {
                    sender.sendMessage(LanguageManager.getMessage("command-permissions-user-not-found").replace("%name%", args[1]));
                }
                break;
            case "group":

                if (args.length == 1) {
                    sender.sendMessage("Groups: ", " ");
                    for (IPermissionGroup group : permissionManagement.getGroups()) {
                        this.displayGroup(sender, group);
                        sender.sendMessage(" ");
                    }

                    return;
                }

                IPermissionGroup permissionGroup = permissionManagement.getGroup(args[1]);

                if (permissionGroup != null) {
                    if (args.length == 2) {
                        this.displayGroup(sender, permissionGroup);
                        return;
                    }
                    if (args.length == 4) {
                        switch (args[2].toLowerCase()) {
                            case "setsortid":
                                if (Validate.testStringParseToInt(args[3])) {
                                    permissionGroup.setSortId(Integer.parseInt(args[3]));
                                    permissionManagement.updateGroup(permissionGroup);
                                    sender.sendMessage(
                                            LanguageManager.getMessage("command-permissions-group-update-property")
                                                    .replace("%group%", permissionGroup.getName())
                                                    .replace("%value%", args[3])
                                                    .replace("%property%", args[2])
                                    );
                                }
                                break;
                            case "setdefaultgroup":
                                permissionGroup.setDefaultGroup(args[3].equalsIgnoreCase("true"));
                                permissionManagement.updateGroup(permissionGroup);
                                sender.sendMessage(
                                        LanguageManager.getMessage("command-permissions-group-update-property")
                                                .replace("%group%", permissionGroup.getName())
                                                .replace("%value%", args[3])
                                                .replace("%property%", args[2])
                                );
                                break;
                            case "setsuffix":
                                permissionGroup.setSuffix(args[3].replace("_", " "));
                                permissionManagement.updateGroup(permissionGroup);
                                sender.sendMessage(
                                        LanguageManager.getMessage("command-permissions-group-update-property")
                                                .replace("%group%", permissionGroup.getName())
                                                .replace("%value%", args[3])
                                                .replace("%property%", args[2])
                                );
                                break;
                            case "setprefix":

                                permissionGroup.setPrefix(args[3].replace("_", " "));
                                permissionManagement.updateGroup(permissionGroup);
                                sender.sendMessage(
                                        LanguageManager.getMessage("command-permissions-group-update-property")
                                                .replace("%group%", permissionGroup.getName())
                                                .replace("%value%", args[3])
                                                .replace("%property%", args[2])
                                );
                                break;
                            case "setdisplay":

                                permissionGroup.setDisplay(args[3].replace("_", " "));
                                permissionManagement.updateGroup(permissionGroup);
                                sender.sendMessage(
                                        LanguageManager.getMessage("command-permissions-group-update-property")
                                                .replace("%group%", permissionGroup.getName())
                                                .replace("%property%", args[2])
                                                .replace("%value%", args[3])
                                );
                                break;
                            case "setcolor":

                                String color = args[3];

                                if (color != null && color.length() == 2) {
                                    permissionGroup.setColor(args[3]);
                                    permissionManagement.updateGroup(permissionGroup);
                                    sender.sendMessage(
                                            LanguageManager.getMessage("command-permissions-group-update-property")
                                                    .replace("%group%", permissionGroup.getName())
                                                    .replace("%property%", args[2])
                                                    .replace("%value%", color.replaceAll("§", "[color]").replaceAll("&", "[color]"))
                                    );
                                }
                                break;
                        }
                    }
                    if (args.length >= 5) {
                        if (args[2].equalsIgnoreCase("add")) {
                            if (args[3].equalsIgnoreCase("group")) {
                                if (!permissionGroup.getGroups().contains(args[4])) {
                                    permissionGroup.getGroups().add(args[4]);
                                }

                                permissionManagement.updateGroup(permissionGroup);
                                sender.sendMessage(LanguageManager.getMessage("command-permissions-group-add-group-successful")
                                        .replace("%name%", args[1])
                                        .replace("%group%", args[4])
                                );
                                return;
                            }
                        }
                        if (args[2].equalsIgnoreCase("remove")) {
                            if (args[3].equalsIgnoreCase("group")) {
                                permissionGroup.getGroups().remove(args[4]);

                                permissionManagement.updateGroup(permissionGroup);
                                sender.sendMessage(LanguageManager.getMessage("command-permissions-group-remove-group-successful")
                                        .replace("%name%", args[1])
                                        .replace("%group%", args[4])
                                );
                                return;
                            }

                            if (args[3].equalsIgnoreCase("permission")) {
                                if (args.length == 6) {
                                    permissionGroup.removePermission(args[5], args[4]);
                                } else {
                                    permissionGroup.removePermission(args[4]);
                                }

                                permissionManagement.updateGroup(permissionGroup);

                                sender.sendMessage(LanguageManager.getMessage("command-permissions-group-remove-permission-successful")
                                        .replace("%name%", permissionGroup.getName())
                                        .replace("%permission%", args[4])
                                );
                                return;
                            }
                            return;
                        }
                    }

                    if (args.length >= 6 && args[3].equalsIgnoreCase("permission")) {
                        if (args[2].equalsIgnoreCase("add")) {
                            try {
                                if (!Validate.testStringParseToInt(args[5])) {
                                    return;
                                }

                                switch (args.length) {
                                    case 6:
                                        permissionGroup.addPermission(new Permission(args[4], Integer.parseInt(args[5])));
                                        break;
                                    case 7:

                                        if (Validate.testStringParseToInt(args[6])) {
                                            permissionGroup.addPermission(new Permission(args[4], Integer.parseInt(args[5]), Integer.parseInt(args[6]), TimeUnit.DAYS));
                                        } else {
                                            permissionGroup.addPermission(new Permission(args[4], Integer.parseInt(args[5])));
                                        }

                                        break;
                                    case 8:

                                        if (Validate.testStringParseToInt(args[6])) {
                                            permissionGroup.addPermission(args[7], new Permission(args[4], Integer.parseInt(args[5]), Integer.parseInt(args[6]), TimeUnit.DAYS));
                                        } else {
                                            permissionGroup.addPermission(args[7], new Permission(args[4], Integer.parseInt(args[5])));
                                        }

                                        break;
                                }

                                permissionManagement.updateGroup(permissionGroup);

                                sender.sendMessage(LanguageManager.getMessage("command-permissions-group-add-permission-successful")
                                        .replace("%name%", permissionGroup.getName())
                                        .replace("%permission%", args[4])
                                        .replace("%potency%", args[5])
                                );
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                            return;
                        }

                        if (args[2].equalsIgnoreCase("remove")) {
                            permissionGroup.removePermission(args[5], args[4]);
                            permissionManagement.updateGroup(permissionGroup);

                            sender.sendMessage(LanguageManager.getMessage("command-permissions-group-remove-permission-successful")
                                    .replace("%name%", permissionGroup.getName())
                                    .replace("%permission%", args[4])
                            );
                            return;
                        }
                    }
                } else {
                    sender.sendMessage(LanguageManager.getMessage("command-permissions-group-not-found").replace("%name%", args[1]));
                }
                break;
        }
    }

    private void displayUser(ICommandSender sender, IPermissionUser permissionUser) {
        sender.sendMessage(
                "* " + permissionUser.getUniqueId() + ":" + permissionUser.getName() + " | " + permissionUser.getPotency(),
                "Groups: "
        );

        for (PermissionUserGroupInfo groupInfo : permissionUser.getGroups()) {
            sender.sendMessage("- " + groupInfo.getGroup() + ": " + (groupInfo.getTimeOutMillis() > 0 ?
                    dateFormat.format(groupInfo.getTimeOutMillis()) : "LIFETIME"));
        }
        if (permissionUser.getGroups().isEmpty() && CloudNet.getInstance().getPermissionManagement() != null) {
            sender.sendMessage("- " + CloudNet.getInstance().getPermissionManagement().getDefaultPermissionGroup().getName() + ": LIFETIME");
        }

        sender.sendMessage(" ");
        this.displayPermissions(sender, permissionUser);
    }

    private void displayGroup(ICommandSender sender, IPermissionGroup permissionGroup) {
        sender.sendMessage(
                "* " + permissionGroup.getName() + " | " + permissionGroup.getPotency(),
                "Parent groups: " + permissionGroup.getGroups(),
                "Default: " + permissionGroup.isDefaultGroup() + " | SortId: " + permissionGroup.getSortId(),
                "Prefix: " + (sender instanceof ConsoleCommandSender ? permissionGroup.getPrefix() : permissionGroup.getPrefix().replace("&", "§")),
                "Color: " + permissionGroup.getColor().replace("&", "[color]"),
                "Suffix: " + (sender instanceof ConsoleCommandSender ? permissionGroup.getSuffix() : permissionGroup.getSuffix().replace("&", "§")),
                "Display: " + (sender instanceof ConsoleCommandSender ? permissionGroup.getDisplay() : permissionGroup.getDisplay().replace("&", "§"))
        );

        this.displayPermissions(sender, permissionGroup);
    }

    private void displayPermissions(ICommandSender sender, IPermissible permissible) {
        sender.sendMessage("Permissions: ");
        for (Permission permission : permissible.getPermissions()) {
            sender.sendMessage("- " + permission.getName() + ":" + permission.getPotency() + " | Timeout " +
                    (permission.getTimeOutMillis() > 0 ?
                            dateFormat.format(permission.getTimeOutMillis()) : "LIFETIME"));
        }

        sender.sendMessage(" ");

        for (Map.Entry<String, Collection<Permission>> groupPermissions : permissible.getGroupPermissions().entrySet()) {
            sender.sendMessage("* " + groupPermissions.getKey());

            for (Permission permission : groupPermissions.getValue()) {
                sender.sendMessage("- " + permission.getName() + ":" + permission.getPotency() + " | Timeout " +
                        (permission.getTimeOutMillis() > 0 ?
                                dateFormat.format(permission.getTimeOutMillis()) : "LIFETIME"));
            }
        }
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        if (args.length <= 1) {
            return Arrays.asList("user", "group", "users", "create", "delete");
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("users")) {
                return Collections.singletonList("group");
            }

            if (args[0].equalsIgnoreCase("create")) {
                return Arrays.asList("user", "group");
            }

            if (args[0].equalsIgnoreCase("delete")) {
                return Arrays.asList("user", "group");
            }

            if (args[0].equalsIgnoreCase("group")) {
                return this.getGroupNames();
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("delete") && args[1].equalsIgnoreCase("group")) {
                return this.getGroupNames();
            }
            if (args[0].equalsIgnoreCase("users") && args[1].equalsIgnoreCase("group")) {
                return this.getGroupNames();
            }
            if (args[0].equalsIgnoreCase("user")) {
                return Arrays.asList("password", "rename", "check", "add", "remove");
            }
            if (args[0].equalsIgnoreCase("group")) {
                return Arrays.asList("setSortId", "setDisplay", "setDefaultGroup", "setPrefix", "setSuffix", "setColor", "add", "remove");
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("user") || args[0].equalsIgnoreCase("group")) {
                if (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove")) {
                    return Arrays.asList("group", "permission");
                }
            }
        }

        if (args.length == 5) {
            if (args[0].equalsIgnoreCase("user") || args[0].equalsIgnoreCase("group")) {
                if (args[2].equalsIgnoreCase("add")) {
                    if (args[3].equalsIgnoreCase("group")) {
                        return this.getGroupNames();
                    }
                }
                if (args[2].equalsIgnoreCase("remove")) {
                    if (args[3].equalsIgnoreCase("group")) {
                        return this.getGroupNames();
                    }
                    if (args[3].equalsIgnoreCase("permission")) {
                        return this.getUserPermissions(args[1]);
                    }
                }
            }
        }

        if (args.length == 6) {
            if (args[0].equalsIgnoreCase("user") || args[0].equalsIgnoreCase("group")) {
                if (args[2].equalsIgnoreCase("remove")) {
                    if (args[3].equalsIgnoreCase("permission")) {
                        return args[0].equalsIgnoreCase("user") ?
                                this.getGroupsForUserPermission(args[1], args[4]) :
                                this.getGroupsForGroupPermission(args[1], args[4]);
                    }
                }
            }
        }

        if (args.length == 9) {
            if (args[0].equalsIgnoreCase("user") || args[0].equalsIgnoreCase("group")) {
                if (args[2].equalsIgnoreCase("add")) {
                    if (args[3].equalsIgnoreCase("permission")) {
                        return getCloudNet().getGroupConfigurations().stream().map(GroupConfiguration::getName).collect(Collectors.toList());
                    }
                }
            }
        }

        return null;
    }

    private Collection<String> getGroupNames() {
        return getCloudNet().getPermissionManagement().getGroups().stream().map(INameable::getName).collect(Collectors.toList());
    }

    private Collection<String> getUserPermissions(String user) {
        List<IPermissionUser> permissionUsers = getCloudNet().getPermissionManagement().getUser(user);
        IPermissionUser permissionUser = permissionUsers.isEmpty() ? null : permissionUsers.get(0);
        if (permissionUser != null) {
            Collection<String> outNames = new ArrayList<>(permissionUser.getPermissionNames());
            for (Collection<Permission> permissions : permissionUser.getGroupPermissions().values()) {
                permissions.stream().map(Permission::getName).forEach(outNames::add);
            }
            return outNames;
        }
        return null;
    }

    private Collection<String> getGroupsForUserPermission(String user, String permissionName) {
        List<IPermissionUser> permissionUsers = getCloudNet().getPermissionManagement().getUser(user);
        IPermissionUser permissionUser = permissionUsers.isEmpty() ? null : permissionUsers.get(0);
        return this.getGroupsForPermission(permissionUser, permissionName);
    }

    private Collection<String> getGroupsForGroupPermission(String group, String permissionName) {
        IPermissionGroup permissionGroup = getCloudNet().getPermissionManagement().getGroup(group);
        return this.getGroupsForPermission(permissionGroup, permissionName);
    }

    private Collection<String> getGroupsForPermission(IPermissible permissible, String permissionName) {
        if (permissible != null) {
            Collection<String> outGroups = new ArrayList<>();
            for (Map.Entry<String, Collection<Permission>> entry : permissible.getGroupPermissions().entrySet()) {
                if (entry.getValue().stream().anyMatch(permission -> permission.getName().equalsIgnoreCase(permissionName))) {
                    outGroups.add(entry.getKey());
                }
            }
            return outGroups;
        }
        return null;
    }

}