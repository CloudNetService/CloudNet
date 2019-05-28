package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.IJsonDocPropertyable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

public interface IPermissible extends INameable, IJsonDocPropertyable,
  Comparable<IPermissible> {

  void setName(String name);

  int getPotency();

  void setPotency(int potency);

  boolean addPermission(Permission permission);

  boolean addPermission(String group, Permission permission);

  boolean removePermission(String permission);

  boolean removePermission(String group, String permission);

  Collection<Permission> getPermissions();

  Map<String, Collection<Permission>> getGroupPermissions();

  /*= ------------------------------------------------------------------------ =*/

  default Permission getPermission(String name) {
    if (name == null) {
      return null;
    }

    return Iterables.first(getPermissions(), new Predicate<Permission>() {
      @Override
      public boolean test(Permission permission) {
        return permission.getName().equalsIgnoreCase(name);
      }
    });
  }

  default boolean isPermissionSet(String name) {
    return Iterables.first(getPermissions(), new Predicate<Permission>() {
      @Override
      public boolean test(Permission permission) {
        return permission.getName().equalsIgnoreCase(name);
      }
    }) != null;
  }

  default boolean addPermission(String permission) {
    return addPermission(permission, 0);
  }

  default boolean addPermission(String permission, boolean value) {
    return addPermission(new Permission(permission, value ? 1 : -1));
  }

  default boolean addPermission(String permission, int potency) {
    return addPermission(new Permission(permission, potency));
  }

  default boolean addPermission(String group, String permission) {
    return addPermission(group, permission, 0);
  }

  default boolean addPermission(String group, String permission, int potency) {
    return addPermission(group, new Permission(permission, potency));
  }

  default boolean addPermission(String group, String permission, int potency,
    long time, TimeUnit millis) {
    return addPermission(group, new Permission(permission, potency,
      (System.currentTimeMillis() + millis.toMillis(time))));
  }

  default Collection<String> getPermissionNames() {
    return Iterables.map(getPermissions(), new Function<Permission, String>() {
      @Override
      public String apply(Permission permission) {
        return permission.getName();
      }
    });
  }

  default PermissionCheckResult hasPermission(
    Collection<Permission> permissions, Permission permission) {
    if (permissions == null || permission == null
      || permission.getName() == null) {
      return PermissionCheckResult.DENIED;
    }

    Permission targetPerms = Iterables
      .first(permissions, new Predicate<Permission>() {
        @Override
        public boolean test(Permission perm) {
          return perm.getName().equalsIgnoreCase(permission.getName());
        }
      });

    if (targetPerms != null && permission.getName()
      .equalsIgnoreCase(targetPerms.getName())
      && targetPerms.getPotency() < 0) {
      return PermissionCheckResult.FORBIDDEN;
    }

    for (Permission permissionEntry : permissions) {

      if (permissionEntry.getName().equals("*") && (
        permissionEntry.getPotency() >= permission.getPotency()
          || getPotency() >= permission.getPotency())) {
        return PermissionCheckResult.ALLOWED;
      }

      if (permissionEntry.getName().endsWith("*") && permission.getName()
        .contains(permissionEntry.getName().replace("*", ""))
        && (permissionEntry.getPotency() >= permission.getPotency()
        || getPotency() >= permission.getPotency())) {
        return PermissionCheckResult.ALLOWED;
      }

      if (permission.getName().equalsIgnoreCase(permissionEntry.getName()) &&
        (permissionEntry.getPotency() >= permission.getPotency()
          || getPotency() >= permission.getPotency())) {
        return PermissionCheckResult.ALLOWED;
      }
    }

    return PermissionCheckResult.DENIED;
  }

  default PermissionCheckResult hasPermission(String group,
    Permission permission) {
    if (group == null || permission == null) {
      return PermissionCheckResult.DENIED;
    }

    return getGroupPermissions().containsKey(group) ? hasPermission(
      getGroupPermissions().get(group), permission)
      : PermissionCheckResult.DENIED;
  }

  default PermissionCheckResult hasPermission(Permission permission) {
    return hasPermission(getPermissions(), permission);
  }

  default PermissionCheckResult hasPermission(String permission) {
    return hasPermission(new Permission(permission, 0));
  }

  @Override
  default int compareTo(IPermissible o) {
    return getPotency() + o.getPotency();
  }
}