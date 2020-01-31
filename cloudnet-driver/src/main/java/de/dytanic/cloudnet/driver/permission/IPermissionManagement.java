package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.collection.Iterables;

import java.util.*;

public interface IPermissionManagement {

    IPermissionManagementHandler getPermissionManagementHandler();

    void setPermissionManagementHandler(IPermissionManagementHandler permissionManagementHandler);

    IPermissionUser addUser(IPermissionUser permissionUser);

    void updateUser(IPermissionUser permissionUser);

    void deleteUser(String name);

    void deleteUser(IPermissionUser permissionUser);

    boolean containsUser(UUID uniqueId);

    boolean containsUser(String name);

    IPermissionUser getUser(UUID uniqueId);

    /**
     * @deprecated use {@link #getUsers(String)} instead
     */
    @Deprecated
    default List<IPermissionUser> getUser(String name) {
        return this.getUsers(name);
    }

    List<IPermissionUser> getUsers(String name);

    default IPermissionUser getFirstUser(String name) {
        List<IPermissionUser> users = this.getUsers(name);
        return users.isEmpty() ? null : users.get(0);
    }


    Collection<IPermissionUser> getUsers();

    void setUsers(Collection<? extends IPermissionUser> users);

    /**
     * @deprecated use {@link #getUsersByGroup(String)} instead
     */
    @Deprecated
    default Collection<IPermissionUser> getUserByGroup(String group) {
        return this.getUsersByGroup(group);
    }

    Collection<IPermissionUser> getUsersByGroup(String group);

    IPermissionGroup addGroup(IPermissionGroup permissionGroup);

    void updateGroup(IPermissionGroup permissionGroup);

    void deleteGroup(String group);

    void deleteGroup(IPermissionGroup group);

    boolean containsGroup(String name);

    IPermissionGroup getGroup(String name);

    Collection<IPermissionGroup> getGroups();

    void setGroups(Collection<? extends IPermissionGroup> groups);

    boolean reload();


    default IPermissionGroup getHighestPermissionGroup(IPermissionUser permissionUser) {
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

    default boolean testPermissionGroup(IPermissionGroup permissionGroup) {
        if (permissionGroup == null) {
            return false;
        }

        return testPermissible(permissionGroup);
    }

    default boolean testPermissionUser(IPermissionUser permissionUser) {
        if (permissionUser == null) {
            return false;
        }

        boolean result = testPermissible(permissionUser);

        List<PermissionUserGroupInfo> groupsToRemove = Iterables.newArrayList();

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

    default boolean testPermissible(IPermissible permissible) {
        if (permissible == null) {
            return false;
        }

        boolean result = false;

        Collection<String> haveToRemove = Iterables.newArrayList();

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

    default IPermissionUser addUser(String name, String password, int potency) {
        return this.addUser(new PermissionUser(UUID.randomUUID(), name, password, potency));
    }

    default IPermissionGroup addGroup(String role, int potency) {
        return this.addGroup(new PermissionGroup(role, potency));
    }

    default Collection<IPermissionGroup> getGroups(IPermissionUser permissionUser) {
        Collection<IPermissionGroup> permissionGroups = Iterables.newArrayList();

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

    default Collection<IPermissionGroup> getExtendedGroups(IPermissionGroup group) {
        return group == null ? Collections.EMPTY_LIST : Iterables.filter(this.getGroups(), permissionGroup -> group.getGroups().contains(permissionGroup.getName()));
    }

    default boolean hasPermission(IPermissionUser permissionUser, String permission) {
        return this.hasPermission(permissionUser, new Permission(permission));
    }

    default boolean hasPermission(IPermissionUser permissionUser, Permission permission) {
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

    default boolean hasPermission(IPermissionUser permissionUser, String group, Permission permission) {
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

    default boolean tryExtendedGroups(IPermissionGroup permissionGroup, Permission permission) {
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

    default boolean tryExtendedGroups(IPermissionGroup permissionGroup, String group, Permission permission) {
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

    default Collection<Permission> getAllPermissions(IPermissible permissible) {
        return this.getAllPermissions(permissible, null);
    }

    default Collection<Permission> getAllPermissions(IPermissible permissible, String group) {
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
