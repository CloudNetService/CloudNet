package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.concurrent.ITask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class DefaultPermissionManagement implements IPermissionManagement {

    public IPermissionManagement getChildPermissionManagement() {
        return null;
    }

    public boolean canBeOverwritten() {
        return true;
    }

    @Deprecated
    public List<IPermissionUser> getUser(String name) {
        return this.getUsers(name);
    }

    @Deprecated
    public Collection<IPermissionUser> getUserByGroup(String group) {
        return this.getUsersByGroup(group);
    }

    public IPermissionGroup getHighestPermissionGroup(@NotNull IPermissionUser permissionUser) {
        IPermissionGroup permissionGroup = null;

        for (IPermissionGroup group : this.getGroups(permissionUser)) {
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

    public boolean testPermissionGroup(@Nullable IPermissionGroup permissionGroup) {
        if (permissionGroup == null) {
            return false;
        }

        return this.testPermissible(permissionGroup);
    }

    public boolean testPermissionUser(@Nullable IPermissionUser permissionUser) {
        if (permissionUser == null) {
            return false;
        }

        boolean result = this.testPermissible(permissionUser);

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

    public boolean testPermissible(@Nullable IPermissible permissible) {
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

    public Collection<IPermissionGroup> getGroups(@Nullable IPermissionUser permissionUser) {
        Collection<IPermissionGroup> permissionGroups = new ArrayList<>();

        if (permissionUser == null) {
            return permissionGroups;
        }

        for (PermissionUserGroupInfo groupInfo : permissionUser.getGroups()) {
            IPermissionGroup permissionGroup = this.getGroup(groupInfo.getGroup());

            if (permissionGroup != null) {
                permissionGroups.add(permissionGroup);
            }
        }

        if (permissionGroups.isEmpty()) {
            permissionGroups.add(this.getDefaultPermissionGroup());
        }

        return permissionGroups;
    }

    public Collection<IPermissionGroup> getExtendedGroups(@Nullable IPermissionGroup group) {
        return group == null ?
                Collections.emptyList() :
                this.getGroups().stream().filter(permissionGroup -> group.getGroups().contains(permissionGroup.getName())).collect(Collectors.toList());
    }

    @NotNull
    public PermissionCheckResult getPermissionResult(@NotNull IPermissionUser permissionUser, @NotNull String permission) {
        return this.getPermissionResult(permissionUser, new Permission(permission));
    }

    @NotNull
    public PermissionCheckResult getPermissionResult(@NotNull IPermissionUser permissionUser, @NotNull Permission permission) {
        return this.getPermissionResult(permissionUser, () -> permissionUser.hasPermission(permission), permissionGroup -> this.tryExtendedGroups(permissionGroup.getName(), permissionGroup, permission, 0));
    }

    @NotNull
    public PermissionCheckResult getPermissionResult(@NotNull IPermissionUser permissionUser, @NotNull String group, @NotNull Permission permission) {
        return this.getPermissionResult(permissionUser, () -> permissionUser.hasPermission(permission), permissionGroup -> this.tryExtendedGroups(permissionGroup.getName(), permissionGroup, group, permission, 0));
    }

    public PermissionCheckResult getPermissionResult(IPermissionUser permissionUser, Supplier<PermissionCheckResult> permissionTester, Function<IPermissionGroup, PermissionCheckResult> extendedGroupsTester) {
        switch (permissionTester.get()) {
            case ALLOWED:
                return PermissionCheckResult.ALLOWED;
            case FORBIDDEN:
                return PermissionCheckResult.FORBIDDEN;
            default:
                for (IPermissionGroup permissionGroup : this.getGroups(permissionUser)) {
                    if (permissionGroup != null) {
                        PermissionCheckResult result = extendedGroupsTester.apply(permissionGroup);
                        if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
                            return result;
                        }
                    }
                }
                break;
        }

        IPermissionGroup publicGroup = this.getDefaultPermissionGroup();
        return publicGroup != null ? extendedGroupsTester.apply(publicGroup) : PermissionCheckResult.DENIED;
    }

    public PermissionCheckResult tryExtendedGroups(@NotNull String firstGroup, @Nullable IPermissionGroup permissionGroup, @NotNull Permission permission, int layer) {
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
                for (IPermissionGroup extended : this.getExtendedGroups(permissionGroup)) {
                    PermissionCheckResult result = this.tryExtendedGroups(firstGroup, extended, permission, layer);
                    if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
                        return result;
                    }
                }
                break;
        }

        return PermissionCheckResult.DENIED;
    }

    public PermissionCheckResult tryExtendedGroups(@NotNull String firstGroup, @Nullable IPermissionGroup permissionGroup, @NotNull String group, @NotNull Permission permission, int layer) {
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
                for (IPermissionGroup extended : this.getExtendedGroups(permissionGroup)) {
                    PermissionCheckResult result = this.tryExtendedGroups(firstGroup, extended, group, permission, layer);
                    if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
                        return result;
                    }
                }
                break;
        }

        return PermissionCheckResult.DENIED;
    }

    public Collection<Permission> getAllPermissions(@NotNull IPermissible permissible) {
        return this.getAllPermissions(permissible, null);
    }

    public Collection<Permission> getAllPermissions(@NotNull IPermissible permissible, String group) {
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

    @Override
    public ITask<IPermissionGroup> modifyGroupAsync(@NotNull String name, @NotNull Consumer<IPermissionGroup> modifier) {
        ITask<IPermissionGroup> task = this.getGroupAsync(name);

        task.onComplete(group -> {
            if (group != null) {
                modifier.accept(group);
                this.updateGroup(group);
            }
        });

        return task;
    }

    @Override
    public ITask<IPermissionUser> modifyUserAsync(@NotNull UUID uniqueId, @NotNull Consumer<IPermissionUser> modifier) {
        ITask<IPermissionUser> task = this.getUserAsync(uniqueId);

        task.onComplete(user -> {
            if (user != null) {
                modifier.accept(user);
                this.updateUser(user);
            }
        });

        return task;
    }

    @Override
    public ITask<List<IPermissionUser>> modifyUsersAsync(@NotNull String name, @NotNull Consumer<IPermissionUser> modifier) {
        ITask<List<IPermissionUser>> task = this.getUsersAsync(name);

        task.onComplete(users -> {
            for (IPermissionUser user : users) {
                modifier.accept(user);
                this.updateUser(user);
            }
        });

        return task;
    }
}
