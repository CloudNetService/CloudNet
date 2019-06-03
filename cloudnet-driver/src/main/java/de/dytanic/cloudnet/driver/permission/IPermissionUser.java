package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.collection.Iterables;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public interface IPermissionUser extends IPermissible {

    UUID getUniqueId();

    Collection<PermissionUserGroupInfo> getGroups();

    String getHashedPassword();

    void changePassword(String password);

    boolean checkPassword(String password);

    /*= ------------------------------------------------------- =*/

    default IPermissionUser addGroup(String group)
    {
        if (group == null) return this;

        return addGroup(group, 0L);
    }

    default IPermissionUser addGroup(String group, long time, TimeUnit timeUnit)
    {
        if (group == null) return this;

        return addGroup(group, (System.currentTimeMillis() + timeUnit.toMillis(time)));
    }

    default IPermissionUser addGroup(String group, long timeOutMillis)
    {
        if (group == null) return this;

        PermissionUserGroupInfo groupInfo = Iterables.first(getGroups(), new Predicate<PermissionUserGroupInfo>() {
            @Override
            public boolean test(PermissionUserGroupInfo permissionUserGroupInfo)
            {
                return permissionUserGroupInfo.getGroup().equalsIgnoreCase(group);
            }
        });

        if (groupInfo != null) removeGroup(groupInfo.getGroup());

        groupInfo = new PermissionUserGroupInfo(group, timeOutMillis);

        getGroups().add(groupInfo);
        return this;
    }

    default IPermissionUser removeGroup(String group)
    {
        if (group == null) return this;

        Collection<PermissionUserGroupInfo> groupInfo = Iterables.filter(getGroups(), new Predicate<PermissionUserGroupInfo>() {
            @Override
            public boolean test(PermissionUserGroupInfo permissionUserGroupInfo)
            {
                return permissionUserGroupInfo.getGroup().equalsIgnoreCase(group);
            }
        });

        getGroups().removeAll(groupInfo);

        return this;
    }

    default boolean inGroup(String group)
    {
        if (group == null) return false;

        return Iterables.first(getGroups(), new Predicate<PermissionUserGroupInfo>() {
            @Override
            public boolean test(PermissionUserGroupInfo permissionUserGroupInfo)
            {
                return permissionUserGroupInfo.getGroup() != null && permissionUserGroupInfo.getGroup().equalsIgnoreCase(group);
            }
        }) != null;
    }
}