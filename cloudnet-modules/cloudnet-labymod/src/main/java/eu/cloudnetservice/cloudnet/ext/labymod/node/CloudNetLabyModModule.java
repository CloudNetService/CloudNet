package eu.cloudnetservice.cloudnet.ext.labymod.node;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.config.DiscordJoinMatchConfig;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.cloudnet.ext.labymod.config.ServiceDisplay;
import eu.cloudnetservice.cloudnet.ext.labymod.node.listener.IncludePluginListener;
import eu.cloudnetservice.cloudnet.ext.labymod.node.listener.LabyModCustomChannelMessageListener;

import java.util.ArrayList;

public class CloudNetLabyModModule extends NodeCloudNetModule {

    private LabyModConfiguration configuration;

    @ModuleTask(event = ModuleLifeCycle.STARTED)
    public void loadConfig() {
        FileUtils.createDirectoryReported(super.getModuleWrapper().getDataDirectory());

        JsonDocument previousConfig = super.getConfig().clone();
        this.configuration = super.getConfig().get("config", LabyModConfiguration.class, new LabyModConfiguration(
                false,
                new ServiceDisplay(true, ServiceDisplay.DisplayType.SERVICE, "Playing on %display%"),
                new DiscordJoinMatchConfig(true, new ArrayList<>()),
                new ServiceDisplay(true, ServiceDisplay.DisplayType.TASK, "§bCloud§fNet §8➢ §e%display%"),
                "mc.example.com",
                true,
                new ArrayList<>()
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
        return LabyModConstants.SUPPORTED_ENVIRONMENTS.contains(environment);
    }

}
