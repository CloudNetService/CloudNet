package de.dytanic.cloudnet.ext.bridge.sponge.listener;

import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.sponge.SpongeCloudNetHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

public final class SpongePlayerListener {

  @Listener
  public void handle(ClientConnectionEvent.Join event) {
    BridgeHelper.sendChannelMessageServerLoginSuccess(SpongeCloudNetHelper
            .createNetworkConnectionInfo(event.getTargetEntity()),
        SpongeCloudNetHelper
            .createNetworkPlayerServerInfo(event.getTargetEntity(), false)
    );

    BridgeHelper.updateServiceInfo();
  }

  @Listener
  public void handle(ClientConnectionEvent.Disconnect event) {
    BridgeHelper.sendChannelMessageServerDisconnect(SpongeCloudNetHelper
            .createNetworkConnectionInfo(event.getTargetEntity()),
        SpongeCloudNetHelper
            .createNetworkPlayerServerInfo(event.getTargetEntity(), false));

    Wrapper.getInstance().runTask(new Runnable() {
      @Override
      public void run() {
        BridgeHelper.updateServiceInfo();
      }
    });
  }
}