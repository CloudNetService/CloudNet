package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;

public final class CommandReload extends CommandDefault {

  public CommandReload() {
    super("reload", "rl", "rel");
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args,
      String commandLine, Properties properties) {
    if (args.length == 0) {
      sender.sendMessage(
          "reload confirm",
          "reload config"
      );
    }

    switch (args.length) {
      case 1:
        if (args[0].equalsIgnoreCase("confirm") || args[0]
            .equalsIgnoreCase("all")) {
          getCloudNet().reload();
          sender.sendMessage(
              LanguageManager.getMessage("command-reload-confirm-success"));
          return;
        }
        if (args[0].equalsIgnoreCase("config") || args[0]
            .equalsIgnoreCase("conf")) {
          getCloudNet().getConfig().load();
          getCloudNet().getConfigurationRegistry().load();
          getCloudNet().getCloudServiceManager().reload();
          getCloudNet().getPermissionManagement().reload();
          sender.sendMessage(LanguageManager
              .getMessage("command-reload-reload-config-success"));
          return;
        }
        break;
    }
  }
}