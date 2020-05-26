package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionCheckResult;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class BukkitCloudNetCloudPermissionsPermissible extends PermissibleBase {

    private final Player player;
    private final IPermissionManagement permissionsManagement;

    public BukkitCloudNetCloudPermissionsPermissible(Player player, IPermissionManagement permissionsManagement) {
        super(player);

        this.player = player;
        this.permissionsManagement = permissionsManagement;
    }

    private Set<Permission> getDefaultPermissions() {
        return this.player.getServer().getPluginManager().getDefaultPermissions(false);
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        Set<PermissionAttachmentInfo> infos = new HashSet<>();
        IPermissionUser permissionUser = CloudNetDriver.getInstance().getPermissionManagement().getUser(this.player.getUniqueId());
        if (permissionUser == null) {
            return infos;
        }

        for (String group : Wrapper.getInstance().getServiceConfiguration().getGroups()) {
            CloudNetDriver.getInstance().getPermissionManagement().getAllPermissions(permissionUser, group).forEach(permission -> {
                Permission bukkitPermission = this.player.getServer().getPluginManager().getPermission(permission.getName());
                if (bukkitPermission != null) {
                    this.forEachChildren(bukkitPermission, (name, value) -> infos.add(new PermissionAttachmentInfo(this, name, null, value)));
                } else {
                    infos.add(new PermissionAttachmentInfo(this, permission.getName(), null, permission.getPotency() >= 0));
                }
            });
        }
        for (Permission defaultPermission : this.getDefaultPermissions()) {
            this.forEachChildren(defaultPermission, (name, value) -> infos.add(new PermissionAttachmentInfo(this, name, null, value)));
        }

        return infos;
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return this.hasPermission(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return this.isPermissionSet(perm.getName());
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return this.hasPermission(perm.getName());
    }

    @Override
    public boolean hasPermission(@NotNull String inName) {
        try {
            IPermissionUser permissionUser = CloudNetDriver.getInstance().getPermissionManagement().getUser(this.player.getUniqueId());
            if (permissionUser == null) {
                return false;
            }
            if (this.checkPermission(permissionUser, inName)) {
                return true;
            }
            return this.testParents(inName, parentPermission -> this.checkPermission(permissionUser, parentPermission.getName()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean testDefaultPermission(IPermissionUser permissionUser, String name) {
        return this.permissionsManagement.getPermissionResult(permissionUser, name) != PermissionCheckResult.FORBIDDEN;
    }

    private boolean testParents(String inName, Predicate<Permission> parentAcceptor) {
        for (Permission parent : this.player.getServer().getPluginManager().getPermissions()) {
            if (this.testParents(inName, parent, null, parentAcceptor)) {
                return true;
            }
        }
        return false;
    }

    private boolean testParents(String inName, Permission parent, Permission lastParent, Predicate<Permission> parentAcceptor) {
        for (Map.Entry<String, Boolean> entry : parent.getChildren().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(inName)) {
                if (lastParent != null && parentAcceptor.test(lastParent)) {
                    return entry.getValue();
                }
                return parentAcceptor.test(parent) && entry.getValue();
            }

            Permission child = this.player.getServer().getPluginManager().getPermission(entry.getKey());
            if (child != null && this.testParents(inName, child, parent, parentAcceptor)) {
                return true;
            }
        }
        return false;
    }

    private void forEachChildren(Permission permission, BiConsumer<String, Boolean> permissionAcceptor) {
        permissionAcceptor.accept(permission.getName(), true);
        for (Map.Entry<String, Boolean> entry : permission.getChildren().entrySet()) {
            permissionAcceptor.accept(entry.getKey(), entry.getValue());
        }
    }

    private boolean checkPermission(IPermissionUser permissionUser, String name) {
        return this.getDefaultPermissions().stream().anyMatch(permission -> permission.getName().equalsIgnoreCase(name) && this.testDefaultPermission(permissionUser, name)) ||
                this.permissionsManagement.hasPermission(permissionUser, name);
    }

    public Player getPlayer() {
        return this.player;
    }
}
