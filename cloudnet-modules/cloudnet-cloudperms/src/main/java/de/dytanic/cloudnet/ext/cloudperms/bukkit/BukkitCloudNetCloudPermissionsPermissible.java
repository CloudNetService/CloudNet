package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;

import java.util.Arrays;
import java.util.Collection;

@Getter
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
        if (inName == null) return false;

        if (DEFAULT_ALLOWED_PERMISSION_COLLECTION.contains(inName.toLowerCase())) return true;

        try {
            IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(player.getUniqueId());
            return permissionUser != null && CloudPermissionsPermissionManagement.getInstance().hasPlayerPermission(permissionUser, inName);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}