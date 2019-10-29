package de.dytanic.cloudnet.ext.bridge.velocity.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import net.kyori.text.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.optional.qual.MaybePresent;

public final class CommandCloudNet implements Command {

    @Override
    public void execute(@MaybePresent CommandSource source, @NonNull @MaybePresent String[] args) {
        if (!source.hasPermission("cloudnet.command.cloudnet")) {
            return;
        }

        if (args.length == 0) {
            source.sendMessage(TextComponent.of(BridgeConfigurationProvider.load().getPrefix().replace("&", "§") + "/cloudnet <command>"));
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String arg : args) {
            stringBuilder.append(arg).append(" ");
        }

        if (source instanceof Player) {
            CommandInfo commandInfo = CloudNetDriver.getInstance().getConsoleCommand(stringBuilder.toString());
            if (commandInfo != null && commandInfo.getPermission() != null) {
                if (!source.hasPermission(commandInfo.getPermission())) {
                    source.sendMessage(
                            TextComponent.of(
                                    BridgeConfigurationProvider.load().getMessages().get("command-cloud-sub-command-no-permission")
                                            .replace("%command%", stringBuilder)
                            )
                    );
                    return;
                }
            }
        }

        String[] messages = CloudNetDriver.getInstance().sendCommandLine(stringBuilder.toString());

        if (messages != null) {
            for (String message : messages) {
                if (message != null) {
                    source.sendMessage(TextComponent.of(BridgeConfigurationProvider.load().getPrefix().replace("&", "§") + message));
                }
            }
        }
    }
}