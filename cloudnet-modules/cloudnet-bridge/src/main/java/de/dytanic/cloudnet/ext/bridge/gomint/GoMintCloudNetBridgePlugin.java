package de.dytanic.cloudnet.ext.bridge.gomint;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.gomint.listener.GoMintCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.gomint.listener.GoMintPlayerListener;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.GoMint;
import io.gomint.event.network.PingEvent;
import io.gomint.plugin.Plugin;
import io.gomint.plugin.PluginName;
import io.gomint.plugin.Version;
import java.util.concurrent.TimeUnit;

@PluginName("CloudNet-Bridge")
@Version(major = 1, minor = 0)
public final class GoMintCloudNetBridgePlugin extends Plugin {

  @Override
  public void onInstall() {
    GoMintCloudNetHelper.setPlugin(this);
  }

  @Override
  public void onStartup() {
    this.initListeners();

    BridgeHelper.updateServiceInfo();
    getScheduler()
      .schedule(Wrapper.getInstance()::publishServiceInfoUpdate, 1000, 1500,
        TimeUnit.MILLISECONDS);
    getScheduler()
      .schedule(this::runFirePingEvent, 500, 50, TimeUnit.MILLISECONDS);
  }

  @Override
  public void onUninstall() {
    CloudNetDriver.getInstance().getEventManager()
      .unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(
      this.getClass().getClassLoader());
  }

  private void initListeners() {
    //GoMint API
    registerListener(new GoMintPlayerListener());

    //CloudNet
    CloudNetDriver.getInstance().getEventManager()
      .registerListener(new GoMintCloudNetListener());
    CloudNetDriver.getInstance().getEventManager()
      .registerListener(new BridgeCustomChannelMessageListener());
  }

  private void runFirePingEvent() {
    PingEvent pingEvent = new PingEvent(
      GoMintCloudNetHelper.getApiMotd(),
      GoMint.instance().getPlayers().size(),
      GoMintCloudNetHelper.getMaxPlayers()
    );

    boolean hasToUpdate = false, value = false;

    GoMint.instance().getPluginManager().callEvent(pingEvent);
    if (pingEvent.getMotd() != null && !pingEvent.getMotd()
      .equalsIgnoreCase(GoMintCloudNetHelper.getApiMotd())) {
      hasToUpdate = true;
      GoMintCloudNetHelper.setApiMotd(pingEvent.getMotd());
      if (pingEvent.getMotd().toLowerCase().contains("running") ||
        pingEvent.getMotd().toLowerCase().contains("ingame") ||
        pingEvent.getMotd().toLowerCase().contains("playing")) {
        value = true;
      }
    }

    if (pingEvent.getMaxPlayers() != GoMintCloudNetHelper.getMaxPlayers()) {
      hasToUpdate = true;
      GoMintCloudNetHelper.setMaxPlayers(pingEvent.getMaxPlayers());
    }

    if (value) {
      GoMintCloudNetHelper.changeToIngame();
      return;
    }

    if (hasToUpdate) {
      BridgeHelper.updateServiceInfo();
    }
  }
}