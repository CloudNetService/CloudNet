package eu.cloudnetservice.cloudnet.ext.labymod.node;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConfiguration;
import eu.cloudnetservice.cloudnet.ext.labymod.node.listener.IncludePluginListener;
import eu.cloudnetservice.cloudnet.ext.labymod.node.listener.LabyModCustomChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.labymod.player.DiscordJoinMatchConfig;
import eu.cloudnetservice.cloudnet.ext.labymod.player.ServiceDisplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class CloudNetLabyModModule extends NodeCloudNetModule {

    private static final Collection<ServiceEnvironmentType> SUPPORTED_ENVIRONMENTS = Arrays.asList(ServiceEnvironmentType.BUNGEECORD/*, TODO: ServiceEnvironmentType.VELOCITY*/);

    private LabyModConfiguration configuration;

    @ModuleTask(event = ModuleLifeCycle.STARTED)
    public void loadConfig() {
        super.getModuleWrapper().getDataFolder().mkdirs();

        JsonDocument previousConfig = super.getConfig().clone();
        this.configuration = super.getConfig().get("config", LabyModConfiguration.class, new LabyModConfiguration(
                false,
                new ServiceDisplay(true, ServiceDisplay.DisplayType.SERVICE, "%display%"),
                new DiscordJoinMatchConfig(true, new ArrayList<>()),
                new ServiceDisplay(true, ServiceDisplay.DisplayType.TASK, "§bCloud§fNet §8➢ §e%display%"),
                "mc.example.com"
        ));

        if (!previousConfig.equals(super.getConfig())) {
            super.saveConfig();
        }
    }

    @ModuleTask(event = ModuleLifeCycle.STARTED)
    public void initListeners() {
        super.registerListeners(new LabyModCustomChannelMessageListener(this), new IncludePluginListener(this));
    }

    public LabyModConfiguration getConfiguration() {
        return this.configuration;
    }

    public boolean isSupportedEnvironment(ServiceEnvironmentType environment) {
        return SUPPORTED_ENVIRONMENTS.contains(environment);
    }

}
