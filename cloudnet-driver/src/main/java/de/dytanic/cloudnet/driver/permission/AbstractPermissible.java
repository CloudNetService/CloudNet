package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class AbstractPermissible extends BasicJsonDocPropertyable implements IPermissible {

    protected final long createdTime;
    protected String name;
    protected int potency;
    protected List<Permission> permissions;

    protected Map<String, Collection<Permission>> groupPermissions;

    public AbstractPermissible() {
        this.createdTime = System.currentTimeMillis();
        this.permissions = new ArrayList<>();
        this.groupPermissions = new HashMap<>();
    }

    private boolean addPermission(Collection<Permission> permissions, Permission permission) {
        if (permission == null) {
            return false;
        }

        permissions.removeIf(existingPermission -> existingPermission.getName().equalsIgnoreCase(permission.getName()));
        permissions.add(permission);

        return true;
    }

    @Override
    public boolean addPermission(@NotNull Permission permission) {
        return this.addPermission(this.permissions, permission);
    }

    @Override
    public boolean addPermission(@NotNull String group, @NotNull Permission permission) {
        return this.addPermission(this.groupPermissions.computeIfAbsent(group, s -> new ArrayList<>()), permission);
    }

    @Override
    public boolean removePermission(@NotNull String permission) {
        Permission exist = this.getPermission(permission);

        if (exist != null) {
            return this.permissions.remove(exist);
        } else {
            return false;
        }
    }

    @Override
    public boolean removePermission(@NotNull String group, @NotNull String permission) {
        if (groupPermissions.containsKey(group)) {
            Optional<Permission> optionalPermission = groupPermissions.get(group).stream().filter(perm -> perm.getName().equalsIgnoreCase(permission)).findFirst();
            if (optionalPermission.isPresent()) {
                groupPermissions.get(group).remove(optionalPermission.get());
                if (groupPermissions.get(group).isEmpty()) {
                    groupPermissions.remove(group);
                }
                return true;
            }
        }

        return false;
    }

    public long getCreatedTime() {
        return this.createdTime;
    }

    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String name) {
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