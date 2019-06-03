package de.dytanic.cloudnet.ext.smart.command;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.ext.smart.CloudNetSmartModule;

public final class CommandSmart extends Command {

    public CommandSmart() {
        super("smart");

        this.permission = "cloudnet.console.command.smart";
        this.prefix = "cloudnet-smart";
        this.description = LanguageManager.getMessage("module-smart-command-smart-description");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    "smart reload"
            );
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            CloudNetSmartModule.getInstance().load();
            sender.sendMessage(LanguageManager.getMessage("module-smart-command-reload-success"));

            CloudNet.getInstance().getClusterNodeServerProvider().sendPacket(new PacketClientServerChannelMessage(
                    "cloudnet_smart_module",
                    "update_configuration",
                    new JsonDocument("smartServiceTaskConfiguration", CloudNetSmartModule.getInstance().getSmartServiceTaskConfigurations())
            ));
        }
    }
}