package de.dytanic.cloudnet.ext.cloudperms.nukkit;

import cn.nukkit.Player;
import cn.nukkit.permission.PermissibleBase;
import cn.nukkit.permission.Permission;
import cn.nukkit.permission.PermissionAttachmentInfo;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class NukkitCloudNetCloudPermissionsPermissible extends PermissibleBase {

    private final Player player;
    private final CloudPermissionsManagement permissionsManagement;

    public NukkitCloudNetCloudPermissionsPermissible(Player player, CloudPermissionsManagement permissionsManagement) {
        super(player);

        this.player = player;
        this.permissionsManagement = permissionsManagement;
    }

    @Override
    public Map<String, PermissionAttachmentInfo> getEffectivePermissions() {
        Map<String, PermissionAttachmentInfo> infos = new HashMap<>();
        IPermissionUser permissionUser = this.permissionsManagement.getUser(this.player.getUniqueId());
        if (permissionUser == null) {
            return infos;
        }

        for (String group : Wrapper.getInstance().getServiceConfiguration().getGroups()) {
            infos.putAll(
                    this.permissionsManagement.getAllPermissions(permissionUser, group)
                            .stream()
                            .map(permission -> new PermissionAttachmentInfo(this, permission.getName(), null, permission.getPotency() >= 0))
                            .collect(Collectors.toMap(PermissionAttachmentInfo::getPermission, o -> o))
            );
        }

        return infos;
    }

    @Override
    public boolean isPermissionSet(String name) {
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
    public boolean hasPermission(String inName) {
        if (inName == null) {
            return false;
        }

        IPermissionUser permissionUser = this.permissionsManagement.getUser(this.player.getUniqueId());
        return permissionUser != null && this.permissionsManagement.hasPlayerPermission(permissionUser, inName);
    }

    public Player getPlayer() {
        return this.player;
    }
}