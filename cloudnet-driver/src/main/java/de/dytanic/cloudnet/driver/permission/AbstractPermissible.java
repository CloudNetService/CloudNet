package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractPermissible extends BasicJsonDocPropertyable implements IPermissible {

    protected final long createdTime;
    protected String name;
    protected int potency;
    protected List<Permission> permissions;

    protected Map<String, Collection<Permission>> groupPermissions;

    public AbstractPermissible() {
        this.createdTime = System.currentTimeMillis();
        this.permissions = Iterables.newArrayList();
        this.groupPermissions = Maps.newHashMap();
    }

    @Override
    public boolean addPermission(Permission permission) {
        if (permission == null || permission.getName() == null) {
            return false;
        }

        Permission exist = this.getPermission(permission.getName());

        if (exist != null) {
            this.permissions.remove(exist);
        }

        this.permissions.add(permission);

        return true;
    }

    @Override
    public boolean addPermission(String group, Permission permission) {
        if (group == null || permission == null) {
            return false;
        }

        if (!groupPermissions.containsKey(group)) {
            groupPermissions.put(group, Iterables.newArrayList());
        }

        groupPermissions.get(group).add(permission);
        return true;
    }

    @Override
    public boolean removePermission(String permission) {
        if (permission == null) {
            return false;
        }

        Permission exist = this.getPermission(permission);

        if (exist != null) {
            return this.permissions.remove(exist);
        } else {
            return false;
        }
    }

    @Override
    public boolean removePermission(String group, String permission) {
        if (group == null || permission == null) {
            return false;
        }

        if (groupPermissions.containsKey(group)) {
            Permission p = Iterables.first(groupPermissions.get(group), perm -> perm.getName().equalsIgnoreCase(permission));

            if (p != null) {
                groupPermissions.get(group).remove(p);
            }

            if (groupPermissions.get(group).isEmpty()) {
                groupPermissions.remove(group);
            }
        }

        return true;
    }

    public long getCreatedTime() {
        return this.createdTime;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPotency() {
        return this.potency;
    }

    public void setPotency(int potency) {
        this.potency = potency;
    }

    public List<Permission> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public Map<String, Collection<Permission>> getGroupPermissions() {
        return this.groupPermissions;
    }
}