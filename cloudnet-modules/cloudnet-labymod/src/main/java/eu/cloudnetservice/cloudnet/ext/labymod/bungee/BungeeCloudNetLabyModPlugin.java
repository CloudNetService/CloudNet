package eu.cloudnetservice.cloudnet.ext.labymod.bungee;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.bungee.listener.BungeeLabyModListener;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeCloudNetLabyModPlugin extends Plugin {

    @Override
    public void onEnable() {
        LabyModConfiguration configuration = LabyModUtils.getConfiguration();
        if (configuration == null || !configuration.isEnabled()) {
            return;
        }
        this.getProxy().registerChannel("LMC");

        BungeeLabyModListener listener = new BungeeLabyModListener();
        this.getProxy().getPluginManager().registerListener(this, listener);
        CloudNetDriver.getInstance().getEventManager().registerListener(listener);
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    }
}
