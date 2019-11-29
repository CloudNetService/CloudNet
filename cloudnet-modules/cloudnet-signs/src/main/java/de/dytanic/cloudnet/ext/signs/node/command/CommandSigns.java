package de.dytanic.cloudnet.ext.signs.node.command;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.SignConstants;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationReaderAndWriter;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntryType;
import de.dytanic.cloudnet.ext.signs.node.CloudNetSignsModule;

import java.util.Arrays;

public final class CommandSigns extends Command {

    public CommandSigns() {
        super("signs", "sign", "cloud-signs");

        this.usage = "signs reload";
        this.permission = "cloudnet.console.command.signs";
        this.prefix = "cloudnet-signs";
        this.description = LanguageManager.getMessage("module-signs-command-signs-description");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    "signs reload",
                    "signs create entry <group> <" + Arrays.toString(SignConfigurationEntryType.values()) + ">"
            );

            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            CloudNetSignsModule.getInstance().setSignConfiguration(SignConfigurationReaderAndWriter.read(
                    CloudNetSignsModule.getInstance().getConfigurationFile()
            ));

            CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                    SignConstants.SIGN_CHANNEL_NAME,
                    SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION,
                    new JsonDocument("signConfiguration", CloudNetSignsModule.getInstance().getSignConfiguration())
            );

            sender.sendMessage(LanguageManager.getMessage("module-signs-command-reload-success"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("create")) {

            if (args[1].equalsIgnoreCase("entry")) {
                String targetGroup = args[2];

                SignConfigurationEntryType signConfigurationEntryType;

                try {
                    signConfigurationEntryType = SignConfigurationEntryType.valueOf(args[3].toUpperCase());
                } catch (Exception exception) {
                    return;
                }

                SignConfiguration signConfiguration = CloudNetSignsModule.getInstance().getSignConfiguration();

                signConfiguration.getConfigurations().add(signConfigurationEntryType.createEntry(targetGroup));
                SignConfigurationReaderAndWriter.write(signConfiguration, CloudNetSignsModule.getInstance().getConfigurationFile());

                sender.sendMessage(LanguageManager.getMessage("module-signs-command-create-entry-success"));
            }

        }
    }
}