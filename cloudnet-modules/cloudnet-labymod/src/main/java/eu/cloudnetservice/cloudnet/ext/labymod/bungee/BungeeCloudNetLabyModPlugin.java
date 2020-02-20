package eu.cloudnetservice.cloudnet.ext.labymod.bungee;

import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.bungee.listener.BungeeLabyModListener;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeCloudNetLabyModPlugin extends Plugin {

    @Override
    public void onEnable() {
        LabyModConfiguration configuration = LabyModUtils.getConfiguration();
        if (configuration == null || !configuration.isEnabled()) {
            return;
        }
        this.getProxy().registerChannel("LMC");

        this.getProxy().getPluginManager().registerListener(this, new BungeeLabyModListener());
    }
}
