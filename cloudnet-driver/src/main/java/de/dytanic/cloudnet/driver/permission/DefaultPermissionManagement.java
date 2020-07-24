package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.concurrent.ITask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class DefaultPermissionManagement implements IPermissionManagement {

    @Override
    public IPermissionManagement getChildPermissionManagement() {
        return null;
    }

    @Override
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

    @Override
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

    @Override
    public boolean testPermissionGroup(@Nullable IPermissionGroup permissionGroup) {
        return this.testPermissible(permissionGroup);
    }

    @Override
    public boolean testPermissionUser(@Nullable IPermissionUser permissionUser) {
        if (permissionUser == null) {
            return false;
        }

        return this.testPermissible(permissionUser) ||
                permissionUser.getGroups().removeIf(groupInfo -> groupInfo.getTimeOutMillis() > 0 && groupInfo.getTimeOutMillis() < System.currentTimeMillis());
    }

    @Override
    public boolean testPermissible(@Nullable IPermissible permissible) {
        if (permissible == null) {
            return false;
        }

        Predicate<Permission> tester = permission -> permission.getTimeOutMillis() > 0 && permission.getTimeOutMillis() < System.currentTimeMillis();

        boolean result = permissible.getPermissions().removeIf(tester);

        for (Map.Entry<String, Collection<Permission>> entry : permissible.getGroupPermissions().entrySet()) {
            result = result || entry.getValue().removeIf(tester);
        }

        return result;
    }

    @Override
    @NotNull
    public Collection<IPermissionGroup> getGroups(@Nullable IPermissible permissible) {
        Collection<IPermissionGroup> permissionGroups = new ArrayList<>();

        if (permissible == null) {
            return permissionGroups;
        }

        for (String group : permissible.getGroupNames()) {
            IPermissionGroup permissionGroup = this.getGroup(group);

            if (permissionGroup != null) {
                permissionGroups.add(permissionGroup);
            }
        }

        if (permissionGroups.isEmpty() && permissible instanceof IPermissionUser) {
            permissionGroups.add(this.getDefaultPermissionGroup());
        }

        return permissionGroups;
    }

    @Override
    public Collection<IPermissionGroup> getExtendedGroups(@Nullable IPermissionGroup group) {
        return this.getGroups(group);
    }

    @Override
    @NotNull
    public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String permission) {
        return this.getPermissionResult(permissible, new Permission(permission));
    }

    @Override
    @NotNull
    public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull Permission permission) {
        return this.getPermissionResult(permissible, () -> permissible.hasPermission(permission), permissionGroup -> this.tryExtendedGroups(permissionGroup.getName(), permissionGroup, permission, 0));
    }

    @Override
    @NotNull
    public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String group, @NotNull Permission permission) {
        return this.getPermissionResult(permissible, () -> permissible.hasPermission(permission), permissionGroup -> this.tryExtendedGroups(permissionGroup.getName(), permissionGroup, group, permission, 0));
    }

    public PermissionCheckResult getPermissionResult(IPermissible permissible, Supplier<PermissionCheckResult> permissionTester, Function<IPermissionGroup, PermissionCheckResult> extendedGroupsTester) {
        switch (permissionTester.get()) {
            case ALLOWED:
                return PermissionCheckResult.ALLOWED;
            case FORBIDDEN:
                return PermissionCheckResult.FORBIDDEN;
            default:
                for (IPermissionGroup permissionGroup : this.getGroups(permissible)) {
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
                for (IPermissionGroup extended : this.getGroups(permissionGroup)) {
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

    @Override
    public Collection<Permission> getAllPermissions(@NotNull IPermissible permissible) {
        return this.getAllPermissions(permissible, null);
    }

    @Override
    public Collection<Permission> getAllPermissions(@NotNull IPermissible permissible, String group) {
        if (permissible == null) {
            return Collections.emptyList();
        }

        Collection<Permission> permissions = new ArrayList<>(permissible.getPermissions());
        if (group != null && permissible.getGroupPermissions().containsKey(group)) {
            permissions.addAll(permissible.getGroupPermissions().get(group));
        }
        for (IPermissionGroup permissionGroup : this.getGroups(permissible)) {
            permissions.addAll(this.getAllPermissions(permissionGroup, group));
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
