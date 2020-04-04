package de.dytanic.cloudnet.driver.permission;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public interface DefaultPermissionManagement extends IPermissionManagement {

    default IPermissionManagement getChildPermissionManagement() {
        return null;
    }

    default boolean canBeOverwritten() {
        return true;
    }

    default IPermissionUser getFirstUser(String name) {
        List<IPermissionUser> users = this.getUsers(name);
        return users.isEmpty() ? null : users.get(0);
    }

    @Deprecated
    default List<IPermissionUser> getUser(String name) {
        return this.getUsers(name);
    }

    @Deprecated
    default Collection<IPermissionUser> getUserByGroup(String group) {
        return this.getUsersByGroup(group);
    }

    default IPermissionGroup getHighestPermissionGroup(@NotNull IPermissionUser permissionUser) {
        IPermissionGroup permissionGroup = null;

        for (IPermissionGroup group : getGroups(permissionUser)) {
            if (permissionGroup == null) {
                permissionGroup = group;
                continue;
            }

            if (permissionGroup.getPotency() <= group.getPotency()) {
                permissionGroup = group;
            }
        }

        return permissionGroup != null ? permissionGroup : this.getDefaultPermissionGroup();
    }

    default IPermissionGroup getDefaultPermissionGroup() {
        for (IPermissionGroup group : getGroups()) {
            if (group != null && group.isDefaultGroup()) {
                return group;
            }
        }

        return null;
    }

    default boolean testPermissionGroup(@NotNull IPermissionGroup permissionGroup) {
        if (permissionGroup == null) {
            return false;
        }

        return testPermissible(permissionGroup);
    }

    default boolean testPermissionUser(@NotNull IPermissionUser permissionUser) {
        if (permissionUser == null) {
            return false;
        }

        boolean result = testPermissible(permissionUser);

        List<PermissionUserGroupInfo> groupsToRemove = new ArrayList<>();

        for (PermissionUserGroupInfo groupInfo : permissionUser.getGroups()) {
            if (groupInfo.getTimeOutMillis() > 0 && groupInfo.getTimeOutMillis() < System.currentTimeMillis()) {
                groupsToRemove.add(groupInfo);
                result = true;
            }
        }

        for (PermissionUserGroupInfo groupInfo : groupsToRemove) {
            permissionUser.getGroups().remove(groupInfo);
        }

        return result;
    }

    default boolean testPermissible(@NotNull IPermissible permissible) {
        if (permissible == null) {
            return false;
        }

        boolean result = false;

        Collection<String> haveToRemove = new ArrayList<>();

        for (Permission permission : permissible.getPermissions()) {
            if (permission.getTimeOutMillis() > 0 && permission.getTimeOutMillis() < System.currentTimeMillis()) {
                haveToRemove.add(permission.getName());
            }
        }

        if (!haveToRemove.isEmpty()) {
            result = true;

            for (String permission : haveToRemove) {
                permissible.removePermission(permission);
            }
            haveToRemove.clear();
        }

        for (Map.Entry<String, Collection<Permission>> entry : permissible.getGroupPermissions().entrySet()) {
            for (Permission permission : entry.getValue()) {
                if (permission.getTimeOutMillis() > 0 && permission.getTimeOutMillis() < System.currentTimeMillis()) {
                    haveToRemove.add(permission.getName());
                }
            }

            if (!haveToRemove.isEmpty()) {
                result = true;

                for (String permission : haveToRemove) {
                    permissible.removePermission(entry.getKey(), permission);
                }
                haveToRemove.clear();
            }
        }

        return result;
    }

    default IPermissionUser addUser(@NotNull String name, @NotNull String password, int potency) {
        return this.addUser(new PermissionUser(UUID.randomUUID(), name, password, potency));
    }

    default IPermissionGroup addGroup(@NotNull String role, int potency) {
        return this.addGroup(new PermissionGroup(role, potency));
    }

    default Collection<IPermissionGroup> getGroups(@NotNull IPermissionUser permissionUser) {
        Collection<IPermissionGroup> permissionGroups = new ArrayList<>();

        if (permissionUser == null) {
            return permissionGroups;
        }

        for (PermissionUserGroupInfo groupInfo : permissionUser.getGroups()) {
            IPermissionGroup permissionGroup = getGroup(groupInfo.getGroup());

            if (permissionGroup != null) {
                permissionGroups.add(permissionGroup);
            }
        }

        if (permissionGroups.isEmpty()) {
            permissionGroups.add(this.getDefaultPermissionGroup());
        }

        return permissionGroups;
    }

    default Collection<IPermissionGroup> getExtendedGroups(@NotNull IPermissionGroup group) {
        return group == null ?
                Collections.emptyList() :
                this.getGroups().stream().filter(permissionGroup -> group.getGroups().contains(permissionGroup.getName())).collect(Collectors.toList());
    }

    default boolean hasPermission(@NotNull IPermissionUser permissionUser, @NotNull String permission) {
        return this.hasPermission(permissionUser, new Permission(permission));
    }

    default boolean hasPermission(@NotNull IPermissionUser permissionUser, @NotNull Permission permission) {
        if (permissionUser == null || permission == null) {
            return false;
        }

        switch (permissionUser.hasPermission(permission)) {
            case ALLOWED:
                return true;
            case FORBIDDEN:
                return false;
            default:
                for (IPermissionGroup permissionGroup : getGroups(permissionUser)) {
                    if (tryExtendedGroups(permissionGroup, permission)) {
                        return true;
                    }
                }
                break;
        }

        return tryExtendedGroups(getDefaultPermissionGroup(), permission);
    }

    default boolean hasPermission(@NotNull IPermissionUser permissionUser, @NotNull String group, @NotNull Permission permission) {
        if (permissionUser == null || group == null || permission == null) {
            return false;
        }

        switch (permissionUser.hasPermission(group, permission)) {
            case ALLOWED:
                return true;
            case FORBIDDEN:
                return false;
            default:
                for (IPermissionGroup permissionGroup : getGroups(permissionUser)) {
                    if (tryExtendedGroups(permissionGroup, group, permission)) {
                        return true;
                    }
                }
                break;
        }

        return tryExtendedGroups(getDefaultPermissionGroup(), group, permission);
    }

    default boolean tryExtendedGroups(@NotNull IPermissionGroup permissionGroup, @NotNull Permission permission) {
        if (permissionGroup == null) {
            return false;
        }

        switch (permissionGroup.hasPermission(permission)) {
            case ALLOWED:
                return true;
            case FORBIDDEN:
                return false;
            default:
                for (IPermissionGroup extended : getExtendedGroups(permissionGroup)) {
                    if (tryExtendedGroups(extended, permission)) {
                        return true;
                    }
                }
                break;
        }

        return false;
    }

    default boolean tryExtendedGroups(@NotNull IPermissionGroup permissionGroup, @NotNull String group, @NotNull Permission permission) {
        if (permissionGroup == null) {
            return false;
        }

        switch (permissionGroup.hasPermission(group, permission)) {
            case ALLOWED:
                return true;
            case FORBIDDEN:
                return false;
            default:
                for (IPermissionGroup extended : getExtendedGroups(permissionGroup)) {
                    if (tryExtendedGroups(extended, group, permission)) {
                        return true;
                    }
                }
                break;
        }

        return false;
    }

    default Collection<Permission> getAllPermissions(@NotNull IPermissible permissible) {
        return this.getAllPermissions(permissible, null);
    }

    default Collection<Permission> getAllPermissions(@NotNull IPermissible permissible, String group) {
        if (permissible == null) {
            return Collections.emptyList();
        }

        Collection<Permission> permissions = new ArrayList<>(permissible.getPermissions());
        if (group != null && permissible.getGroupPermissions().containsKey(group)) {
            permissions.addAll(permissible.getGroupPermissions().get(group));
        }
        if (permissible instanceof IPermissionGroup) {
            for (IPermissionGroup extendedGroup : this.getExtendedGroups((IPermissionGroup) permissible)) {
                permissions.addAll(this.getAllPermissions(extendedGroup, group));
            }
        }
        if (permissible instanceof IPermissionUser) {
            for (IPermissionGroup permissionGroup : this.getGroups((IPermissionUser) permissible)) {
                permissions.addAll(this.getAllPermissions(permissionGroup, group));
            }
        }
        return permissions;
    }

}
