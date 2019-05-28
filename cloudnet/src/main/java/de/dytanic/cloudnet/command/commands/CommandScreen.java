package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.List;
import java.util.function.Predicate;

public final class CommandScreen extends CommandDefault {

  public CommandScreen() {
    super("screen", "scr", "console");
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args,
    String commandLine, Properties properties) {
    if (args.length == 0) {
      sender.sendMessage("screen <local service uniqueId | name> | toggling");
      return;
    }

    if (args.length == 1) {
      ICloudService cloudService = getCloudService(args[0]);

      if (cloudService != null) {
        if (!(sender instanceof ConsoleCommandSender)) {
          for (String input : cloudService.getServiceConsoleLogCache()
            .getCachedLogMessages()) {
            sender.sendMessage(
              "[" + cloudService.getServiceId().getName() + "] " + input);
          }
        } else {
          cloudService.getServiceConsoleLogCache().setAutoPrintReceivedInput(
            !cloudService.getServiceConsoleLogCache()
              .isAutoPrintReceivedInput());

          if (cloudService.getServiceConsoleLogCache()
            .isAutoPrintReceivedInput()) {
            for (String input : cloudService.getServiceConsoleLogCache()
              .getCachedLogMessages()) {
              CloudNetDriver.getInstance().getLogger().log(LogLevel.IMPORTANT,
                "[" + cloudService.getServiceId().getName() + "] " + input);
            }

            sender.sendMessage(
              LanguageManager.getMessage("command-screen-enable-for-service")
                .replace("%name%",
                  cloudService.getServiceId().getName() + "")
                .replace("%uniqueId%",
                  cloudService.getServiceId().getUniqueId().toString()
                    .split("-")[0] + "")
            );
          } else {
            sender.sendMessage(
              LanguageManager.getMessage("command-screen-disable-for-service")
                .replace("%name%",
                  cloudService.getServiceId().getName() + "")
                .replace("%uniqueId%",
                  cloudService.getServiceId().getUniqueId().toString()
                    .split("-")[0] + "")
            );
          }
        }
      }
    }
  }

  private ICloudService getCloudService(String argument) {
    Validate.checkNotNull(argument);

    ICloudService cloudService = Iterables.first(
      CloudNet.getInstance().getCloudServiceManager().getCloudServices()
        .values(), new Predicate<ICloudService>() {
        @Override
        public boolean test(ICloudService cloudService) {
          return cloudService.getServiceId().getUniqueId().toString()
            .toLowerCase().contains(argument.toLowerCase());
        }
      });

    if (cloudService == null) {
      List<ICloudService> cloudServices = Iterables.filter(
        CloudNet.getInstance().getCloudServiceManager().getCloudServices()
          .values(), new Predicate<ICloudService>() {
          @Override
          public boolean test(ICloudService cloudService) {
            return cloudService.getServiceId().getName().toLowerCase()
              .contains(argument.toLowerCase());
          }
        });

      if (!cloudServices.isEmpty()) {
        if (cloudServices.size() > 1) {
          cloudService = Iterables
            .first(cloudServices, new Predicate<ICloudService>() {
              @Override
              public boolean test(ICloudService cloudService) {
                return cloudService.getServiceId().getName()
                  .equalsIgnoreCase(argument);
              }
            });
        } else {
          cloudService = cloudServices.get(0);
        }
      }
    }

    return cloudService;
  }
}