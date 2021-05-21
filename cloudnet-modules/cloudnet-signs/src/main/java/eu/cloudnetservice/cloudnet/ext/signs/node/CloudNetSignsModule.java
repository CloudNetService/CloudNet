package eu.cloudnetservice.cloudnet.ext.signs.node;

import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.node.commands.CommandSign;
import eu.cloudnetservice.cloudnet.ext.signs.node.configuration.NodeSignsConfigurationHelper;

import java.nio.file.Path;

public class CloudNetSignsModule extends NodeCloudNetModule {

    protected static final String DATABASE_NAME = "cloudnet_signs";

    protected Database database;
    protected Path configurationPath;
    protected SignsConfiguration configuration;

    @ModuleTask(order = 50)
    public void initialize() {
        this.database = this.getCloudNet().getDatabaseProvider().getDatabase(DATABASE_NAME);
        this.configurationPath = this.getModuleWrapper().getDataDirectory().resolve("config.json");
    }

    @ModuleTask(order = 40)
    public void loadConfiguration() {
        this.configuration = NodeSignsConfigurationHelper.read(this.configurationPath);
    }

    @ModuleTask(order = 30)
    public void handleInitialization() {
        SignManagement management = new NodeSignManagement(this.configuration, this.configurationPath, this.database);
        management.registerToServiceRegistry();

        this.registerCommand(new CommandSign(management, this.configurationPath));
        this.getCloudNet().getEventManager().registerListeners(new GlobalChannelMessageListener(management),
                new NodeSignsListener(management));
    }

    @ModuleTask(order = 40, event = ModuleLifeCycle.STOPPED)
    public void handleStopping() throws Exception {
        this.database.close();
        this.getCloudNet().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    }
}
