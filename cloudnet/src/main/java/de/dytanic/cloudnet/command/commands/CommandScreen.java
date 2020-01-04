package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public final class CommandScreen extends CommandDefault implements ITabCompleter {

    public CommandScreen() {
        super("screen", "scr", "console");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    "screen <local service uniqueId | name> | toggling",
                    "screen write <command>"
            );
        } else if (args.length == 1) {
            ServiceInfoSnapshot serviceInfoSnapshot = super.getCloudNet().getCloudServiceByNameOrUniqueId(args[0]);

            if (serviceInfoSnapshot != null) {

                ICloudService cloudService = super.getCloudNet().getCloudServiceManager().getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());

                if (cloudService != null) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        for (String input : cloudService.getServiceConsoleLogCache().getCachedLogMessages()) {
                            sender.sendMessage("[" + cloudService.getServiceId().getName() + "] " + input);
                        }
                    } else {
                        boolean enabled = !cloudService.getServiceConsoleLogCache().isScreenEnabled();
                        cloudService.getServiceConsoleLogCache().setScreenEnabled(enabled);

                        if (enabled) {
                            for (String input : cloudService.getServiceConsoleLogCache().getCachedLogMessages()) {
                                CloudNetDriver.getInstance().getLogger().log(LogLevel.INFO, "[" + cloudService.getServiceId().getName() + "] " + input);
                            }

                            sender.sendMessage(LanguageManager.getMessage("command-screen-enable-for-service")
                                    .replace("%name%", cloudService.getServiceId().getName())
                                    .replace("%uniqueId%", cloudService.getServiceId().getUniqueId().toString().split("-")[0])
                            );
                        } else {
                            sender.sendMessage(LanguageManager.getMessage("command-screen-disable-for-service")
                                    .replace("%name%", cloudService.getServiceId().getName())
                                    .replace("%uniqueId%", cloudService.getServiceId().getUniqueId().toString().split("-")[0])
                            );
                        }
                    }
                }
            }
        } else {
            String line = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            Collection<String> targetServiceNames = CloudNet.getInstance().getCloudServiceManager().getCloudServices().values().stream()
                    .filter(cloudService -> {
                        if (cloudService.getServiceConsoleLogCache().isScreenEnabled()) {
                            cloudService.runCommand(line);
                            return true;
                        }
                        return false;
                    })
                    .map(cloudService -> cloudService.getServiceId().getName())
                    .collect(Collectors.toSet());

            if (targetServiceNames.isEmpty()) {
                sender.sendMessage(LanguageManager.getMessage("command-screen-write-no-screen"));
            } else {
                sender.sendMessage(LanguageManager.getMessage("command-screen-write-success")
                        .replace("%command%", line)
                        .replace("%targets%", String.join(", ", targetServiceNames)));
            }
        }
    }


    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        return CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()
                .stream().map(cloudService -> cloudService.getServiceId().getName())
                .collect(Collectors.toList());
    }
}
