package de.dytanic.cloudnet.ext.cloudperms.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.proxy.Player;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import net.kyori.text.Component;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;

public final class VelocityCloudNetCloudPermissionsPlayerListener {

    private final IPermissionManagement permissionsManagement;
    private final PermissionProvider permissionProvider;

    public VelocityCloudNetCloudPermissionsPlayerListener(IPermissionManagement permissionsManagement, PermissionProvider permissionProvider) {
        this.permissionsManagement = permissionsManagement;
        this.permissionProvider = permissionProvider;
    }

    @Subscribe(order = PostOrder.LAST)
    public void handle(LoginEvent event) {
        if (event.getResult().isAllowed()) {
            Player player = event.getPlayer();
            CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, player.getUniqueId(), player.getUsername(), message -> {
                Component reasonComponent = LegacyComponentSerializer.legacyLinking().deserialize(message.replace("&", "§"));
                event.setResult(ResultedEvent.ComponentResult.denied(reasonComponent));
            });
        }
    }

    @Subscribe
    public void handle(PermissionsSetupEvent event) {
        if (event.getSubject() instanceof Player) {
            event.setProvider(this.permissionProvider);
        }
    }

    @Subscribe
    public void handle(DisconnectEvent event) {
        CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUniqueId());
    }
}
