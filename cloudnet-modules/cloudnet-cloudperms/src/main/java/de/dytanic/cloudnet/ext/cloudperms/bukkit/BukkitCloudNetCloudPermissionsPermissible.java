package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class BukkitCloudNetCloudPermissionsPermissible extends PermissibleBase {

    private static final Collection<String> DEFAULT_ALLOWED_PERMISSION_COLLECTION = Iterables.newArrayList(Arrays.asList(
            "bukkit.broadcast.user"
    ));

    private final Player player;

    public BukkitCloudNetCloudPermissionsPermissible(Player player) {
        super(player);

        this.player = player;
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        Set<PermissionAttachmentInfo> infos = new HashSet<>();
        IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(player.getUniqueId());
        if (permissionUser == null) {
            return infos;
        }

        for (String group : Wrapper.getInstance().getServiceConfiguration().getGroups()) {
            infos.addAll(
                    CloudPermissionsPermissionManagement.getInstance().getAllPermissions(permissionUser, group)
                            .stream()
                            .map(permission -> new PermissionAttachmentInfo(this, permission.getName(), null, permission.getPotency() >= 0))
                            .collect(Collectors.toSet())
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

        if (DEFAULT_ALLOWED_PERMISSION_COLLECTION.contains(inName.toLowerCase())) {
            return true;
        }

        try {
            IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(player.getUniqueId());
            return permissionUser != null && CloudPermissionsPermissionManagement.getInstance().hasPlayerPermission(permissionUser, inName);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public Player getPlayer() {
        return this.player;
    }
}