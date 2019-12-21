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

    public NukkitCloudNetCloudPermissionsPermissible(Player player) {
        super(player);

        this.player = player;
    }

    @Override
    public Map<String, PermissionAttachmentInfo> getEffectivePermissions() {
        Map<String, PermissionAttachmentInfo> infos = new HashMap<>();
        IPermissionUser permissionUser = CloudPermissionsManagement.getInstance().getUser(player.getUniqueId());
        if (permissionUser == null) {
            return infos;
        }

        for (String group : Wrapper.getInstance().getServiceConfiguration().getGroups()) {
            infos.putAll(
                    CloudPermissionsManagement.getInstance().getAllPermissions(permissionUser, group)
                            .stream()
                            .map(permission -> new PermissionAttachmentInfo(this, permission.getName(), null, permission.getPotency() >= 0))
                            .collect(Collectors.toMap(PermissionAttachmentInfo::getPermission, o -> o))
            );
        }

        return infos;
    }

    @Override
    public boolean isPermissionSet(String name) {
        return hasPermission(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return isPermissionSet(perm.getName());
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return hasPermission(perm.getName());
    }

    @Override
    public boolean hasPermission(String inName) {
        if (inName == null) {
            return false;
        }

        IPermissionUser permissionUser = CloudPermissionsManagement.getInstance().getUser(player.getUniqueId());
        return permissionUser != null && CloudPermissionsManagement.getInstance().hasPlayerPermission(permissionUser, inName);
    }

    public Player getPlayer() {
        return this.player;
    }
}