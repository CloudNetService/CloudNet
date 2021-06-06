package eu.cloudnetservice.cloudnet.ext.labymod.bungee;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.bungee.listener.BungeeLabyModListener;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeCloudNetLabyModPlugin extends Plugin {

  private AbstractLabyModManagement labyModManagement;

  @Override
  public void onEnable() {
    LabyModConfiguration configuration = LabyModUtils.getConfiguration();
    if (configuration == null || !configuration.isEnabled()) {
      return;
    }
    this.getProxy().registerChannel(LabyModConstants.LMC_CHANNEL_NAME);

    this.labyModManagement = new BungeeLabyModManagement(super.getProxy());

    this.getProxy().getPluginManager().registerListener(this, new BungeeLabyModListener(this.labyModManagement));
  }

  @Override
  public void onDisable() {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
  }
}
