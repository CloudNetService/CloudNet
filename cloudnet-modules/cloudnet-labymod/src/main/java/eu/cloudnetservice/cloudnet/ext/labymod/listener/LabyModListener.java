package eu.cloudnetservice.cloudnet.ext.labymod.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.EventPriority;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;

public class LabyModListener {

  private final AbstractLabyModManagement labyModManagement;

  public LabyModListener(AbstractLabyModManagement labyModManagement) {
    this.labyModManagement = labyModManagement;
  }

  @EventListener(priority = EventPriority.HIGHEST)
  public void handle(CloudServiceInfoUpdateEvent event) {
    ServiceInfoSnapshot newServiceInfoSnapshot = event.getServiceInfo();
    ServiceInfoSnapshot oldServiceInfoSnapshot = BridgeProxyHelper
      .getCachedServiceInfoSnapshot(newServiceInfoSnapshot.getServiceId().getName());
    if (oldServiceInfoSnapshot == null) {
      return;
    }

    if (LabyModUtils.canSpectate(newServiceInfoSnapshot) &&
      !oldServiceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false)) {
      newServiceInfoSnapshot.getProperty(BridgeServiceProperty.PLAYERS).ifPresent(players -> {
        for (ServicePlayer player : players) {
          ICloudPlayer cloudPlayer = this.labyModManagement.getPlayerManager().getOnlinePlayer(player.getUniqueId());
          if (cloudPlayer != null &&
            cloudPlayer.getLoginService().getServiceId().getUniqueId()
              .equals(Wrapper.getInstance().getServiceId().getUniqueId()) &&
            LabyModUtils.getLabyModOptions(cloudPlayer) != null) {
            this.labyModManagement.sendServerUpdate(player.getUniqueId(), cloudPlayer, newServiceInfoSnapshot);
          }
        }
      });
    }
  }

}
