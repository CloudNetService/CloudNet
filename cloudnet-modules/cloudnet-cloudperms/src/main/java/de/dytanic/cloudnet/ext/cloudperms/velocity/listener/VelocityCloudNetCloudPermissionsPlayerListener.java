package de.dytanic.cloudnet.ext.cloudperms.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.proxy.Player;
import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;

public final class VelocityCloudNetCloudPermissionsPlayerListener {

    private final CachedPermissionManagement permissionsManagement;

    private final PermissionProvider permissionProvider;

    public VelocityCloudNetCloudPermissionsPlayerListener(CachedPermissionManagement permissionsManagement, PermissionProvider permissionProvider) {
        this.permissionsManagement = permissionsManagement;
        this.permissionProvider = permissionProvider;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void handle(LoginEvent event) {
        CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, event.getPlayer().getUniqueId(), event.getPlayer().getUsername(), message ->
                event.setResult(ResultedEvent.ComponentResult.denied(LegacyComponentSerializer.legacyLinking().deserialize(message.replace("&", "§")))));
    }

    @Subscribe
    public void handle(PermissionsSetupEvent event) {
        if (event.getSubject() instanceof Player) {
            event.setProvider(this.permissionProvider);
        }
    }

    @Subscribe
    public void handle(DisconnectEvent event) {
        this.permissionsManagement.getCachedPermissionUsers().remove(event.getPlayer().getUniqueId());
    }

}