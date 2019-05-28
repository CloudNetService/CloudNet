package de.dytanic.cloudnet.ext.cloudperms.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.velocity.listener.VelocityCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.lang.reflect.Field;
import lombok.Getter;

@Getter
@Plugin(
  id = "cloudnet_cloudperms_velocity",
  name = "CloudNet-CloudPerms",
  version = "1.0",
  description = "Velocity extension which implement the permission management system from CloudNet into Velocity for players",
  url = "https://cloudnetservice.eu",
  authors = {
    "Dytanic"
  }
)
public final class VelocityCloudNetCloudPermissionsPlugin {

  @Getter
  private static VelocityCloudNetCloudPermissionsPlugin instance;

  private final ProxyServer proxyServer;

  private final PermissionProvider permissionProvider = new VelocityCloudNetCloudPermissionsPermissionProvider();

  @Inject
  public VelocityCloudNetCloudPermissionsPlugin(ProxyServer proxyServer) {
    instance = this;

    this.proxyServer = proxyServer;
  }

  @Subscribe
  public void handleProxyInit(ProxyInitializeEvent event) {
    new CloudPermissionsPermissionManagement();
    initPlayersPermissionFunction();

    proxyServer.getEventManager()
      .register(this, new VelocityCloudNetCloudPermissionsPlayerListener());
  }

  @Subscribe
  public void handleShutdown(ProxyShutdownEvent event) {
    CloudNetDriver.getInstance().getEventManager()
      .unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(
      this.getClass().getClassLoader());
  }

  /*= -------------------------------------------------------------------------------------------------- =*/

  private void initPlayersPermissionFunction() {
    for (Player player : proxyServer.getAllPlayers()) {
      injectPermissionFunction(player);
    }
  }

  public void injectPermissionFunction(Player player) {
    Validate.checkNotNull(player);

    try {

      Field field = player.getClass().getDeclaredField("permissionFunction");
      field.setAccessible(true);
      field.set(player, new VelocityCloudNetCloudPermissionsPermissionFunction(
        player.getUniqueId()));

    } catch (Exception ignored) {

    }
  }
}