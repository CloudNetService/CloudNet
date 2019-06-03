package de.dytanic.cloudnet.ext.bridge.node.command;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;

public final class CommandReloadBridge extends Command {

    public CommandReloadBridge() {
        super("bridge", "rlb", "rl-bridge", "reload-bridge");

        this.permission = "cloudnet.console.command.reload";
        this.prefix = "cloudnet-bridge";
        this.usage = "reload-bridge";
        this.description = LanguageManager.getMessage("module-bridge-command-bridge-description");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        BridgeConfiguration bridgeConfiguration = CloudNetBridgeModule.getInstance().reloadConfig().get("config", BridgeConfiguration.TYPE);
        CloudNetBridgeModule.getInstance().setBridgeConfiguration(bridgeConfiguration);

        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                "update_bridge_configuration",
                new JsonDocument("bridgeConfiguration", bridgeConfiguration)
        );

        sender.sendMessage(LanguageManager.getMessage("module-bridge-command-bridge-execute-success"));
    }
}