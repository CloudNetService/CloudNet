package de.dytanic.cloudnet.driver.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public interface DefaultPermissionManagement extends IPermissionManagement {

    default IPermissionManagement getChildPermissionManagement() {
        return null;
    }

    default boolean canBeOverwritten() {
        return true;
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

    default boolean testPermissionGroup(@Nullable IPermissionGroup permissionGroup) {
        if (permissionGroup == null) {
            return false;
        }

        return testPermissible(permissionGroup);
    }

    default boolean testPermissionUser(@Nullable IPermissionUser permissionUser) {
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

    default boolean testPermissible(@Nullable IPermissible permissible) {
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

    default Collection<IPermissionGroup> getGroups(@Nullable IPermissionUser permissionUser) {
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

    default Collection<IPermissionGroup> getExtendedGroups(@Nullable IPermissionGroup group) {
        return group == null ?
                Collections.emptyList() :
                this.getGroups().stream().filter(permissionGroup -> group.getGroups().contains(permissionGroup.getName())).collect(Collectors.toList());
    }

    @NotNull
    default PermissionCheckResult getPermissionResult(@NotNull IPermissionUser permissionUser, @NotNull String permission) {
        return this.getPermissionResult(permissionUser, new Permission(permission));
    }

    @NotNull
    default PermissionCheckResult getPermissionResult(@NotNull IPermissionUser permissionUser, @NotNull Permission permission) {
        switch (permissionUser.hasPermission(permission)) {
            case ALLOWED:
                return PermissionCheckResult.ALLOWED;
            case FORBIDDEN:
                return PermissionCheckResult.FORBIDDEN;
            default:
                for (IPermissionGroup permissionGroup : getGroups(permissionUser)) {
                    if (permissionGroup != null) {
                        PermissionCheckResult result = this.tryExtendedGroups(permissionGroup.getName(), permissionGroup, permission, 0);
                        if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
                            return result;
                        }
                    }
                }
                break;
        }

        IPermissionGroup defaultGroup = this.getDefaultPermissionGroup();
        return defaultGroup != null ? tryExtendedGroups(defaultGroup.getName(), defaultGroup, permission, 0) : PermissionCheckResult.DENIED;
    }

    @NotNull
    default PermissionCheckResult getPermissionResult(@NotNull IPermissionUser permissionUser, @NotNull String group, @NotNull Permission permission) {
        switch (permissionUser.hasPermission(group, permission)) {
            case ALLOWED:
                return PermissionCheckResult.ALLOWED;
            case FORBIDDEN:
                return PermissionCheckResult.FORBIDDEN;
            default:
                for (IPermissionGroup permissionGroup : getGroups(permissionUser)) {
                    if (permissionGroup != null) {
                        PermissionCheckResult result = tryExtendedGroups(permissionGroup.getName(), permissionGroup, group, permission, 0);
                        if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
                            return result;
                        }
                    }
                }
                break;
        }

        IPermissionGroup defaultGroup = this.getDefaultPermissionGroup();
        return defaultGroup != null ? tryExtendedGroups(defaultGroup.getName(), defaultGroup, group, permission, 0) : PermissionCheckResult.DENIED;
    }

    default PermissionCheckResult tryExtendedGroups(@NotNull String firstGroup, @Nullable IPermissionGroup permissionGroup, @NotNull Permission permission, int layer) {
        if (permissionGroup == null) {
            return PermissionCheckResult.DENIED;
        }
        if (layer >= 30) {
            System.err.println("Detected recursive permission group implementation on group " + firstGroup);
            return PermissionCheckResult.DENIED;
        }
        layer++;

        switch (permissionGroup.hasPermission(permission)) {
            case ALLOWED:
                return PermissionCheckResult.ALLOWED;
            case FORBIDDEN:
                return PermissionCheckResult.FORBIDDEN;
            default:
                for (IPermissionGroup extended : getExtendedGroups(permissionGroup)) {
                    PermissionCheckResult result = this.tryExtendedGroups(firstGroup, extended, permission, layer);
                    if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
                        return result;
                    }
                }
                break;
        }

        return PermissionCheckResult.DENIED;
    }

    default PermissionCheckResult tryExtendedGroups(@NotNull String firstGroup, @Nullable IPermissionGroup permissionGroup, @NotNull String group, @NotNull Permission permission, int layer) {
        if (permissionGroup == null) {
            return PermissionCheckResult.DENIED;
        }
        if (layer >= 30) {
            System.err.println("Detected recursive permission group implementation on group " + firstGroup);
            return PermissionCheckResult.DENIED;
        }
        layer++;

        switch (permissionGroup.hasPermission(group, permission)) {
            case ALLOWED:
                return PermissionCheckResult.ALLOWED;
            case FORBIDDEN:
                return PermissionCheckResult.FORBIDDEN;
            default:
                for (IPermissionGroup extended : getExtendedGroups(permissionGroup)) {
                    PermissionCheckResult result = tryExtendedGroups(firstGroup, extended, group, permission, layer);
                    if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
                        return result;
                    }
                }
                break;
        }

        return PermissionCheckResult.DENIED;
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
