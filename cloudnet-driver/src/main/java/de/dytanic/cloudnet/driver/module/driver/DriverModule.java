package de.dytanic.cloudnet.driver.module.driver;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.module.DefaultModule;

import java.io.File;

public class DriverModule extends DefaultModule {

    private JsonDocument config;

    public final JsonDocument getConfig()
    {
        if (config == null)
        {
            config = reloadConfig0();
        }

        return config;
    }

    public final JsonDocument reloadConfig()
    {
        return config = reloadConfig0();
    }

    public final DriverModule saveConfig()
    {
        if (config != null)
        {
            config.write(new File(getModuleWrapper().getDataFolder(), "config.json"));
        }

        return this;
    }

    private JsonDocument reloadConfig0()
    {
        getModuleWrapper().getDataFolder().mkdirs();

        File file = new File(getModuleWrapper().getDataFolder(), "config.json");

        if (!file.exists()) new JsonDocument().write(file);

        return JsonDocument.newDocument(file);
    }

    public final ILogger log(LogLevel level, String message)
    {
        return getLogger().log(level, message);
    }

    public final IEventManager registerListener(Object listener)
    {
        return getEventManager().registerListener(listener);
    }

    public final IEventManager registerListeners(Object... listeners)
    {
        return getEventManager().registerListeners(listeners);
    }

    public final IServicesRegistry getRegistry()
    {
        return this.getDriver().getServicesRegistry();
    }

    public final ILogger getLogger()
    {
        return this.getDriver().getLogger();
    }

    public final IEventManager getEventManager()
    {
        return this.getDriver().getEventManager();
    }

    public final CloudNetDriver getDriver()
    {
        return CloudNetDriver.getInstance();
    }
}