package de.dytanic.cloudnet.ext.cloudperms.bukkit.vault;


import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUserGroupInfo;
import net.milkbowl.vault.permission.Permission;

import java.util.Optional;

public class VaultPermissionImplementation extends Permission {

    private final IPermissionManagement permissionManagement;

    public VaultPermissionImplementation(IPermissionManagement permissionManagement) {
        this.permissionManagement = permissionManagement;
    }

    private Optional<IPermissionUser> permissionUserByName(String name) {
        return this.permissionManagement.getUsers(name).stream().findFirst();
    }

    private Optional<IPermissionGroup> permissionGroupByName(String name) {
        return Optional.ofNullable(this.permissionManagement.getGroup(name));
    }

    @Override
    public String getName() {
        return "CloudNet-CloudPerms";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

        return optionalPermissionUser.isPresent() && optionalPermissionUser.get().hasPermission(permission).asBoolean();
    }

    @Override
    public boolean playerAdd(String world, String player, String permission) {
        Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

        return optionalPermissionUser.map(permissionUser -> permissionUser.addPermission(permission)).orElse(false);
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

        return optionalPermissionUser.map(permissionUser -> permissionUser.removePermission(permission)).orElse(false);

    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
        Optional<IPermissionGroup> optionalPermissionGroup = this.permissionGroupByName(group);

        return optionalPermissionGroup.isPresent() && optionalPermissionGroup.get().hasPermission(permission).asBoolean();
    }

    @Override
    public boolean groupAdd(String world, String group, String permission) {
        Optional<IPermissionGroup> optionalPermissionGroup = this.permissionGroupByName(group);

        return optionalPermissionGroup.map(permissionGroup -> permissionGroup.addPermission(permission)).orElse(false);
    }

    @Override
    public boolean groupRemove(String world, String group, String permission) {
        Optional<IPermissionGroup> optionalPermissionGroup = this.permissionGroupByName(group);

        return optionalPermissionGroup.map(permissionGroup -> permissionGroup.removePermission(permission)).orElse(false);
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

        return optionalPermissionUser.isPresent() && optionalPermissionUser.get().inGroup(group);
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

        return optionalPermissionUser.isPresent() && optionalPermissionUser.get().addGroup(group) != null;
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

        return optionalPermissionUser.isPresent() && optionalPermissionUser.get().removeGroup(group) != null;
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

        return optionalPermissionUser.map(permissionUser ->
                permissionUser.getGroups().stream().map(PermissionUserGroupInfo::getGroup).toArray(String[]::new)).orElse(new String[0]);
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        Optional<IPermissionUser> optionalPermissionUser = this.permissionUserByName(player);

        return optionalPermissionUser.map(permissionUser ->
                this.permissionManagement.getHighestPermissionGroup(permissionUser).getName()).orElse(null);
    }

    @Override
    public String[] getGroups() {
        return this.permissionManagement.getGroups().stream().map(INameable::getName).toArray(String[]::new);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

}
