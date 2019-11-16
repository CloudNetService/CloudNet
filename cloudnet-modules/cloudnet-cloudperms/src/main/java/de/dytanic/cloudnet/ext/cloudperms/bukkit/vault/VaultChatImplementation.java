package de.dytanic.cloudnet.ext.cloudperms.bukkit.vault;


import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

import java.util.Optional;

public class VaultChatImplementation extends Chat {

    private IPermissionManagement permissionManagement;

    public VaultChatImplementation(Permission permission, IPermissionManagement permissionManagement) {
        super(permission);
        this.permissionManagement = permissionManagement;
    }

    private Optional<String> userPermissionGroupName(String username) {
        Optional<IPermissionUser> optionalPermissionUser = this.permissionManagement.getUsers(username).stream().findFirst();

        return optionalPermissionUser.map(permissionUser -> this.permissionManagement.getHighestPermissionGroup(permissionUser).getName());
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
    public String getPlayerPrefix(String world, String player) {
        Optional<String> optionalGroupName = this.userPermissionGroupName(player);

        return optionalGroupName.map(groupName -> this.getGroupPrefix(world, groupName)).orElse(null);
    }

    @Override
    public void setPlayerPrefix(String world, String player, String prefix) {
        Optional<String> optionalGroupName = this.userPermissionGroupName(player);

        optionalGroupName.ifPresent(groupName -> this.setGroupPrefix(world, groupName, prefix));
    }

    @Override
    public String getPlayerSuffix(String world, String player) {
        Optional<String> optionalGroupName = this.userPermissionGroupName(player);

        return optionalGroupName.map(groupName -> this.getGroupSuffix(world, groupName)).orElse(null);
    }

    @Override
    public void setPlayerSuffix(String world, String player, String suffix) {
        Optional<String> optionalGroupName = this.userPermissionGroupName(player);

        optionalGroupName.ifPresent(groupName -> this.setGroupSuffix(world, groupName, suffix));
    }

    @Override
    public String getGroupPrefix(String world, String group) {
        Optional<IPermissionGroup> optionalPermissionGroup = this.permissionGroupByName(group);

        return optionalPermissionGroup.map(IPermissionGroup::getDisplay).orElse(null);
    }

    @Override
    public void setGroupPrefix(String world, String group, String prefix) {
        Optional<IPermissionGroup> optionalPermissionGroup = this.permissionGroupByName(group);

        optionalPermissionGroup.ifPresent(permissionGroup -> permissionGroup.setDisplay(prefix));
    }

    @Override
    public String getGroupSuffix(String world, String group) {
        Optional<IPermissionGroup> optionalPermissionGroup = this.permissionGroupByName(group);

        return optionalPermissionGroup.map(IPermissionGroup::getSuffix).orElse(null);
    }

    @Override
    public void setGroupSuffix(String world, String group, String suffix) {
        Optional<IPermissionGroup> optionalPermissionGroup = this.permissionGroupByName(group);

        optionalPermissionGroup.ifPresent(permissionGroup -> permissionGroup.setSuffix(suffix));
    }

    @Override
    public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
        throw new UnsupportedOperationException("Player info not supported by " + this.getName());
    }

    @Override
    public void setPlayerInfoInteger(String world, String player, String node, int value) {
        throw new UnsupportedOperationException("Player info not supported by " + this.getName());
    }

    @Override
    public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
        throw new UnsupportedOperationException("Group info not supported by " + this.getName());
    }

    @Override
    public void setGroupInfoInteger(String world, String group, String node, int value) {
        throw new UnsupportedOperationException("Group info not supported by " + this.getName());
    }

    @Override
    public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
        throw new UnsupportedOperationException("Player info not supported by " + this.getName());
    }

    @Override
    public void setPlayerInfoDouble(String world, String player, String node, double value) {
        throw new UnsupportedOperationException("Player info not supported by " + this.getName());
    }

    @Override
    public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
        throw new UnsupportedOperationException("Group info not supported by " + this.getName());
    }

    @Override
    public void setGroupInfoDouble(String world, String group, String node, double value) {
        throw new UnsupportedOperationException("Group info not supported by " + this.getName());
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
        throw new UnsupportedOperationException("Player info not supported by " + this.getName());
    }

    @Override
    public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
        throw new UnsupportedOperationException("Player info not supported by " + this.getName());
    }

    @Override
    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
        throw new UnsupportedOperationException("Group info not supported by " + this.getName());
    }

    @Override
    public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
        throw new UnsupportedOperationException("Group info not supported by " + this.getName());
    }

    @Override
    public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
        throw new UnsupportedOperationException("Player info not supported by " + this.getName());
    }

    @Override
    public void setPlayerInfoString(String world, String player, String node, String value) {
        throw new UnsupportedOperationException("Player info not supported by " + this.getName());
    }

    @Override
    public String getGroupInfoString(String world, String group, String node, String defaultValue) {
        throw new UnsupportedOperationException("Group info not supported by " + this.getName());
    }

    @Override
    public void setGroupInfoString(String world, String group, String node, String value) {
        throw new UnsupportedOperationException("Group info not supported by " + this.getName());
    }

}
