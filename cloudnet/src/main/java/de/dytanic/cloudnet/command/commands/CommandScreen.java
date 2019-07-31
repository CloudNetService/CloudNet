package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandScreen extends CommandDefault implements ITabCompleter {

    public CommandScreen() {
        super("screen", "scr", "console");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage("screen <local service uniqueId | name> | toggling");
            return;
        }

        if (args.length == 1) {
            ICloudService cloudService = getCloudService(args[0]);

            if (cloudService != null) {
                if (!(sender instanceof ConsoleCommandSender)) {
                    for (String input : cloudService.getServiceConsoleLogCache().getCachedLogMessages()) {
                        sender.sendMessage("[" + cloudService.getServiceId().getName() + "] " + input);
                    }
                } else {
                    boolean autoPrintReceivedInput = !cloudService.getServiceConsoleLogCache().isAutoPrintReceivedInput();
                    cloudService.getServiceConsoleLogCache().setAutoPrintReceivedInput(autoPrintReceivedInput);

                    if (autoPrintReceivedInput) {
                        for (String input : cloudService.getServiceConsoleLogCache().getCachedLogMessages()) {
                            CloudNetDriver.getInstance().getLogger().log(LogLevel.IMPORTANT, "[" + cloudService.getServiceId().getName() + "] " + input);
                        }

                        sender.sendMessage(LanguageManager.getMessage("command-screen-enable-for-service")
                                .replace("%name%", cloudService.getServiceId().getName() + "")
                                .replace("%uniqueId%", cloudService.getServiceId().getUniqueId().toString().split("-")[0] + "")
                        );
                    } else {
                        sender.sendMessage(LanguageManager.getMessage("command-screen-disable-for-service")
                                .replace("%name%", cloudService.getServiceId().getName() + "")
                                .replace("%uniqueId%", cloudService.getServiceId().getUniqueId().toString().split("-")[0] + "")
                        );
                    }
                }
            }
        }
    }

    private ICloudService getCloudService(String argument) {
        Validate.checkNotNull(argument);

        ICloudService cloudService = Iterables.first(CloudNet.getInstance().getCloudServiceManager().getCloudServices().values(), cloudService13 -> cloudService13.getServiceId().getUniqueId().toString().toLowerCase().contains(argument.toLowerCase()));

        if (cloudService == null) {
            List<ICloudService> cloudServices = Iterables.filter(CloudNet.getInstance().getCloudServiceManager().getCloudServices().values(), cloudService12 -> cloudService12.getServiceId().getName().toLowerCase().contains(argument.toLowerCase()));

            if (!cloudServices.isEmpty()) {
                if (cloudServices.size() > 1) {
                    cloudService = Iterables.first(cloudServices, cloudService1 -> cloudService1.getServiceId().getName().equalsIgnoreCase(argument));
                } else {
                    cloudService = cloudServices.get(0);
                }
            }
        }

        return cloudService;
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        return CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()
                .stream().map(cloudService -> cloudService.getServiceId().getName())
                .collect(Collectors.toList());
    }
}