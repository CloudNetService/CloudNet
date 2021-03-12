package de.dytanic.cloudnet.ext.database.mongodb.command;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.database.mongodb.CloudNetMongoDBDatabaseModule;
import de.dytanic.cloudnet.ext.database.mongodb.install.ModuleInstaller;

public class SetupCommand extends Command {

    private CloudNetMongoDBDatabaseModule mongoDBDatabaseModule;

    public SetupCommand(CloudNetMongoDBDatabaseModule mongoDBDatabaseModule) {
        super(new String[] {"setupMongo"}, "cloudnet.database.mongosetup", "Setup the MongoDB Config");
        this.mongoDBDatabaseModule = mongoDBDatabaseModule;
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(LanguageManager.getMessage("command-sub-only-console"));
            return;
        }
        CloudNetDriver.getInstance().getEventManager().registerListener(new ModuleInstaller(mongoDBDatabaseModule));
    }
}
