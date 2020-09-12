package de.dytanic.cloudnet.ext.bridge.velocity.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.optional.qual.MaybePresent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CommandCloudNet implements Command {

    @Override
    public void execute(@MaybePresent CommandSource source, @NonNull @MaybePresent String[] args) {
        if (!source.hasPermission("cloudnet.command.cloudnet")) {
            return;
        }

        if (args.length == 0) {
            source.sendMessage(LegacyComponentSerializer.legacyLinking().deserialize(BridgeConfigurationProvider.load().getPrefix().replace("&", "§") + "/cloudnet <command>"));
            return;
        }

        String commandLine = String.join(" ", args);

        if (source instanceof Player) {
            CommandInfo commandInfo = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(commandLine);
            if (commandInfo != null && commandInfo.getPermission() != null) {
                if (!source.hasPermission(commandInfo.getPermission())) {
                    source.sendMessage(
                            LegacyComponentSerializer.legacyLinking().deserialize(
                                    BridgeConfigurationProvider.load().getMessages().get("command-cloud-sub-command-no-permission")
                                            .replace("%command%", commandLine)
                            )
                    );
                    return;
                }
            }
        }

        CloudNetDriver.getInstance().getNodeInfoProvider().sendCommandLineAsync(commandLine).onComplete(messages -> {
            for (String message : messages) {
                if (message != null) {
                    source.sendMessage(LegacyComponentSerializer.legacyLinking().deserialize(BridgeConfigurationProvider.load().getPrefix().replace("&", "§") + message));
                }
            }
        });
    }

    @Override
    public List<String> suggest(CommandSource source, @NonNull String[] currentArgs) {
        String commandLine = String.join(" ", currentArgs);

        if (source instanceof Player) {

            if (commandLine.isEmpty() || commandLine.indexOf(' ') == -1) {
                List<String> responses = new ArrayList<>();
                for (CommandInfo commandInfo : CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommands()) {
                    if (commandInfo.getPermission() == null || source.hasPermission(commandInfo.getPermission())) {
                        responses.addAll(Arrays.asList(commandInfo.getNames()));
                    }
                }
                return responses;
            }

            CommandInfo commandInfo = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(commandLine);

            if (commandInfo != null && commandInfo.getPermission() != null) {
                if (!source.hasPermission(commandInfo.getPermission())) {
                    return Collections.emptyList();
                }
            }

        }

        return new ArrayList<>(CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleTabCompleteResults(commandLine));
    }
}