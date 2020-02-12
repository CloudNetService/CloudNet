package de.dytanic.cloudnet.ext.cloudperms.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.velocity.listener.VelocityCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.lang.reflect.Field;

@Plugin(id = "cloudnet_cloudperms_velocity")
public final class VelocityCloudNetCloudPermissionsPlugin {

    private static VelocityCloudNetCloudPermissionsPlugin instance;

    private final ProxyServer proxyServer;

    private final PermissionProvider permissionProvider = new VelocityCloudNetCloudPermissionsPermissionProvider();

    @Inject
    public VelocityCloudNetCloudPermissionsPlugin(ProxyServer proxyServer) {
        instance = this;

        this.proxyServer = proxyServer;
    }

    public static VelocityCloudNetCloudPermissionsPlugin getInstance() {
        return VelocityCloudNetCloudPermissionsPlugin.instance;
    }

    @Subscribe
    public void handleProxyInit(ProxyInitializeEvent event) {
        CloudPermissionsManagement.getInstance();
        initPlayersPermissionFunction();

        proxyServer.getEventManager().register(this, new VelocityCloudNetCloudPermissionsPlayerListener());
    }

    @Subscribe
    public void handleShutdown(ProxyShutdownEvent event) {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }


    private void initPlayersPermissionFunction() {
        for (Player player : proxyServer.getAllPlayers()) {
            injectPermissionFunction(player);
        }
    }

    public void injectPermissionFunction(Player player) {
        Preconditions.checkNotNull(player);

        try {

            Field field = player.getClass().getDeclaredField("permissionFunction");
            field.setAccessible(true);
            field.set(player, new VelocityCloudNetCloudPermissionsPermissionFunction(player.getUniqueId()));

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public ProxyServer getProxyServer() {
        return this.proxyServer;
    }

    public PermissionProvider getPermissionProvider() {
        return this.permissionProvider;
    }
}